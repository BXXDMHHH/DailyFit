package com.dailyfit.app.feature.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dailyfit.app.data.local.DailyFitDatabase
import com.dailyfit.app.data.local.DailyWorkoutPlanEntity
import com.dailyfit.app.data.local.ExerciseEntity
import com.dailyfit.app.data.local.WorkoutHistoryEntity
import com.dailyfit.app.data.local.WorkoutSessionEntity
import com.dailyfit.app.data.local.WorkoutSessionExerciseEntity
import com.dailyfit.app.data.local.WorkoutSessionSetEntity
import com.dailyfit.app.data.repository.ExerciseProgress
import com.dailyfit.app.data.repository.TrainingPlanRepository
import com.dailyfit.app.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkoutExerciseUi(
    val row: WorkoutSessionExerciseEntity,
    val sets: List<WorkoutSessionSetEntity>,
    val progress: ExerciseProgress = ExerciseProgress(null, 2.5, null, null)
)

data class WorkoutUiState(
    val session: WorkoutSessionEntity? = null,
    val exercises: List<WorkoutExerciseUi> = emptyList(),
    val library: List<ExerciseEntity> = emptyList(),
    val history: List<WorkoutHistoryEntity> = emptyList(),
    val message: String? = null
)

class WorkoutViewModel(
    private val workouts: WorkoutRepository,
    private val plans: TrainingPlanRepository,
    private val database: DailyFitDatabase
) : ViewModel() {
    private val _state = MutableStateFlow(WorkoutUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            workouts.observeActive().collect { active ->
                val previousSessionId = _state.value.session?.id
                _state.update { it.copy(session = active) }
                if (active?.id != previousSessionId) {
                    if (active != null) refresh(active.id)
                    else _state.update { it.copy(exercises = emptyList()) }
                }
            }
        }
        viewModelScope.launch {
            workouts.observeHistory().collect { rows -> _state.update { it.copy(history = rows) } }
        }
    }

    fun refresh(id: String) {
        viewModelScope.launch {
            val dao = database.workoutSessionDao()
            val rows = dao.getExercises(id)
            val exercises = rows.map { exercise ->
                WorkoutExerciseUi(exercise, dao.getSets(exercise.id), workouts.getExerciseProgress(exercise))
            }
            _state.update { it.copy(exercises = exercises) }
        }
    }

    fun start(plan: DailyWorkoutPlanEntity) {
        viewModelScope.launch {
            runCatching {
                if (database.workoutSessionDao().getActive() != null) error("已有未完成训练，请先恢复")
                val actions = plans.getPlanExercises("DAILY_PLAN", plan.id)
                workouts.start(plan, actions)
            }.onSuccess { refresh(it) }.onFailure { message(it) }
        }
    }

    fun loadLibrary() {
        viewModelScope.launch {
            _state.update { it.copy(library = database.exerciseDao().observeAll().first()) }
        }
    }

    fun addLibraryExercise(exercise: ExerciseEntity) {
        val id = _state.value.session?.id ?: return
        viewModelScope.launch {
            runCatching { workouts.addExercise(id, exercise); refresh(id) }.onFailure { message(it) }
        }
    }

    fun addTemporary(name: String) {
        val id = _state.value.session?.id ?: return
        viewModelScope.launch {
            runCatching { workouts.addExercise(id, null, name); refresh(id) }.onFailure { message(it) }
        }
    }

    fun addSet(exerciseId: String) {
        val id = _state.value.session?.id ?: return
        viewModelScope.launch {
            runCatching { workouts.addSet(id, exerciseId); refresh(id) }.onFailure { message(it) }
        }
    }

    fun removeSet(exerciseId: String, setId: String) {
        val id = _state.value.session?.id ?: return
        viewModelScope.launch {
            runCatching { workouts.removeSet(id, exerciseId, setId); refresh(id) }.onFailure { message(it) }
        }
    }

    fun removeExercise(exerciseId: String) {
        val id = _state.value.session?.id ?: return
        viewModelScope.launch {
            runCatching { workouts.removeExercise(id, exerciseId); refresh(id) }.onFailure { message(it) }
        }
    }

    fun setWorkingWeight(exerciseId: String, weight: Double) {
        val sessionId = _state.value.session?.id ?: return
        viewModelScope.launch {
            runCatching { workouts.updateExerciseWorkingWeight(sessionId, exerciseId, weight); refresh(sessionId) }
                .onFailure { message(it) }
        }
    }

    fun setWeightIncrement(exerciseId: String, increment: Double) {
        val sessionId = _state.value.session?.id ?: return
        val exercise = _state.value.exercises.firstOrNull { it.row.id == exerciseId } ?: return
        viewModelScope.launch {
            runCatching { workouts.saveExerciseIncrement(exercise.row, increment); refresh(sessionId) }
                .onFailure { message(it) }
        }
    }

    fun updateSet(set: WorkoutSessionSetEntity, weight: String, reps: String, done: Boolean) {
        val id = _state.value.session?.id ?: return
        viewModelScope.launch {
            runCatching {
                val actualWeight = weight.toDoubleOrNull()
                val actualReps = reps.toIntOrNull()
                workouts.updateSet(id, set.id, actualWeight, actualReps, done)
                _state.update { current ->
                    current.copy(exercises = current.exercises.map { exercise ->
                        if (exercise.row.id != set.sessionExerciseId) exercise
                        else exercise.copy(sets = exercise.sets.map { currentSet ->
                            if (currentSet.id != set.id) currentSet
                            else currentSet.copy(actualWeight = actualWeight, actualReps = actualReps, isCompleted = done)
                        })
                    })
                }
            }.onFailure { message(it) }
        }
    }

    fun recordOneRepMax(exerciseId: String, actualOneRepMaxText: String) {
        val sessionId = _state.value.session?.id ?: return
        val exercise = _state.value.exercises.firstOrNull { it.row.id == exerciseId } ?: return
        viewModelScope.launch {
            runCatching {
                val actual = if (actualOneRepMaxText.isBlank()) null else
                    actualOneRepMaxText.trim().toDoubleOrNull() ?: error("请输入有效的实际 1RM")
                val estimated = exercise.sets.asSequence()
                    .filter { it.isCompleted && (it.actualWeight ?: 0.0) > 0 && (it.actualReps ?: 0) in 1..30 }
                    .map { it.actualWeight!! * (1.0 + it.actualReps!! / 30.0) }
                    .maxOrNull()
                require(actual != null || estimated != null) { "请填写实际 1RM，或先完成至少一组训练以计算估算 1RM" }
                workouts.saveOneRepMax(exercise.row, actual, estimated)
                refresh(sessionId)
                _state.update { it.copy(message = buildString {
                    append("1RM 已记录")
                    if (actual != null) append(" · 实际 ").append(actual).append(" kg")
                    if (estimated != null) append(" · 估算 ").append(estimated).append(" kg")
                }) }
            }.onFailure { message(it) }
        }
    }

    fun finish(status: String) {
        val id = _state.value.session?.id ?: return
        viewModelScope.launch {
            runCatching { workouts.finish(id, status) }
                .onSuccess {
                    val encouragement = when (status) {
                        "COMPLETED" -> listOf("训练完成！每一次认真训练都在积累进步。", "今天的训练已经记录，继续稳步变强。", "做得很好，保持节奏，不必急于求成。").random()
                        "INCOMPLETE" -> "未完成的训练也已保存，完成多少都算数。"
                        else -> "已记录本次跳过，按自己的节奏继续。"
                    }
                    _state.update { it.copy(session = null, exercises = emptyList(), message = encouragement) }
                }
                .onFailure { message(it) }
        }
    }

    fun discard() {
        val id = _state.value.session?.id ?: return
        viewModelScope.launch {
            runCatching { workouts.discard(id) }
                .onSuccess { _state.update { it.copy(session = null, exercises = emptyList(), message = "未完成训练已放弃，未生成正式历史") } }
                .onFailure { message(it) }
        }
    }

    fun dismiss() { _state.update { it.copy(message = null) } }

    private fun message(error: Throwable) {
        _state.update { it.copy(message = error.message ?: "操作失败") }
    }
}

class WorkoutViewModelFactory(
    private val workouts: WorkoutRepository,
    private val plans: TrainingPlanRepository,
    private val database: DailyFitDatabase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutViewModel::class.java)) {
            return WorkoutViewModel(workouts, plans, database) as T
        }
        throw IllegalArgumentException("Unknown model class: " + modelClass.name)
    }
}
