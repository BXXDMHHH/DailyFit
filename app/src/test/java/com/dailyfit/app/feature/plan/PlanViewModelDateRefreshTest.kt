package com.dailyfit.app.feature.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import com.dailyfit.app.data.local.DailyFitDatabase
import com.dailyfit.app.data.repository.TrainingPlanRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PlanViewModelDateRefreshTest {
    @Test
    fun selectedCurrentDayAdvancesAfterMidnightButManualDateSelectionIsPreserved() = runBlocking {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val context = RuntimeEnvironment.getApplication()
        val database = Room.inMemoryDatabaseBuilder(context, DailyFitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val clock = MutablePlanClock(
            Instant.parse("2026-11-23T08:00:00Z"),
            ZoneId.of("Asia/Tokyo")
        )
        val store = ViewModelStore()
        try {
            val repository = TrainingPlanRepository(database, clock)
            val factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    PlanViewModel(repository, clock) as T
            }
            val viewModel = ViewModelProvider(store, factory)[PlanViewModel::class.java]
            val firstDate = LocalDate.now(clock)
            withTimeout(5_000) {
                viewModel.uiState.first { it.selectedDate == firstDate && !it.isLoading }
            }

            clock.currentInstant = Instant.parse("2026-11-24T08:00:00Z")
            viewModel.refreshDate()
            val nextDate = firstDate.plusDays(1)
            withTimeout(5_000) {
                viewModel.uiState.first { it.selectedDate == nextDate && !it.isLoading }
            }

            val manuallySelected = firstDate.minusDays(3)
            viewModel.selectDate(manuallySelected)
            withTimeout(5_000) {
                viewModel.uiState.first { it.selectedDate == manuallySelected && !it.isLoading }
            }
            clock.currentInstant = Instant.parse("2026-11-25T08:00:00Z")
            viewModel.refreshDate()
            assertEquals(manuallySelected, viewModel.uiState.value.selectedDate)
        } finally {
            store.clear()
            database.close()
            Dispatchers.resetMain()
        }
    }
}

private class MutablePlanClock(
    var currentInstant: Instant,
    private val currentZone: ZoneId
) : Clock() {
    override fun getZone(): ZoneId = currentZone
    override fun withZone(zone: ZoneId): Clock = MutablePlanClock(currentInstant, zone)
    override fun instant(): Instant = currentInstant
}
