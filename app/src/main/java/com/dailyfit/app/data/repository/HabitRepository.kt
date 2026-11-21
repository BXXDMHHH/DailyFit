package com.dailyfit.app.data.repository

import androidx.room.withTransaction
import com.dailyfit.app.data.local.DailyFitDatabase
import com.dailyfit.app.data.local.HabitEntity
import com.dailyfit.app.data.local.HabitRecordEntity
import com.dailyfit.app.domain.model.HabitFrequency
import com.dailyfit.app.domain.model.HabitScheduleRules
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class HabitItem(
    val habit: HabitEntity,
    val record: HabitRecordEntity
)

data class HabitDashboard(
    val date: LocalDate,
    val activeHabits: List<HabitEntity>,
    val todayHabits: List<HabitItem>,
    val inactiveHabits: List<HabitEntity>,
    val todayCompleted: Int,
    val todayTotal: Int,
    val allCompleted: Int,
    val allTotal: Int
) {
    val todayCompletionPercent: Int
        get() = if (todayTotal == 0) 0 else todayCompleted * 100 / todayTotal
    val overallCompletionPercent: Int
        get() = if (allTotal == 0) 0 else allCompleted * 100 / allTotal
}

class HabitRepository(
    private val database: DailyFitDatabase,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    private val habitDao = database.habitDao()
    private val recordDao = database.habitRecordDao()

    fun observeDashboard(date: LocalDate): Flow<HabitDashboard> {
        val dateKey = date.toString()
        return combine(
            habitDao.observeAll(),
            recordDao.observeForDate(dateKey),
            recordDao.observeAll()
        ) { habits, todayRecords, allRecords ->
            val recordsByHabit = todayRecords.associateBy { it.habitId }
            val todayItems = habits.asSequence()
                .filter { it.isActive }
                .mapNotNull { habit -> recordsByHabit[habit.id]?.let { HabitItem(habit, it) } }
                .toList()
            HabitDashboard(
                date = date,
                activeHabits = habits.filter { it.isActive },
                todayHabits = todayItems,
                inactiveHabits = habits.filterNot { it.isActive },
                todayCompleted = todayItems.count { it.record.isCompleted },
                todayTotal = todayItems.size,
                allCompleted = allRecords.count { it.isCompleted },
                allTotal = allRecords.size
            )
        }
    }

    fun observeHistory(): Flow<List<HabitRecordEntity>> = recordDao.observeAll()

    /** Creates records for the current local date only. It never backfills a missed date. */
    suspend fun ensureTodayRecords() {
        val today = LocalDate.now(clock)
        database.withTransaction {
            habitDao.getActive().forEach { habit ->
                val frequency = HabitFrequency.valueOf(habit.frequency)
                val days = parseDays(habit.customDays)
                if (HabitScheduleRules.shouldRunOn(frequency, today, days)) {
                    recordDao.insertIfMissing(
                        HabitRecordEntity(
                            id = UUID.randomUUID().toString(),
                            habitId = habit.id,
                            businessDate = today.toString(),
                            habitNameSnapshot = habit.name,
                            createdAt = clock.millis()
                        )
                    )
                }
            }
        }
    }

    suspend fun createHabit(name: String, frequency: HabitFrequency, customDays: Set<DayOfWeek>) {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "习惯名称不能为空" }
        require(frequency != HabitFrequency.CUSTOM || customDays.isNotEmpty()) { "自定义频率至少选择一天" }
        val now = clock.millis()
        database.withTransaction {
            habitDao.insert(
                HabitEntity(
                    id = UUID.randomUUID().toString(),
                    name = cleanName,
                    frequency = frequency.name,
                    customDays = serializeDays(customDays),
                    isActive = true,
                    createdAt = now,
                    updatedAt = now
                )
            )
            ensureTodayRecordsInTransaction()
        }
    }

    suspend fun updateHabit(
        id: String,
        name: String,
        frequency: HabitFrequency,
        customDays: Set<DayOfWeek>
    ) {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "习惯名称不能为空" }
        require(frequency != HabitFrequency.CUSTOM || customDays.isNotEmpty()) { "自定义频率至少选择一天" }
        database.withTransaction {
            habitDao.updateDefinition(
                id = id,
                name = cleanName,
                frequency = frequency.name,
                customDays = serializeDays(customDays),
                updatedAt = clock.millis()
            )
            // Existing records are deliberately not rewritten: their name/status are historical snapshots.
            ensureTodayRecordsInTransaction()
        }
    }

    suspend fun deactivateHabit(id: String) {
        database.withTransaction { habitDao.deactivate(id, clock.millis()) }
    }

    suspend fun setTodayCompletion(recordId: String, completed: Boolean): Boolean {
        val today = LocalDate.now(clock).toString()
        val changed = recordDao.setCompletion(
            id = recordId,
            today = today,
            completed = completed,
            completedAt = if (completed) clock.millis() else null
        )
        return changed == 1
    }

    private suspend fun ensureTodayRecordsInTransaction() {
        val today = LocalDate.now(clock)
        habitDao.getActive().forEach { habit ->
            val frequency = HabitFrequency.valueOf(habit.frequency)
            if (HabitScheduleRules.shouldRunOn(frequency, today, parseDays(habit.customDays))) {
                recordDao.insertIfMissing(
                    HabitRecordEntity(
                        id = UUID.randomUUID().toString(),
                        habitId = habit.id,
                        businessDate = today.toString(),
                        habitNameSnapshot = habit.name,
                        createdAt = clock.millis()
                    )
                )
            }
        }
    }

    private fun serializeDays(days: Set<DayOfWeek>): String =
        days.map { it.value }.sorted().joinToString(",")

    private fun parseDays(value: String): Set<DayOfWeek> =
        value.split(",").mapNotNull { it.toIntOrNull()?.let { day -> DayOfWeek.of(day) } }.toSet()
}
