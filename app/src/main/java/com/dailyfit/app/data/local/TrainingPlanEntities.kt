package com.dailyfit.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "weekly_workout_plan_versions",
    indices = [Index(value = ["effectiveFromDate", "createdAt"])]
)
data class WeeklyWorkoutPlanVersionEntity(
    @PrimaryKey val id: String,
    val effectiveFromDate: String,
    val createdAt: Long
)

@Entity(
    tableName = "weekly_workout_plan_days",
    foreignKeys = [
        ForeignKey(
            entity = WeeklyWorkoutPlanVersionEntity::class,
            parentColumns = ["id"],
            childColumns = ["versionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["versionId", "dayOfWeek"], unique = true),
        Index(value = ["versionId"])
    ]
)
data class WeeklyWorkoutPlanDayEntity(
    @PrimaryKey val id: String,
    val versionId: String,
    /** ISO-8601 weekday: Monday=1 ... Sunday=7. */
    val dayOfWeek: Int,
    val title: String,
    val notes: String,
    val isRestDay: Boolean
)

@Entity(
    tableName = "daily_workout_plans",
    indices = [
        Index(value = ["businessDate"]),
        Index(value = ["businessDate", "isPrimary"])
    ]
)
data class DailyWorkoutPlanEntity(
    @PrimaryKey val id: String,
    val businessDate: String,
    val title: String,
    val notes: String,
    /** PLANNED, COMPLETED, or SKIPPED. */
    val status: String,
    val isPrimary: Boolean,
    /** The immutable weekly template version from which this snapshot was generated. */
    val sourceVersionId: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "exercises",
    indices = [Index(value = ["name"])]
)
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val equipment: String,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long
)
