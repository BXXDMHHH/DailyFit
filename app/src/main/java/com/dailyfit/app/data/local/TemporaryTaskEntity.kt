package com.dailyfit.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "temporary_tasks",
    indices = [Index(value = ["businessDate"]), Index(value = ["businessDate", "isCompleted"])]
)
data class TemporaryTaskEntity(
    @PrimaryKey val id: String,
    val businessDate: String,
    val title: String,
    val isCompleted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long? = null
)
