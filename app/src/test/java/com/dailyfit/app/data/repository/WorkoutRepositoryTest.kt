package com.dailyfit.app.data.repository
import androidx.room.Room
import com.dailyfit.app.data.local.*
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class WorkoutRepositoryTest {
 private lateinit var db:DailyFitDatabase
 private lateinit var repo:WorkoutRepository
 private val clock=Clock.fixed(Instant.parse("2026-11-22T10:00:00Z"),ZoneOffset.UTC)
 @Before fun setUp(){db=Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(),DailyFitDatabase::class.java).allowMainThreadQueries().build();repo=WorkoutRepository(db,clock)}
 @After fun tearDown(){db.close()}
 @Test fun autosavedSessionCanBeRecoveredAndCompletedAsImmutableSnapshot()= runBlocking {
  val plan=DailyWorkoutPlanEntity("plan-1","2026-11-22","上肢训练","", "PLANNED",true,null,1,1)
  val sessionId=repo.start(plan, emptyList())
  val exercise=ExerciseEntity("ex-1","杠铃卧推","胸部","杠铃","原始备注",1,1)
  repo.addExercise(sessionId,exercise,targetSets=2,targetReps="8",targetWeight=60.0)
  var row=db.workoutSessionDao().getExercises(sessionId).single()
  var sets=db.workoutSessionDao().getSets(row.id)
  repo.updateSet(sessionId,sets[0].id,62.5,8,true)
  val recovered=repo.recoverableSession()
  assertEquals(sessionId,recovered?.id)
  assertEquals(62.5,db.workoutSessionDao().getSets(row.id).first().actualWeight!!,0.0)
  val historyId=repo.finish(sessionId)
  db.exerciseDao().delete("ex-1")
  val history=repo.observeHistory().first().single()
  assertEquals(historyId,history.id)
  val historyExercise=repo.observeHistoryExercises(historyId).first().single()
  assertEquals("杠铃卧推",historyExercise.exerciseNameSnapshot)
  assertEquals("原始备注",historyExercise.notesSnapshot)
  val snapshotSet=repo.observeHistorySets(historyExercise.id).first().first()
  assertEquals(62.5,snapshotSet.actualWeight!!,0.0)
  assertTrue(snapshotSet.isCompleted)
  assertNull(repo.recoverableSession())
 }
 @Test fun finishedHistoryCannotBeUpdatedOrDeletedAtSqliteLevel()= runBlocking {
  val plan=DailyWorkoutPlanEntity("plan-2","2026-11-22","跑步","", "PLANNED",true,null,1,1)
  val sid=repo.start(plan, emptyList())
  val hid=repo.finish(sid)
  val blockedUpdate=runCatching{db.openHelper.writableDatabase.execSQL("UPDATE workout_history SET titleSnapshot='tampered' WHERE id=?",arrayOf(hid))}
  val blockedDelete=runCatching{db.openHelper.writableDatabase.execSQL("DELETE FROM workout_history WHERE id=?",arrayOf(hid))}
  assertTrue(blockedUpdate.isFailure);assertTrue(blockedDelete.isFailure)
  assertEquals("跑步",repo.observeHistory().first().single().titleSnapshot)
 }
 @Test fun incompleteAndSkippedArePersistedAsReadOnlyHistoryStatuses()= runBlocking {
  val p1=DailyWorkoutPlanEntity("p3","2026-11-22","力量","", "PLANNED",true,null,1,1)
  val s1=repo.start(p1,emptyList());repo.finish(s1,"INCOMPLETE")
  val p2=DailyWorkoutPlanEntity("p4","2026-11-22","恢复","", "PLANNED",false,null,1,1)
  val s2=repo.start(p2,emptyList());repo.finish(s2,"SKIPPED")
  assertEquals(setOf("INCOMPLETE","SKIPPED"),repo.observeHistory().first().map{it.status}.toSet())
 }
}
