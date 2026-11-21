package com.dailyfit.app.data.repository
import androidx.room.Room
import com.dailyfit.app.data.local.*
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
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
 @Before fun setUp(){db=Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(),DailyFitDatabase::class.java).addCallback(DailyFitDatabase.CALLBACK).allowMainThreadQueries().build();repo=WorkoutRepository(db,clock)}
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
  repo.addExercise(sid, ExerciseEntity("history-ex", "跑步", "有氧", "跑步机", "", 1, 1))
  val sessionExercise=db.workoutSessionDao().getExercises(sid).single()
  val sessionSet=db.workoutSessionDao().getSets(sessionExercise.id).single()
  repo.updateSet(sid, sessionSet.id, 8.0, 5, true)
  val hid=repo.finish(sid)
  val historyExercise=repo.observeHistoryExercises(hid).first().single()
  val historySet=repo.observeHistorySets(historyExercise.id).first().single()

  val blocked = listOf(
   runCatching{db.openHelper.writableDatabase.execSQL("UPDATE workout_history SET titleSnapshot='tampered' WHERE id=?",arrayOf(hid))},
   runCatching{db.openHelper.writableDatabase.execSQL("DELETE FROM workout_history WHERE id=?",arrayOf(hid))},
   runCatching{db.openHelper.writableDatabase.execSQL("UPDATE workout_history_exercises SET exerciseNameSnapshot='tampered' WHERE id=?",arrayOf(historyExercise.id))},
   runCatching{db.openHelper.writableDatabase.execSQL("DELETE FROM workout_history_exercises WHERE id=?",arrayOf(historyExercise.id))},
   runCatching{db.openHelper.writableDatabase.execSQL("UPDATE workout_history_sets SET actualReps=999 WHERE id=?",arrayOf(historySet.id))},
   runCatching{db.openHelper.writableDatabase.execSQL("DELETE FROM workout_history_sets WHERE id=?",arrayOf(historySet.id))}
  )
  assertTrue(blocked.all { it.isFailure })
  assertEquals("跑步",repo.observeHistory().first().single().titleSnapshot)
  assertEquals("跑步",repo.observeHistoryExercises(hid).first().single().exerciseNameSnapshot)
  assertEquals(5,repo.observeHistorySets(historyExercise.id).first().single().actualReps)
 }
 @Test fun incompleteAndSkippedArePersistedAsReadOnlyHistoryStatuses()= runBlocking {
  val p1=DailyWorkoutPlanEntity("p3","2026-11-22","力量","", "PLANNED",true,null,1,1)
  val s1=repo.start(p1,emptyList());repo.finish(s1,"INCOMPLETE")
  val p2=DailyWorkoutPlanEntity("p4","2026-11-22","恢复","", "PLANNED",false,null,1,1)
  val s2=repo.start(p2,emptyList());repo.finish(s2,"SKIPPED")
  assertEquals(setOf("INCOMPLETE","SKIPPED"),repo.observeHistory().first().map{it.status}.toSet())
 }
 @Test fun weeklyPlanActionsAreSnapshottedIntoDailyPlanAndWorkoutSession() = runBlocking {
  val planRepository = TrainingPlanRepository(db, clock)
  planRepository.ensureInitialTemplate()
  val monday = LocalDate.parse("2026-11-23")
  val initialDays = planRepository.observeWeeklyPlan(monday).first()
  val mondayTemplate = initialDays.single { it.dayOfWeek == 1 }
  // The app may already have materialized today's plan before the user configures the template.
  planRepository.ensureDailyPrimary(monday)

  planRepository.saveExercise(null, "杠铃卧推", "胸部", "杠铃", "平板卧推")
  val exercise = planRepository.observeExercises("").first().single()
  planRepository.savePlanExercises(
   "WEEKLY_DAY",
   mondayTemplate.id,
   listOf(PlanActionInput(exercise.id, exercise.name, exercise.category, exercise.equipment, exercise.notes, 4, "8-10", 60.0))
  )

  // Creating a new weekly version must preserve action configuration for each weekday.
  planRepository.saveWeeklyPlan(
   monday,
   initialDays.map { WeeklyPlanInput(it.dayOfWeek, it.title, it.notes, it.isRestDay) }
  )
  val updatedMonday = planRepository.observeWeeklyPlan(monday).first().single { it.dayOfWeek == 1 }
  val weeklyActions = planRepository.getPlanExercises("WEEKLY_DAY", updatedMonday.id)
  assertEquals(1, weeklyActions.size)
  assertEquals(60.0, weeklyActions.single().targetWeight, 0.0)

  val dailyPlan = planRepository.observeDailyPlans(monday).first().single()
  assertTrue(planRepository.getPlanExercises("DAILY_PLAN", dailyPlan.id).isEmpty())
  planRepository.importWeeklyActions(monday, dailyPlan.id)
  val copiedActions = planRepository.getPlanExercises("DAILY_PLAN", dailyPlan.id)
  assertEquals(60.0, copiedActions.single().targetWeight, 0.0)

  // A dated override stays independent from the weekly template.
  planRepository.savePlanExercises(
   "DAILY_PLAN",
   dailyPlan.id,
   listOf(PlanActionInput(exercise.id, exercise.name, exercise.category, exercise.equipment, exercise.notes, 3, "6-8", 65.0))
  )
  assertEquals(60.0, planRepository.getPlanExercises("WEEKLY_DAY", updatedMonday.id).single().targetWeight, 0.0)

  val sessionId = repo.start(dailyPlan, planRepository.getPlanExercises("DAILY_PLAN", dailyPlan.id))
  val sessionExercise = db.workoutSessionDao().getExercises(sessionId).single()
  assertEquals("杠铃卧推", sessionExercise.exerciseNameSnapshot)
  assertEquals(3, sessionExercise.targetSets)
  assertEquals("6-8", sessionExercise.targetReps)
  assertEquals(65.0, sessionExercise.targetWeight, 0.0)
  val sessionSets = db.workoutSessionDao().getSets(sessionExercise.id)
  assertEquals(3, sessionSets.size)
  assertEquals(6, sessionSets.first().targetReps)
  assertEquals(65.0, sessionSets.first().targetWeight, 0.0)

  val historyId = repo.finish(sessionId)
  val historyExercise = repo.observeHistoryExercises(historyId).first().single()
  assertEquals("杠铃卧推", historyExercise.exerciseNameSnapshot)
  assertEquals(65.0, historyExercise.targetWeight, 0.0)
 }

 @Test fun workoutHistoryStatisticsAggregateCompletedSessionsSetsAndVolume() = runBlocking {
  val plan = DailyWorkoutPlanEntity("stats-plan", "2026-11-22", "力量训练", "", "PLANNED", true, null, 1, 1)
  val sessionId = repo.start(plan)
  repo.addExercise(
   sessionId,
   ExerciseEntity("stats-exercise", "深蹲", "腿部", "杠铃", "", 1, 1),
   targetSets = 2,
   targetReps = "8-10",
   targetWeight = 50.0
  )
  val exercise = db.workoutSessionDao().getExercises(sessionId).single()
  val sets = db.workoutSessionDao().getSets(exercise.id)
  repo.updateSet(sessionId, sets[0].id, 50.0, 10, true)
  repo.updateSet(sessionId, sets[1].id, 55.0, 8, true)
  repo.finish(sessionId)

  val stats = db.workoutSessionDao().observeStats().first()
  assertEquals(1, stats.totalSessions)
  assertEquals(1, stats.completedSessions)
  assertEquals(2, stats.completedSets)
  assertEquals(940.0, stats.totalVolume, 0.0)
 }

 @Test fun discardingUnfinishedWorkoutDeletesDraftWithoutCreatingHistory() = runBlocking {
  val plan = DailyWorkoutPlanEntity("discard-plan", "2026-11-22", "力量", "", "PLANNED", true, null, 1, 1)
  val sessionId = repo.start(plan)
  repo.addExercise(sessionId, ExerciseEntity("discard-ex", "深蹲", "腿部", "杠铃", "", 1, 1))
  repo.discard(sessionId)
  assertNull(repo.recoverableSession())
  assertTrue(repo.observeHistory().first().isEmpty())
  assertNull(db.workoutSessionDao().get(sessionId))
 }

 @Test fun commonExerciseLibrarySeedsOnceAndDoesNotRestoreUserDeletedItems() = runBlocking {
  val planRepository = TrainingPlanRepository(db, clock)
  planRepository.ensureInitialTemplate()
  val firstSeed = planRepository.observeExercises("").first()
  assertTrue(firstSeed.map { it.name }.containsAll(listOf("卧推", "深蹲", "硬拉", "杠铃划船", "高位下拉", "肩推", "侧平举")))
  val deletedId = firstSeed.first().id
  planRepository.deleteExercise(deletedId)
  planRepository.ensureInitialTemplate()
  assertFalse(planRepository.observeExercises("").first().any { it.id == deletedId })
 }

}
