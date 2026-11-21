package com.dailyfit.app.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class HabitScheduleRulesTest {
    @Test fun dailyRunsEveryDay() {
        assertTrue(HabitScheduleRules.shouldRunOn(HabitFrequency.DAILY, LocalDate.of(2026, 11, 21)))
    }

    @Test fun weekdaysExcludeSaturdayAndSunday() {
        assertTrue(HabitScheduleRules.shouldRunOn(HabitFrequency.WEEKDAYS, LocalDate.of(2026, 11, 20)))
        assertFalse(HabitScheduleRules.shouldRunOn(HabitFrequency.WEEKDAYS, LocalDate.of(2026, 11, 21)))
    }

    @Test fun customFrequencyUsesIsoWeekdays() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        assertTrue(HabitScheduleRules.shouldRunOn(HabitFrequency.CUSTOM, LocalDate.of(2026, 11, 20), days))
        assertFalse(HabitScheduleRules.shouldRunOn(HabitFrequency.CUSTOM, LocalDate.of(2026, 11, 21), days))
    }
}
