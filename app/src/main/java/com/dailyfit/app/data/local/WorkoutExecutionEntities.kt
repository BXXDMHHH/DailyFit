package com.dailyfit.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "workout_sessions", indices = [Index("businessDate"), Index("status")])
data class WorkoutSessionEntity(
 @PrimaryKey val id:String, val planId:String?, val businessDate:String, val title:String,
 val status:String, val startedAt:Long, val updatedAt:Long
)
@Entity(tableName = "workout_session_exercises", indices=[Index("sessionId"),Index("exerciseId")])
data class WorkoutSessionExerciseEntity(
 @PrimaryKey val id:String, val sessionId:String, val exerciseId:String?,
 val exerciseNameSnapshot:String, val categorySnapshot:String, val equipmentSnapshot:String,
 val notesSnapshot:String, val sortOrder:Int, val targetSets:Int, val targetReps:String,
 val targetWeight:Double, val isTemporary:Boolean
)
@Entity(tableName = "workout_session_sets", indices=[Index("sessionExerciseId")])
data class WorkoutSessionSetEntity(
 @PrimaryKey val id:String, val sessionExerciseId:String, val setNumber:Int,
 val targetReps:Int, val targetWeight:Double, val actualReps:Int?, val actualWeight:Double?,
 val isCompleted:Boolean, val updatedAt:Long
)
@Entity(tableName = "workout_history", indices=[Index("businessDate"),Index("completedAt")])
data class WorkoutHistoryEntity(
 @PrimaryKey val id:String, val sessionId:String, val planId:String?, val businessDate:String,
 val titleSnapshot:String, val status:String, val startedAt:Long, val completedAt:Long,
 val durationSeconds:Long
)
@Entity(tableName = "workout_history_exercises", indices=[Index("historyId")])
data class WorkoutHistoryExerciseEntity(
 @PrimaryKey val id:String, val historyId:String, val sourceExerciseId:String?,
 val exerciseNameSnapshot:String, val categorySnapshot:String, val equipmentSnapshot:String,
 val notesSnapshot:String, val sortOrder:Int, val targetSets:Int, val targetReps:String,
 val targetWeight:Double, val isTemporary:Boolean
)
@Entity(tableName = "workout_history_sets", indices=[Index("historyExerciseId")])
data class WorkoutHistorySetEntity(
 @PrimaryKey val id:String, val historyExerciseId:String, val setNumber:Int,
 val targetReps:Int, val targetWeight:Double, val actualReps:Int?, val actualWeight:Double?,
 val isCompleted:Boolean
)


data class WorkoutHistoryStats(
    val totalSessions: Int,
    val completedSessions: Int,
    val completedSets: Int,
    val totalVolume: Double
)
