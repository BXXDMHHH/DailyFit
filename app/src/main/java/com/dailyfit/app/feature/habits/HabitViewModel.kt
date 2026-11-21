package com.dailyfit.app.feature.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dailyfit.app.data.local.HabitRecordEntity
import com.dailyfit.app.data.repository.HabitDashboard
import com.dailyfit.app.data.repository.HabitRepository
import com.dailyfit.app.domain.model.HabitFrequency
import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
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
    val message: String? = null
)

class HabitViewModel(
    private val repository: HabitRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {
    private val currentDate = MutableStateFlow(LocalDate.now(clock))
    private val _uiState = MutableStateFlow(HabitUiState())
    val uiState: StateFlow<HabitUiState> = _uiState.asStateFlow()
    private var midnightJob: Job? = null

    init {
        viewModelScope.launch {
            currentDate.flatMapLatest { date ->
                repository.ensureTodayRecords()
                combine(repository.observeDashboard(date), repository.observeHistory()) { dashboard, history ->
                    HabitUiState(isLoading = false, dashboard = dashboard, history = history)
                }
            }.collect { _uiState.value = it }
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
                    if (!changed) _uiState.value = _uiState.value.copy(message = "只能打卡或取消今天的习惯")
                }
                .onFailure { showError(it) }
        }
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun showError(error: Throwable) {
        _uiState.value = _uiState.value.copy(message = error.message ?: "操作失败，请重试")
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
    private val repository: HabitRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HabitViewModel::class.java)) {
            return HabitViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
