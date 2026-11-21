package com.dailyfit.app.feature.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dailyfit.app.data.local.DailyWorkoutPlanEntity
import com.dailyfit.app.data.local.ExerciseEntity
import com.dailyfit.app.data.local.WeeklyWorkoutPlanDayEntity
import com.dailyfit.app.data.repository.PlanActionInput
import com.dailyfit.app.data.repository.TrainingPlanRepository
import com.dailyfit.app.data.repository.WeeklyPlanInput
import com.dailyfit.app.data.repository.WorkoutPlanStatus
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

data class PlanActionDraft(
    val exerciseId: String?,
    val name: String,
    val category: String,
    val equipment: String,
    val notes: String,
    val targetSets: String,
    val targetReps: String,
    val targetWeight: String
)

data class PlanUiState(
    val isLoading: Boolean = true,
    val selectedDate: LocalDate = LocalDate.now(),
    val weeklyDays: List<WeeklyWorkoutPlanDayEntity> = emptyList(),
    val dailyPlans: List<DailyWorkoutPlanEntity> = emptyList(),
    val exercises: List<ExerciseEntity> = emptyList(),
    val exerciseQuery: String = "",
    val editingActionOwnerType: String? = null,
    val editingActionOwnerId: String? = null,
    val planActions: List<PlanActionDraft> = emptyList(),
    val message: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class PlanViewModel(
    private val repository: TrainingPlanRepository
) : ViewModel() {
    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val exerciseQuery = MutableStateFlow("")
    private val _uiState = MutableStateFlow(PlanUiState())
    val uiState: StateFlow<PlanUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { repository.ensureInitialTemplate() }
        viewModelScope.launch {
            selectedDate.flatMapLatest { date ->
                repository.ensureDailyPrimary(date)
                combine(
                    repository.observeWeeklyPlan(date),
                    repository.observeDailyPlans(date),
                    exerciseQuery.flatMapLatest(repository::observeExercises)
                ) { weekly, daily, exercises ->
                    PlanUiState(
                        isLoading = false,
                        selectedDate = date,
                        weeklyDays = weekly,
                        dailyPlans = daily,
                        exercises = exercises,
                        exerciseQuery = exerciseQuery.value
                    )
                }
            }.collect { next ->
                val current = _uiState.value
                _uiState.value = next.copy(
                    message = current.message,
                    editingActionOwnerType = current.editingActionOwnerType,
                    editingActionOwnerId = current.editingActionOwnerId,
                    planActions = current.planActions
                )
            }
        }
    }

    fun selectDate(date: LocalDate) { selectedDate.value = date }

    fun setExerciseQuery(query: String) { exerciseQuery.value = query }

    fun saveWeeklyPlan(effectiveFrom: LocalDate, days: List<WeeklyPlanInput>) {
        viewModelScope.launch { runCatching { repository.saveWeeklyPlan(effectiveFrom, days) }.onFailure { showError(it) } }
    }

    fun updatePrimary(title: String, notes: String) {
        viewModelScope.launch { runCatching { repository.updatePrimary(selectedDate.value, title, notes) }.onFailure { showError(it) } }
    }

    fun addExtra(title: String, notes: String) {
        viewModelScope.launch { runCatching { repository.addExtra(selectedDate.value, title, notes) }.onFailure { showError(it) } }
    }

    fun updateExtra(id: String, title: String, notes: String) {
        viewModelScope.launch { runCatching { repository.updateExtra(selectedDate.value, id, title, notes) }.onFailure { showError(it) } }
    }

    fun deleteExtra(id: String) {
        viewModelScope.launch { runCatching { repository.deleteExtra(selectedDate.value, id) }.onFailure { showError(it) } }
    }

    fun setStatus(id: String, status: WorkoutPlanStatus) {
        viewModelScope.launch { runCatching { repository.setStatus(selectedDate.value, id, status) }.onFailure { showError(it) } }
    }

    fun saveExercise(id: String?, name: String, category: String, equipment: String, notes: String) {
        viewModelScope.launch {
            runCatching { repository.saveExercise(id, name, category, equipment, notes) }.onFailure { showError(it) }
        }
    }

    fun deleteExercise(id: String) {
        viewModelScope.launch { runCatching { repository.deleteExercise(id) }.onFailure { showError(it) } }
    }

    fun openActionEditor(ownerType: String, ownerId: String) {
        viewModelScope.launch {
            runCatching { repository.getPlanExercises(ownerType, ownerId) }
                .onSuccess { rows ->
                    _uiState.value = _uiState.value.copy(
                        editingActionOwnerType = ownerType,
                        editingActionOwnerId = ownerId,
                        planActions = rows.map {
                            PlanActionDraft(
                                it.exerciseId, it.exerciseNameSnapshot, it.categorySnapshot,
                                it.equipmentSnapshot, it.notesSnapshot, it.targetSets.toString(),
                                it.targetReps, it.targetWeight.toString()
                            )
                        }
                    )
                }
                .onFailure { showError(it) }
        }
    }

    fun addAction(exercise: ExerciseEntity) {
        val current = _uiState.value.planActions
        _uiState.value = _uiState.value.copy(
            planActions = current + PlanActionDraft(
                exercise.id, exercise.name, exercise.category, exercise.equipment,
                exercise.notes, "3", "8-12", "0"
            )
        )
    }

    fun updateAction(index: Int, targetSets: String? = null, targetReps: String? = null, targetWeight: String? = null) {
        val actions = _uiState.value.planActions.toMutableList()
        val old = actions.getOrNull(index) ?: return
        actions[index] = old.copy(
            targetSets = targetSets ?: old.targetSets,
            targetReps = targetReps ?: old.targetReps,
            targetWeight = targetWeight ?: old.targetWeight
        )
        _uiState.value = _uiState.value.copy(planActions = actions)
    }

    fun removeAction(index: Int) {
        _uiState.value = _uiState.value.copy(planActions = _uiState.value.planActions.filterIndexed { i, _ -> i != index })
    }

    fun importWeeklyActionsIntoDailyPlan() {
        val state = _uiState.value
        val ownerId = state.editingActionOwnerId ?: return
        if (state.editingActionOwnerType != "DAILY_PLAN") return
        viewModelScope.launch {
            runCatching {
                repository.importWeeklyActions(state.selectedDate, ownerId)
                repository.getPlanExercises("DAILY_PLAN", ownerId)
            }.onSuccess { rows ->
                _uiState.value = _uiState.value.copy(
                    planActions = rows.map {
                        PlanActionDraft(
                            it.exerciseId, it.exerciseNameSnapshot, it.categorySnapshot,
                            it.equipmentSnapshot, it.notesSnapshot, it.targetSets.toString(),
                            it.targetReps, it.targetWeight.toString()
                        )
                    },
                    message = null
                )
            }.onFailure { showError(it) }
        }
    }

    fun saveActionEditor() {
        val state = _uiState.value
        val ownerType = state.editingActionOwnerType ?: return
        val ownerId = state.editingActionOwnerId ?: return
        viewModelScope.launch {
            runCatching {
                repository.savePlanExercises(ownerType, ownerId, state.planActions.map {
                    PlanActionInput(
                        it.exerciseId, it.name, it.category, it.equipment, it.notes,
                        it.targetSets.toIntOrNull() ?: -1, it.targetReps, it.targetWeight.toDoubleOrNull() ?: -1.0
                    )
                })
            }.onSuccess {
                _uiState.value = _uiState.value.copy(
                    editingActionOwnerType = null,
                    editingActionOwnerId = null,
                    planActions = emptyList(),
                    message = "计划动作已保存"
                )
            }.onFailure { showError(it) }
        }
    }

    fun closeActionEditor() {
        _uiState.value = _uiState.value.copy(
            editingActionOwnerType = null,
            editingActionOwnerId = null,
            planActions = emptyList()
        )
    }

    fun consumeMessage() { _uiState.value = _uiState.value.copy(message = null) }

    private fun showError(error: Throwable) {
        _uiState.value = _uiState.value.copy(message = error.message ?: "操作失败，请重试")
    }
}

class PlanViewModelFactory(
    private val repository: TrainingPlanRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlanViewModel::class.java)) return PlanViewModel(repository) as T
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
