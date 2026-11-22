package com.dailyfit.app.data.local
import androidx.room.*
import kotlinx.coroutines.flow.Flow
@Dao interface WorkoutSessionDao {
 @Query("SELECT * FROM workout_sessions WHERE status='IN_PROGRESS' ORDER BY updatedAt DESC LIMIT 1") fun observeActive():Flow<WorkoutSessionEntity?>
 @Query("SELECT * FROM workout_sessions WHERE status='IN_PROGRESS' ORDER BY updatedAt DESC LIMIT 1") suspend fun getActive():WorkoutSessionEntity?
 @Query("SELECT * FROM workout_sessions WHERE id=:id") suspend fun get(id:String):WorkoutSessionEntity?
 @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun insertSession(row:WorkoutSessionEntity)
 @Query("DELETE FROM workout_session_sets WHERE sessionExerciseId IN (SELECT id FROM workout_session_exercises WHERE sessionId=:sessionId)")
 suspend fun deleteSetsForSession(sessionId:String):Int
 @Query("DELETE FROM workout_session_exercises WHERE sessionId=:sessionId")
 suspend fun deleteExercisesForSession(sessionId:String):Int
 @Query("DELETE FROM workout_sessions WHERE id=:sessionId AND status='IN_PROGRESS'")
 suspend fun deleteActiveSession(sessionId:String):Int
 @Query("UPDATE workout_sessions SET updatedAt=:at WHERE id=:id AND status='IN_PROGRESS'") suspend fun touch(id:String,at:Long):Int
 @Query("UPDATE workout_sessions SET title=:title,updatedAt=:at WHERE id=:id AND status='IN_PROGRESS'") suspend fun rename(id:String,title:String,at:Long):Int
 @Query("UPDATE workout_sessions SET status=:status,updatedAt=:at WHERE id=:id AND status='IN_PROGRESS'") suspend fun setStatus(id:String,status:String,at:Long):Int
 @Query("SELECT * FROM workout_session_exercises WHERE sessionId=:id ORDER BY sortOrder") suspend fun getExercises(id:String):List<WorkoutSessionExerciseEntity>
 @Query("SELECT * FROM workout_session_exercises WHERE sessionId=:id ORDER BY sortOrder") fun observeExercises(id:String):Flow<List<WorkoutSessionExerciseEntity>>
 @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun insertExercise(row:WorkoutSessionExerciseEntity)
 @Query("DELETE FROM workout_session_sets WHERE sessionExerciseId=:id AND EXISTS(SELECT 1 FROM workout_session_exercises WHERE id=:id AND sessionId=:sessionId) AND EXISTS(SELECT 1 FROM workout_sessions WHERE id=:sessionId AND status='IN_PROGRESS')") suspend fun deleteSetsForExercise(id:String,sessionId:String):Int
 @Query("DELETE FROM workout_session_exercises WHERE id=:id AND sessionId=:sessionId AND EXISTS(SELECT 1 FROM workout_sessions WHERE id=:sessionId AND status='IN_PROGRESS')") suspend fun deleteExercise(id:String,sessionId:String):Int
 @Query("SELECT * FROM workout_session_exercises WHERE id=:exerciseId AND sessionId=:sessionId LIMIT 1") suspend fun getExercise(exerciseId:String,sessionId:String):WorkoutSessionExerciseEntity?
 @Query("SELECT * FROM workout_session_sets WHERE sessionExerciseId=:id ORDER BY setNumber") suspend fun getSets(id:String):List<WorkoutSessionSetEntity>
 @Query("SELECT * FROM workout_session_sets WHERE sessionExerciseId=:exerciseId AND EXISTS(SELECT 1 FROM workout_session_exercises WHERE id=:exerciseId AND sessionId=:sessionId) ORDER BY setNumber") suspend fun getSetsForExercise(exerciseId:String,sessionId:String):List<WorkoutSessionSetEntity>
 @Query("SELECT * FROM workout_session_sets WHERE sessionExerciseId=:id ORDER BY setNumber") fun observeSets(id:String):Flow<List<WorkoutSessionSetEntity>>
 @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun insertSets(rows:List<WorkoutSessionSetEntity>)
 @Query("UPDATE workout_session_sets SET actualWeight=:weight,actualReps=:reps,isCompleted=:done,updatedAt=:at WHERE id=:id AND sessionExerciseId IN (SELECT id FROM workout_session_exercises WHERE sessionId=:sessionId) AND EXISTS(SELECT 1 FROM workout_sessions WHERE id=:sessionId AND status='IN_PROGRESS')") suspend fun updateSet(id:String,sessionId:String,weight:Double?,reps:Int?,done:Boolean,at:Long):Int
 @Query("DELETE FROM workout_session_sets WHERE id=:id AND sessionExerciseId IN (SELECT id FROM workout_session_exercises WHERE sessionId=:sessionId) AND EXISTS(SELECT 1 FROM workout_sessions WHERE id=:sessionId AND status='IN_PROGRESS')") suspend fun deleteSet(id:String,sessionId:String):Int
 @Query("SELECT (SELECT COUNT(*) FROM workout_history) AS totalSessions, (SELECT COUNT(*) FROM workout_history WHERE status='COMPLETED') AS completedSessions, (SELECT COUNT(*) FROM workout_history_sets WHERE isCompleted=1) AS completedSets, (SELECT COALESCE(SUM(actualWeight * actualReps), 0) FROM workout_history_sets WHERE isCompleted=1 AND actualWeight IS NOT NULL AND actualReps IS NOT NULL) AS totalVolume")
 fun observeStats():Flow<WorkoutHistoryStats>
 @Query("SELECT * FROM workout_history ORDER BY businessDate DESC,completedAt DESC") fun observeHistory():Flow<List<WorkoutHistoryEntity>>
 @Query("SELECT * FROM workout_history WHERE id=:id") fun observeHistoryById(id:String):Flow<WorkoutHistoryEntity?>
 @Query("SELECT * FROM workout_history_exercises WHERE historyId=:id ORDER BY sortOrder") fun observeHistoryExercises(id:String):Flow<List<WorkoutHistoryExerciseEntity>>
 @Query("SELECT * FROM workout_history_sets WHERE historyExerciseId=:id ORDER BY setNumber") fun observeHistorySets(id:String):Flow<List<WorkoutHistorySetEntity>>
 @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun insertHistory(row:WorkoutHistoryEntity)
 @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun insertHistoryExercises(rows:List<WorkoutHistoryExerciseEntity>)
 @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun insertHistorySets(rows:List<WorkoutHistorySetEntity>)
 @Query("SELECT * FROM workout_history WHERE sessionId=:id LIMIT 1") suspend fun historyForSession(id:String):WorkoutHistoryEntity?
}
