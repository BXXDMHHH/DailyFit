package com.dailyfit.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Action configured for either a versioned weekly day (WEEKLY_DAY) or a dated plan (DAILY_PLAN).
 * The copied descriptive fields keep plans stable when the independent exercise library changes.
 */
@Entity(
    tableName = "workout_plan_exercises",
    indices = [Index(value = ["ownerType", "ownerId"]), Index(value = ["exerciseId"])]
)
data class WorkoutPlanExerciseEntity(
    @PrimaryKey val id: String,
    val ownerType: String,
    val ownerId: String,
    val exerciseId: String?,
    val exerciseNameSnapshot: String,
    val categorySnapshot: String,
    val equipmentSnapshot: String,
    val notesSnapshot: String,
    val sortOrder: Int,
    val targetSets: Int,
    val targetReps: String,
    val targetWeight: Double
)
