package com.dailyfit.app.feature.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dailyfit.app.data.local.DailyWorkoutPlanEntity
import com.dailyfit.app.data.local.ExerciseEntity
import com.dailyfit.app.data.local.WeeklyWorkoutPlanDayEntity
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

data class PlanUiState(
    val isLoading: Boolean = true,
    val selectedDate: LocalDate = LocalDate.now(),
    val weeklyDays: List<WeeklyWorkoutPlanDayEntity> = emptyList(),
    val dailyPlans: List<DailyWorkoutPlanEntity> = emptyList(),
    val exercises: List<ExerciseEntity> = emptyList(),
    val exerciseQuery: String = "",
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
            }.collect { next -> _uiState.value = next.copy(message = _uiState.value.message) }
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
