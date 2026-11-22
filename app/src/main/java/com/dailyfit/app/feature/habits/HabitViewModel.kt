package com.dailyfit.app.feature.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dailyfit.app.data.local.HabitRecordEntity
import com.dailyfit.app.data.local.TemporaryTaskEntity
import com.dailyfit.app.data.repository.HabitDashboard
import com.dailyfit.app.data.repository.HabitRepository
import com.dailyfit.app.data.repository.TemporaryTaskDashboard
import com.dailyfit.app.data.repository.TemporaryTaskRepository
import com.dailyfit.app.domain.model.HabitFrequency
import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class HabitUiState(
    val isLoading: Boolean = true,
    val dashboard: HabitDashboard? = null,
    val history: List<HabitRecordEntity> = emptyList(),
    val temporaryTaskDashboard: TemporaryTaskDashboard? = null,
    val message: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class HabitViewModel(
    private val repository: HabitRepository,
    private val temporaryTaskRepository: TemporaryTaskRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {
    private val currentDate = MutableStateFlow(LocalDate.now(clock))
    val businessDate: StateFlow<LocalDate> = currentDate.asStateFlow()
    private val _uiState = MutableStateFlow(HabitUiState())
    val uiState: StateFlow<HabitUiState> = _uiState.asStateFlow()
    private var midnightJob: Job? = null

    init {
        viewModelScope.launch {
            currentDate.flatMapLatest { date ->
                runCatching { repository.ensureTodayRecords() }.onFailure(::showError)
                combine(
                    repository.observeDashboard(date),
                    repository.observeHistory(),
                    temporaryTaskRepository.observeDashboard(date)
                ) { dashboard, history, taskDashboard ->
                    HabitUiState(
                        isLoading = false,
                        dashboard = dashboard,
                        history = history,
                        temporaryTaskDashboard = taskDashboard
                    )
                }
            }.collect { _uiState.value = it.copy(message = _uiState.value.message) }
        }
        scheduleMidnightRefresh()
    }

    fun refreshDate() {
        val latest = LocalDate.now(clock)
        if (latest != currentDate.value) currentDate.value = latest
        else viewModelScope.launch { repository.ensureTodayRecords() }
    }

    fun addHabit(name: String, frequency: HabitFrequency, customDays: Set<DayOfWeek>) {
        viewModelScope.launch { runCatching { repository.createHabit(name, frequency, customDays) }.onFailure { showError(it) } }
    }

    fun editHabit(id: String, name: String, frequency: HabitFrequency, customDays: Set<DayOfWeek>) {
        viewModelScope.launch { runCatching { repository.updateHabit(id, name, frequency, customDays) }.onFailure { showError(it) } }
    }

    fun deactivateHabit(id: String) {
        viewModelScope.launch { runCatching { repository.deactivateHabit(id) }.onFailure { showError(it) } }
    }

    fun setCompleted(recordId: String, completed: Boolean) {
        viewModelScope.launch {
            runCatching { repository.setTodayCompletion(recordId, completed) }
                .onSuccess { changed ->
                    if (!changed) showMessage("只能打卡或取消今天的习惯")
                    else if (completed) showMessage(
                        listOf("完成一项习惯，今天又向前走了一步。", "保持这个节奏，你正在积累改变。", "很好，持续行动比完美计划更重要。").random()
                    )
                }
                .onFailure { showError(it) }
        }
    }

    fun addTemporaryTask(title: String) {
        viewModelScope.launch {
            runCatching { temporaryTaskRepository.createTask(title) }
                .onFailure { showError(it) }
        }
    }

    fun editTemporaryTask(id: String, title: String) {
        viewModelScope.launch {
            runCatching { temporaryTaskRepository.updateTask(id, title) }
                .onSuccess { changed -> if (!changed) showMessage("只能修改今天创建的临时任务") }
                .onFailure { showError(it) }
        }
    }

    fun setTemporaryTaskCompleted(id: String, completed: Boolean) {
        viewModelScope.launch {
            runCatching { temporaryTaskRepository.setCompletion(id, completed) }
                .onSuccess { changed ->
                    if (!changed) showMessage("只能完成或取消今天的临时任务")
                    else if (completed) showMessage(listOf("临时任务完成，做得不错！", "又清掉一件事，继续保持。").random())
                }
                .onFailure { showError(it) }
        }
    }

    fun deleteTemporaryTask(id: String) {
        viewModelScope.launch {
            runCatching { temporaryTaskRepository.deleteTask(id) }
                .onSuccess { changed -> if (!changed) showMessage("只能删除今天的临时任务") }
                .onFailure { showError(it) }
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun showMessage(message: String) {
        _uiState.value = _uiState.value.copy(message = message)
    }

    private fun showError(error: Throwable) {
        showMessage(error.message ?: "操作失败，请重试")
    }

    private fun scheduleMidnightRefresh() {
        midnightJob?.cancel()
        midnightJob = viewModelScope.launch {
            while (true) {
                val now = LocalDateTime.now(clock)
                val nextDay = now.toLocalDate().plusDays(1).atStartOfDay(clock.zone)
                delay(Duration.between(now, nextDay).toMillis().coerceAtLeast(1L))
                refreshDate()
            }
        }
    }
}

class HabitViewModelFactory(
    private val repository: HabitRepository,
    private val temporaryTaskRepository: TemporaryTaskRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HabitViewModel::class.java)) {
            return HabitViewModel(repository, temporaryTaskRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
