package com.dailyfit.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "workout_templates",
    indices = [Index(value = ["category"]), Index(value = ["name"])]
)
data class WorkoutTemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val description: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Dao
interface WorkoutTemplateDao {
    @Query("SELECT * FROM workout_templates ORDER BY category COLLATE NOCASE, name COLLATE NOCASE")
    fun observeAll(): Flow<List<WorkoutTemplateEntity>>

    @Query("SELECT * FROM workout_templates WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): WorkoutTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(template: WorkoutTemplateEntity)

    @Update
    suspend fun update(template: WorkoutTemplateEntity): Int

    @Query("DELETE FROM workout_templates WHERE id = :id")
    suspend fun delete(id: String): Int
}
