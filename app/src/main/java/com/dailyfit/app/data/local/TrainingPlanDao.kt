package com.dailyfit.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WeeklyWorkoutPlanDao {
    @Query("""
        SELECT * FROM weekly_workout_plan_versions
        WHERE effectiveFromDate <= :date
        ORDER BY effectiveFromDate DESC, createdAt DESC
        LIMIT 1
    """)
    suspend fun getVersionForDate(date: String): WeeklyWorkoutPlanVersionEntity?

    @Query("""
        SELECT * FROM weekly_workout_plan_versions
        WHERE effectiveFromDate <= :date
        ORDER BY effectiveFromDate DESC, createdAt DESC
        LIMIT 1
    """)
    fun observeVersionForDate(date: String): Flow<WeeklyWorkoutPlanVersionEntity?>

    @Query("SELECT * FROM weekly_workout_plan_days WHERE versionId = :versionId ORDER BY dayOfWeek")
    fun observeDays(versionId: String): Flow<List<WeeklyWorkoutPlanDayEntity>>

    @Query("SELECT * FROM weekly_workout_plan_days WHERE versionId = :versionId AND dayOfWeek = :day LIMIT 1")
    suspend fun getDay(versionId: String, day: Int): WeeklyWorkoutPlanDayEntity?

    @Query("SELECT * FROM weekly_workout_plan_days WHERE versionId = :versionId ORDER BY dayOfWeek")
    suspend fun getDays(versionId: String): List<WeeklyWorkoutPlanDayEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertVersion(version: WeeklyWorkoutPlanVersionEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDays(days: List<WeeklyWorkoutPlanDayEntity>)
}

@Dao
interface DailyWorkoutPlanDao {
    @Query("SELECT * FROM daily_workout_plans WHERE businessDate = :date ORDER BY isPrimary DESC, createdAt ASC")
    fun observeForDate(date: String): Flow<List<DailyWorkoutPlanEntity>>

    @Query("SELECT * FROM daily_workout_plans WHERE businessDate = :date AND isPrimary = 1 LIMIT 1")
    suspend fun getPrimary(date: String): DailyWorkoutPlanEntity?

    @Query("SELECT * FROM daily_workout_plans WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DailyWorkoutPlanEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(plan: DailyWorkoutPlanEntity)

    @Query("""
        UPDATE daily_workout_plans
        SET title = :title, notes = :notes, updatedAt = :updatedAt
        WHERE id = :id AND businessDate = :date AND isPrimary = 1
    """)
    suspend fun updatePrimary(id: String, date: String, title: String, notes: String, updatedAt: Long): Int

    @Query("""
        UPDATE daily_workout_plans
        SET status = :status, updatedAt = :updatedAt
        WHERE id = :id AND businessDate = :date
    """)
    suspend fun updateStatus(id: String, date: String, status: String, updatedAt: Long): Int

    @Query("""
        UPDATE daily_workout_plans
        SET title = :title, notes = :notes, updatedAt = :updatedAt
        WHERE id = :id AND businessDate = :date AND isPrimary = 0
    """)
    suspend fun updateExtra(id: String, date: String, title: String, notes: String, updatedAt: Long): Int

    @Query("DELETE FROM daily_workout_plans WHERE id = :id AND businessDate = :date AND isPrimary = 0")
    suspend fun deleteExtra(id: String, date: String): Int
}

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE")
    fun search(query: String): Flow<List<ExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(exercise: ExerciseEntity)

    @Update
    suspend fun update(exercise: ExerciseEntity): Int

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun delete(id: String): Int

    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ExerciseEntity?
}


@Dao
interface WorkoutPlanExerciseDao {
    @Query("SELECT * FROM workout_plan_exercises WHERE ownerType = :ownerType AND ownerId = :ownerId ORDER BY sortOrder")
    suspend fun getForOwner(ownerType: String, ownerId: String): List<WorkoutPlanExerciseEntity>

    @Query("DELETE FROM workout_plan_exercises WHERE ownerType = :ownerType AND ownerId = :ownerId")
    suspend fun deleteForOwner(ownerType: String, ownerId: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(rows: List<WorkoutPlanExerciseEntity>)
}
