package com.dailyfit.app.data.repository

import androidx.room.withTransaction
import com.dailyfit.app.data.local.DailyFitDatabase
import com.dailyfit.app.data.local.DailyWorkoutPlanEntity
import com.dailyfit.app.data.local.ExerciseEntity
import com.dailyfit.app.data.local.WeeklyWorkoutPlanDayEntity
import com.dailyfit.app.data.local.WeeklyWorkoutPlanVersionEntity
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

enum class WorkoutPlanStatus { PLANNED, COMPLETED, SKIPPED }

data class WeeklyPlanInput(
    val dayOfWeek: Int,
    val title: String,
    val notes: String = "",
    val isRestDay: Boolean = false
)

class TrainingPlanRepository(
    private val database: DailyFitDatabase,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    private val weeklyDao = database.weeklyWorkoutPlanDao()
    private val dailyDao = database.dailyWorkoutPlanDao()
    private val exerciseDao = database.exerciseDao()

    suspend fun ensureInitialTemplate() {
        database.withTransaction {
            if (weeklyDao.getVersionForDate(LocalDate.now(clock).toString()) == null) {
                val version = WeeklyWorkoutPlanVersionEntity(
                    id = UUID.randomUUID().toString(),
                    effectiveFromDate = LocalDate.now(clock).toString(),
                    createdAt = clock.millis()
                )
                weeklyDao.insertVersion(version)
                weeklyDao.insertDays(defaultWeek(version.id))
            }
        }
    }

    fun observeWeeklyPlan(date: LocalDate): Flow<List<WeeklyWorkoutPlanDayEntity>> =
        weeklyDao.observeVersionForDate(date.toString()).flatMapLatest { version ->
            if (version == null) flowOf(emptyList())
            else weeklyDao.observeDays(version.id)
        }

    suspend fun saveWeeklyPlan(effectiveFrom: LocalDate, days: List<WeeklyPlanInput>) {
        require(!effectiveFrom.isBefore(LocalDate.now(clock))) { "固定计划不能追溯修改过去，请选择今天或未来日期" }
        require(days.size == 7 && days.map { it.dayOfWeek }.toSet() == (1..7).toSet()) {
            "每周计划必须完整包含周一至周日"
        }
        require(days.all { it.title.isNotBlank() || it.isRestDay }) { "训练日名称不能为空" }

        database.withTransaction {
            val version = WeeklyWorkoutPlanVersionEntity(
                id = UUID.randomUUID().toString(),
                effectiveFromDate = effectiveFrom.toString(),
                createdAt = clock.millis()
            )
            weeklyDao.insertVersion(version)
            weeklyDao.insertDays(days.sortedBy { it.dayOfWeek }.map {
                WeeklyWorkoutPlanDayEntity(
                    id = UUID.randomUUID().toString(),
                    versionId = version.id,
                    dayOfWeek = it.dayOfWeek,
                    title = it.title.trim().ifBlank { "休息" },
                    notes = it.notes.trim(),
                    isRestDay = it.isRestDay
                )
            })
        }
    }

    suspend fun ensureDailyPrimary(date: LocalDate) {
        database.withTransaction {
            if (dailyDao.getPrimary(date.toString()) != null) return@withTransaction
            val version = weeklyDao.getVersionForDate(date.toString()) ?: run {
                val initial = WeeklyWorkoutPlanVersionEntity(
                    id = UUID.randomUUID().toString(),
                    effectiveFromDate = LocalDate.now(clock).toString(),
                    createdAt = clock.millis()
                )
                weeklyDao.insertVersion(initial)
                weeklyDao.insertDays(defaultWeek(initial.id))
                weeklyDao.getVersionForDate(date.toString())
            } ?: return@withTransaction
            val day = weeklyDao.getDay(version.id, date.dayOfWeek.value) ?: return@withTransaction
            dailyDao.insert(
                DailyWorkoutPlanEntity(
                    id = UUID.randomUUID().toString(),
                    businessDate = date.toString(),
                    title = day.title,
                    notes = day.notes,
                    status = WorkoutPlanStatus.PLANNED.name,
                    isPrimary = true,
                    sourceVersionId = version.id,
                    createdAt = clock.millis(),
                    updatedAt = clock.millis()
                )
            )
        }
    }

    fun observeDailyPlans(date: LocalDate): Flow<List<DailyWorkoutPlanEntity>> =
        dailyDao.observeForDate(date.toString())

    suspend fun updatePrimary(date: LocalDate, title: String, notes: String): Boolean {
        require(title.isNotBlank()) { "训练安排名称不能为空" }
        ensureDailyPrimary(date)
        val primary = dailyDao.getPrimary(date.toString()) ?: return false
        return dailyDao.updatePrimary(primary.id, date.toString(), title.trim(), notes.trim(), clock.millis()) > 0
    }

    suspend fun addExtra(date: LocalDate, title: String, notes: String = ""): String {
        require(title.isNotBlank()) { "额外训练名称不能为空" }
        val id = UUID.randomUUID().toString()
        dailyDao.insert(
            DailyWorkoutPlanEntity(
                id = id,
                businessDate = date.toString(),
                title = title.trim(),
                notes = notes.trim(),
                status = WorkoutPlanStatus.PLANNED.name,
                isPrimary = false,
                sourceVersionId = null,
                createdAt = clock.millis(),
                updatedAt = clock.millis()
            )
        )
        return id
    }

    suspend fun updateExtra(date: LocalDate, id: String, title: String, notes: String): Boolean {
        require(title.isNotBlank()) { "训练安排名称不能为空" }
        return dailyDao.updateExtra(id, date.toString(), title.trim(), notes.trim(), clock.millis()) > 0
    }

    suspend fun deleteExtra(date: LocalDate, id: String): Boolean =
        dailyDao.deleteExtra(id, date.toString()) > 0

    suspend fun setStatus(date: LocalDate, id: String, status: WorkoutPlanStatus): Boolean =
        dailyDao.updateStatus(id, date.toString(), status.name, clock.millis()) > 0

    fun observeExercises(query: String): Flow<List<ExerciseEntity>> =
        if (query.isBlank()) exerciseDao.observeAll() else exerciseDao.search(query.trim())

    suspend fun saveExercise(
        id: String?,
        name: String,
        category: String,
        equipment: String,
        notes: String
    ) {
        require(name.isNotBlank()) { "动作名称不能为空" }
        val now = clock.millis()
        if (id == null) {
            exerciseDao.insert(
                ExerciseEntity(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    category = category.trim(),
                    equipment = equipment.trim(),
                    notes = notes.trim(),
                    createdAt = now,
                    updatedAt = now
                )
            )
        } else {
            val old = exerciseDao.getById(id) ?: return
            exerciseDao.update(old.copy(
                name = name.trim(),
                category = category.trim(),
                equipment = equipment.trim(),
                notes = notes.trim(),
                updatedAt = now
            ))
        }
    }

    suspend fun deleteExercise(id: String): Boolean = exerciseDao.delete(id) > 0

    private fun defaultWeek(versionId: String): List<WeeklyWorkoutPlanDayEntity> {
        val titles = listOf("上肢 A", "下肢 A", "有氧", "上肢 B", "下肢 B", "有氧", "恢复")
        return titles.mapIndexed { index, title ->
            WeeklyWorkoutPlanDayEntity(
                id = UUID.randomUUID().toString(),
                versionId = versionId,
                dayOfWeek = index + 1,
                title = title,
                notes = "",
                isRestDay = index == 6
            )
        }
    }
}
