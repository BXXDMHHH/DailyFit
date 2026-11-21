package com.dailyfit.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val frequency: String,
    /** Comma-separated ISO weekdays (1=Monday ... 7=Sunday); empty for non-custom schedules. */
    val customDays: String,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "habit_records",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["habitId", "businessDate"], unique = true),
        Index(value = ["businessDate"])
    ]
)
data class HabitRecordEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val businessDate: String,
    val habitNameSnapshot: String,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long
)
