package com.dailyfit.app.data.repository

import androidx.room.Room
import com.dailyfit.app.data.local.DailyFitDatabase
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TrainingPlanRepositoryTest {
    private lateinit var database: DailyFitDatabase
    private lateinit var repository: TrainingPlanRepository
    private val clock = Clock.fixed(Instant.parse("2026-11-23T08:00:00Z"), ZoneId.of("Asia/Tokyo"))
    private val today = LocalDate.now(clock)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            DailyFitDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = TrainingPlanRepository(database, clock)
    }

    @After
    fun tearDown() { database.close() }

    @Test
    fun seedsSevenDayTemplateAndCreatesPrimaryPlanSnapshot() = runBlocking {
        repository.ensureInitialTemplate()
        val week = repository.observeWeeklyPlan(today).first()
        assertEquals(7, week.size)
        assertEquals((1..7).toList(), week.map { it.dayOfWeek })
        repository.ensureDailyPrimary(today)
        val primary = repository.observeDailyPlans(today).first().single()
        assertTrue(primary.isPrimary)
        assertEquals(week.first { it.dayOfWeek == today.dayOfWeek.value }.title, primary.title)
        assertEquals(week.first().versionId, primary.sourceVersionId)
    }

    @Test
    fun weeklyTemplateVersionOnlyAppliesFromEffectiveDateForward() = runBlocking {
        repository.ensureInitialTemplate()
        val oldMonday = repository.observeWeeklyPlan(today).first().first { it.dayOfWeek == 1 }
        val newWeek = (1..7).map { day ->
            WeeklyPlanInput(day, "新版训练 $day", isRestDay = day == 7)
        }
        val effective = today.plusDays(2)
        repository.saveWeeklyPlan(effective, newWeek)

        assertEquals(oldMonday.title, repository.observeWeeklyPlan(today).first().first { it.dayOfWeek == 1 }.title)
        assertEquals("新版训练 3", repository.observeWeeklyPlan(effective).first().first { it.dayOfWeek == 3 }.title)
        assertEquals(7, repository.observeWeeklyPlan(effective).first().size)
    }

    @Test
    fun editingOneDayDoesNotChangeWeeklyTemplateOrOtherDates() = runBlocking {
        repository.ensureInitialTemplate()
        val originalMonday = repository.observeWeeklyPlan(today).first().first { it.dayOfWeek == 1 }.title
        repository.ensureDailyPrimary(today)
        assertTrue(repository.updatePrimary(today, "当天临时调整", "仅今天"))
        val tomorrow = today.plusDays(1)
        repository.ensureDailyPrimary(tomorrow)

        assertEquals("当天临时调整", repository.observeDailyPlans(today).first().single().title)
        assertEquals(originalMonday, repository.observeWeeklyPlan(today).first().first { it.dayOfWeek == 1 }.title)
        assertEquals(
            repository.observeWeeklyPlan(tomorrow).first().first { it.dayOfWeek == 2 }.title,
            repository.observeDailyPlans(tomorrow).first().single().title
        )
    }

    @Test
    fun dailySnapshotDoesNotChangeWhenFutureWeeklyVersionIsAdded() = runBlocking {
        repository.ensureInitialTemplate()
        repository.ensureDailyPrimary(today)
        val before = repository.observeDailyPlans(today).first().single()
        repository.saveWeeklyPlan(today.plusDays(1), (1..7).map { WeeklyPlanInput(it, "替换 $it") })
        val after = repository.observeDailyPlans(today).first().single()
        assertEquals(before.title, after.title)
        assertEquals(before.sourceVersionId, after.sourceVersionId)
    }

    @Test
    fun supportsExtraPlansAndPlannedCompletedSkippedStates() = runBlocking {
        repository.ensureInitialTemplate()
        repository.ensureDailyPrimary(today)
        val extraId = repository.addExtra(today, "晚间慢跑")
        assertTrue(repository.setStatus(today, extraId, WorkoutPlanStatus.COMPLETED))
        assertTrue(repository.setStatus(today, extraId, WorkoutPlanStatus.SKIPPED))
        assertTrue(repository.setStatus(today, extraId, WorkoutPlanStatus.PLANNED))
        val extra = repository.observeDailyPlans(today).first().single { it.id == extraId }
        assertEquals("PLANNED", extra.status)
        assertFalse(extra.isPrimary)
        assertTrue(repository.deleteExtra(today, extraId))
        assertEquals(1, repository.observeDailyPlans(today).first().size)
    }

    @Test
    fun rejectsInvalidWeeklyTemplatesAndPastEffectiveDates() = runBlocking {
        repository.ensureInitialTemplate()
        assertTrue(runCatching {
            repository.saveWeeklyPlan(today, listOf(WeeklyPlanInput(1, "只有周一")))
        }.isFailure)
        assertTrue(runCatching {
            repository.saveWeeklyPlan(today.minusDays(1), (1..7).map { WeeklyPlanInput(it, "训练") })
        }.isFailure)
        assertTrue(runCatching {
            repository.saveWeeklyPlan(today, (1..7).map { WeeklyPlanInput(it, " ") })
        }.isFailure)
    }

    @Test
    fun exerciseLibrarySupportsCreateSearchUpdateAndDelete() = runBlocking {
        repository.saveExercise(null, "杠铃卧推", "胸部", "杠铃", "复合动作")
        val created = repository.observeExercises("卧推").first().single()
        assertTrue(created.id.isNotBlank())
        repository.saveExercise(created.id, "哑铃卧推", "胸部", "哑铃", "修改后")
        val updated = repository.observeExercises("哑铃").first().single()
        assertEquals("哑铃卧推", updated.name)
        assertEquals("修改后", updated.notes)
        assertTrue(repository.deleteExercise(updated.id))
        assertTrue(repository.observeExercises("").first().isEmpty())
    }

    @Test
    fun blankExerciseNameIsRejected() = runBlocking {
        assertTrue(runCatching { repository.saveExercise(null, "  ", "", "", "") }.isFailure)
        assertTrue(repository.observeExercises("").first().isEmpty())
    }

    @Test
    fun migrationFromVersionThreeAddsNewTablesAndPreservesExistingRows() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val name = "dailyfit-v3-to-v4-test.db"
        context.deleteDatabase(name)
        val old = context.openOrCreateDatabase(name, 0, null)
        old.execSQL("CREATE TABLE app_metadata (`key` TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL)")
        old.execSQL("INSERT INTO app_metadata (`key`, value) VALUES ('existing', 'kept')")
        old.execSQL("CREATE TABLE habits (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, frequency TEXT NOT NULL, customDays TEXT NOT NULL, isActive INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
        old.execSQL("CREATE TABLE habit_records (id TEXT NOT NULL PRIMARY KEY, habitId TEXT NOT NULL, businessDate TEXT NOT NULL, habitNameSnapshot TEXT NOT NULL, isCompleted INTEGER NOT NULL, completedAt INTEGER, createdAt INTEGER NOT NULL, FOREIGN KEY(habitId) REFERENCES habits(id) ON UPDATE NO ACTION ON DELETE RESTRICT)")
        old.execSQL("CREATE UNIQUE INDEX index_habit_records_habitId_businessDate ON habit_records(habitId, businessDate)")
        old.execSQL("CREATE INDEX index_habit_records_businessDate ON habit_records(businessDate)")
        old.execSQL("CREATE TABLE temporary_tasks (id TEXT NOT NULL PRIMARY KEY, businessDate TEXT NOT NULL, title TEXT NOT NULL, isCompleted INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, completedAt INTEGER)")
        old.execSQL("CREATE INDEX index_temporary_tasks_businessDate ON temporary_tasks(businessDate)")
        old.execSQL("CREATE INDEX index_temporary_tasks_businessDate_isCompleted ON temporary_tasks(businessDate, isCompleted)")
        old.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
        old.execSQL("INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, 'room-generated-during-ci')")
        old.version = 3
        old.close()

        val migrated = Room.databaseBuilder(context, DailyFitDatabase::class.java, name)
            .addMigrations(DailyFitDatabase.MIGRATION_1_2, DailyFitDatabase.MIGRATION_2_3, DailyFitDatabase.MIGRATION_3_4)
            .build()
        try {
            assertEquals("kept", migrated.appMetadataDao().getValue("existing"))
            assertTrue(migrated.weeklyWorkoutPlanDao().observeVersionForDate(today.toString()).first() == null)
            assertTrue(migrated.dailyWorkoutPlanDao().observeForDate(today.toString()).first().isEmpty())
            assertTrue(migrated.exerciseDao().observeAll().first().isEmpty())
        } finally {
            migrated.close()
            context.deleteDatabase(name)
        }
    }
}
