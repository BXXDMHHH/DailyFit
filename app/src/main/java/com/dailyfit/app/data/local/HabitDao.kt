package com.dailyfit.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY isActive DESC, createdAt ASC")
    fun observeAll(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE isActive = 1 ORDER BY createdAt ASC")
    suspend fun getActive(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): HabitEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(habit: HabitEntity)

    @Query("""
        UPDATE habits
        SET name = :name, frequency = :frequency, customDays = :customDays, updatedAt = :updatedAt
        WHERE id = :id AND isActive = 1
    """)
    suspend fun updateDefinition(
        id: String,
        name: String,
        frequency: String,
        customDays: String,
        updatedAt: Long
    ): Int

    @Query("UPDATE habits SET isActive = 0, updatedAt = :updatedAt WHERE id = :id AND isActive = 1")
    suspend fun deactivate(id: String, updatedAt: Long): Int
}

@Dao
interface HabitRecordDao {
    @Query("SELECT * FROM habit_records WHERE businessDate = :date ORDER BY createdAt ASC")
    fun observeForDate(date: String): Flow<List<HabitRecordEntity>>

    @Query("SELECT * FROM habit_records ORDER BY businessDate DESC, createdAt DESC")
    fun observeAll(): Flow<List<HabitRecordEntity>>

    @Query("SELECT * FROM habit_records WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): HabitRecordEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(record: HabitRecordEntity): Long

    /** Only today's record can be checked or unchecked; past records are immutable. */
    @Query("""
        UPDATE habit_records
        SET isCompleted = :completed, completedAt = :completedAt
        WHERE id = :id AND businessDate = :today
    """)
    suspend fun setCompletion(id: String, today: String, completed: Boolean, completedAt: Long?): Int
}
