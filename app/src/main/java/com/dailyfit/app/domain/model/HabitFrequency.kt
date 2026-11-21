package com.dailyfit.app.domain.model
import java.time.DayOfWeek
import java.time.LocalDate
enum class HabitFrequency { DAILY, WEEKDAYS, CUSTOM }
/** Custom weekdays use ISO values: Monday=1 through Sunday=7. */
object HabitScheduleRules {
    fun shouldRunOn(frequency: HabitFrequency, date: LocalDate, customDays: Set<DayOfWeek> = emptySet()): Boolean =
        when (frequency) {
            HabitFrequency.DAILY -> true
            HabitFrequency.WEEKDAYS -> date.dayOfWeek.value in DayOfWeek.MONDAY.value..DayOfWeek.FRIDAY.value
            HabitFrequency.CUSTOM -> date.dayOfWeek in customDays
        }
}
