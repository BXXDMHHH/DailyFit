package com.dailyfit.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TemporaryTaskDao {
    @Query("SELECT * FROM temporary_tasks WHERE businessDate = :date ORDER BY createdAt ASC")
    fun observeForDate(date: String): Flow<List<TemporaryTaskEntity>>

    @Query("SELECT * FROM temporary_tasks ORDER BY businessDate DESC, createdAt DESC")
    fun observeAll(): Flow<List<TemporaryTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(task: TemporaryTaskEntity)

    @Query("""
        UPDATE temporary_tasks
        SET title = :title, updatedAt = :updatedAt
        WHERE id = :id AND businessDate = :today
    """)
    suspend fun updateToday(id: String, today: String, title: String, updatedAt: Long): Int

    @Query("""
        UPDATE temporary_tasks
        SET isCompleted = :completed, completedAt = :completedAt, updatedAt = :updatedAt
        WHERE id = :id AND businessDate = :today
    """)
    suspend fun setTodayCompletion(
        id: String,
        today: String,
        completed: Boolean,
        completedAt: Long?,
        updatedAt: Long
    ): Int

    @Query("DELETE FROM temporary_tasks WHERE id = :id AND businessDate = :today")
    suspend fun deleteToday(id: String, today: String): Int
}
