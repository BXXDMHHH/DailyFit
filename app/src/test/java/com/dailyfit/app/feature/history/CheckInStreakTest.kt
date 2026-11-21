package com.dailyfit.app.feature.history

import com.dailyfit.app.data.local.HabitRecordEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CheckInStreakTest {
    private fun record(date: String, completed: Boolean = true) = HabitRecordEntity(
        id = date,
        habitId = "habit",
        businessDate = date,
        habitNameSnapshot = "晨间阅读",
        isCompleted = completed,
        createdAt = 0L
    )

    @Test fun countsConsecutiveDaysIncludingToday() {
        val today = LocalDate.parse("2026-11-22")
        val records = listOf(record("2026-11-22"), record("2026-11-21"), record("2026-11-20"))
        assertEquals(3, calculateCheckInStreak(records, today))
    }

    @Test fun continuesFromYesterdayWhenNothingCompletedToday() {
        val today = LocalDate.parse("2026-11-22")
        val records = listOf(record("2026-11-21"), record("2026-11-20"))
        assertEquals(2, calculateCheckInStreak(records, today))
    }

    @Test fun breaksOnMissingDayAndIgnoresIncompleteRecords() {
        val today = LocalDate.parse("2026-11-22")
        val records = listOf(record("2026-11-22"), record("2026-11-21", false), record("2026-11-20"))
        assertEquals(1, calculateCheckInStreak(records, today))
    }

    @Test fun emptyHistoryHasZeroStreak() {
        assertEquals(0, calculateCheckInStreak(emptyList(), LocalDate.parse("2026-11-22")))
    }
}
