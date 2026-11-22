package com.dailyfit.app.data.repository

import androidx.room.withTransaction
import com.dailyfit.app.data.local.AppMetadataEntity
import com.dailyfit.app.data.local.DailyFitDatabase
import com.dailyfit.app.data.local.DailyWorkoutPlanEntity
import com.dailyfit.app.data.local.ExerciseEntity
import com.dailyfit.app.data.local.WeeklyWorkoutPlanDayEntity
import com.dailyfit.app.data.local.WeeklyWorkoutPlanVersionEntity
import com.dailyfit.app.data.local.WorkoutPlanExerciseEntity
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import com.dailyfit.app.domain.model.WorkoutInputRules
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

data class PlanActionInput(
    val exerciseId: String?,
    val name: String,
    val category: String = "",
    val equipment: String = "",
    val notes: String = "",
    val targetSets: Int = 3,
    val targetReps: String = "8-12",
    val targetWeight: Double = 0.0
)

class TrainingPlanRepository(
    private val database: DailyFitDatabase,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    private val weeklyDao = database.weeklyWorkoutPlanDao()
    private val dailyDao = database.dailyWorkoutPlanDao()
    private val exerciseDao = database.exerciseDao()
    private val planExerciseDao = database.workoutPlanExerciseDao()

    suspend fun ensureInitialTemplate() {
        database.withTransaction {
            val seedKey = "default_exercises_seeded_v1"
            if (database.appMetadataDao().getValue(seedKey) == null) {
                if (exerciseDao.getAll().isEmpty()) {
                    val now = clock.millis()
                    listOf(
                        ExerciseEntity("builtin_bench_press", "卧推", "胸部", "杠铃", "", now, now),
                        ExerciseEntity("builtin_squat", "深蹲", "腿部", "杠铃", "", now, now),
                        ExerciseEntity("builtin_deadlift", "硬拉", "背部", "杠铃", "", now, now),
                        ExerciseEntity("builtin_barbell_row", "杠铃划船", "背部", "杠铃", "", now, now),
                        ExerciseEntity("builtin_lat_pulldown", "高位下拉", "背部", "高位下拉器", "", now, now),
                        ExerciseEntity("builtin_shoulder_press", "肩推", "肩部", "哑铃", "", now, now),
                        ExerciseEntity("builtin_lateral_raise", "侧平举", "肩部", "哑铃", "", now, now)
                    ).forEach { exerciseDao.insert(it) }
                }
                database.appMetadataDao().put(AppMetadataEntity(seedKey, "true"))
            }
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
            // Preserve each weekday's configured actions when creating a new template version.
            val previousVersion = weeklyDao.getVersionForDate(effectiveFrom.toString())
            val previousDays = previousVersion?.let { weeklyDao.getDays(it.id) }.orEmpty()
            val previousByWeekday = previousDays.associateBy { it.dayOfWeek }
            val version = WeeklyWorkoutPlanVersionEntity(
                id = UUID.randomUUID().toString(),
                effectiveFromDate = effectiveFrom.toString(),
                createdAt = clock.millis()
            )
            weeklyDao.insertVersion(version)
            val newDays = days.sortedBy { it.dayOfWeek }.map {
                WeeklyWorkoutPlanDayEntity(
                    id = UUID.randomUUID().toString(),
                    versionId = version.id,
                    dayOfWeek = it.dayOfWeek,
                    title = it.title.trim().ifBlank { "休息" },
                    notes = it.notes.trim(),
                    isRestDay = it.isRestDay
                )
            }
            weeklyDao.insertDays(newDays)
            newDays.forEach { newDay ->
                previousByWeekday[newDay.dayOfWeek]?.let { oldDay ->
                    val copied = planExerciseDao.getForOwner("WEEKLY_DAY", oldDay.id).map { old ->
                        old.copy(id = UUID.randomUUID().toString(), ownerId = newDay.id)
                    }
                    if (copied.isNotEmpty()) planExerciseDao.insertAll(copied)
                }
            }
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
            val planId = UUID.randomUUID().toString()
            dailyDao.insert(
                DailyWorkoutPlanEntity(
                    id = planId,
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
            val copiedActions = planExerciseDao.getForOwner("WEEKLY_DAY", day.id).map { action ->
                action.copy(id = UUID.randomUUID().toString(), ownerType = "DAILY_PLAN", ownerId = planId)
            }
            if (copiedActions.isNotEmpty()) planExerciseDao.insertAll(copiedActions)
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
        database.withTransaction {
            val plan = dailyDao.getById(id) ?: return@withTransaction false
            if (plan.businessDate != date.toString() || plan.isPrimary) return@withTransaction false
            planExerciseDao.deleteForOwner("DAILY_PLAN", id)
            dailyDao.deleteExtra(id, date.toString()) == 1
        }

    suspend fun setStatus(date: LocalDate, id: String, status: WorkoutPlanStatus): Boolean =
        dailyDao.updateStatus(id, date.toString(), status.name, clock.millis()) > 0

    suspend fun getPlanExercises(ownerType: String, ownerId: String): List<WorkoutPlanExerciseEntity> {
        require(ownerType == "WEEKLY_DAY" || ownerType == "DAILY_PLAN")
        return planExerciseDao.getForOwner(ownerType, ownerId)
    }

    /** Explicitly replaces one dated plan's actions with the current weekly template actions. */
    suspend fun importWeeklyActions(date: LocalDate, dailyPlanId: String) {
        database.withTransaction {
            val dailyPlan = dailyDao.getById(dailyPlanId) ?: error("当天训练安排不存在")
            require(dailyPlan.businessDate == date.toString()) { "训练日期不匹配" }
            val version = weeklyDao.getVersionForDate(date.toString()) ?: error("没有可用的每周训练模板")
            val day = weeklyDao.getDay(version.id, date.dayOfWeek.value) ?: error("每周模板缺少当天配置")
            val copied = planExerciseDao.getForOwner("WEEKLY_DAY", day.id).map { action ->
                action.copy(id = UUID.randomUUID().toString(), ownerType = "DAILY_PLAN", ownerId = dailyPlanId)
            }
            planExerciseDao.deleteForOwner("DAILY_PLAN", dailyPlanId)
            if (copied.isNotEmpty()) planExerciseDao.insertAll(copied)
        }
    }

    suspend fun savePlanExercises(ownerType: String, ownerId: String, actions: List<PlanActionInput>) {
        require(ownerType == "WEEKLY_DAY" || ownerType == "DAILY_PLAN")
        require(actions.all {
            it.name.isNotBlank() && it.targetSets in 1..30 &&
                runCatching { WorkoutInputRules.parseTargetReps(it.targetReps) }.isSuccess &&
                WorkoutInputRules.isValidWeight(it.targetWeight)
        }) {
            "动作名称、目标组数、次数或重量不合法"
        }
        database.withTransaction {
            planExerciseDao.deleteForOwner(ownerType, ownerId)
            val rows = actions.mapIndexed { index, action ->
                WorkoutPlanExerciseEntity(
                    id = UUID.randomUUID().toString(),
                    ownerType = ownerType,
                    ownerId = ownerId,
                    exerciseId = action.exerciseId,
                    exerciseNameSnapshot = action.name.trim(),
                    categorySnapshot = action.category.trim(),
                    equipmentSnapshot = action.equipment.trim(),
                    notesSnapshot = action.notes.trim(),
                    sortOrder = index,
                    targetSets = action.targetSets,
                    targetReps = action.targetReps.trim(),
                    targetWeight = action.targetWeight
                )
            }
            if (rows.isNotEmpty()) planExerciseDao.insertAll(rows)
        }
    }

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
