package com.dailyfit.app.data.repository

import androidx.room.Room
import com.dailyfit.app.data.local.DailyFitDatabase
import com.dailyfit.app.domain.model.HabitFrequency
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
import org.robolectric.annotation.Config
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TemporaryTaskRepositoryTest {
    private lateinit var database: DailyFitDatabase
    private lateinit var habitRepository: HabitRepository
    private lateinit var repository: TemporaryTaskRepository
    private val fixedClock = Clock.fixed(Instant.parse("2026-11-23T08:00:00Z"), ZoneId.of("Asia/Tokyo"))
    private val today = LocalDate.now(fixedClock)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            DailyFitDatabase::class.java
        ).allowMainThreadQueries().build()
        habitRepository = HabitRepository(database, fixedClock)
        repository = TemporaryTaskRepository(database, fixedClock)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun createsTaskWithStableIdAndOnlyShowsItOnCreationDate() = runBlocking {
        repository.createTask("买牛奶")
        val task = repository.observeDashboard(today).first().tasks.single()
        assertEquals("买牛奶", task.title)
        assertTrue(task.id.isNotBlank())
        assertEquals(today.toString(), task.businessDate)
        assertTrue(repository.observeDashboard(today.plusDays(1)).first().tasks.isEmpty())
        assertEquals(1, repository.observeDashboard(today.plusDays(1)).first().allTotal)
    }

    @Test
    fun taskCanBeCompletedAndUnchecked() = runBlocking {
        repository.createTask("提交作业")
        val task = repository.observeDashboard(today).first().tasks.single()
        assertTrue(repository.setCompletion(task.id, true))
        assertTrue(repository.observeDashboard(today).first().tasks.single().isCompleted)
        assertTrue(repository.setCompletion(task.id, false))
        val updated = repository.observeDashboard(today).first().tasks.single()
        assertFalse(updated.isCompleted)
        assertEquals(null, updated.completedAt)
    }

    @Test
    fun taskCanBeEditedAndDeletedOnCreationDate() = runBlocking {
        repository.createTask("旧任务")
        val task = repository.observeDashboard(today).first().tasks.single()
        assertTrue(repository.updateTask(task.id, "新任务"))
        assertEquals("新任务", repository.observeDashboard(today).first().tasks.single().title)
        assertTrue(repository.deleteTask(task.id))
        assertTrue(repository.observeDashboard(today).first().tasks.isEmpty())
    }

    @Test
    fun blankTaskTitleIsRejected() = runBlocking {
        val result = runCatching { repository.createTask("   ") }
        assertTrue(result.isFailure)
        assertTrue(repository.observeDashboard(today).first().tasks.isEmpty())
    }

    @Test
    fun oldTasksCannotBeEditedCompletedOrDeletedOnAnotherDate() = runBlocking {
        repository.createTask("昨天任务")
        val task = repository.observeDashboard(today).first().tasks.single()
        val nextClock = MutableTaskClock(
            Instant.parse("2026-11-24T08:00:00Z"),
            ZoneId.of("Asia/Tokyo")
        )
        val nextDayRepository = TemporaryTaskRepository(database, nextClock)

        assertFalse(nextDayRepository.updateTask(task.id, "不允许修改"))
        assertFalse(nextDayRepository.setCompletion(task.id, true))
        assertFalse(nextDayRepository.deleteTask(task.id))
        val history = repository.observeDashboard(today).first().tasks.single()
        assertEquals("昨天任务", history.title)
        assertFalse(history.isCompleted)
    }

    @Test
    fun tasksDoNotCarryOverAndRemainInIndependentHistoricalStatistics() = runBlocking {
        repository.createTask("当天任务")
        val task = repository.observeDashboard(today).first().tasks.single()
        repository.setCompletion(task.id, true)

        val nextClock = MutableTaskClock(
            Instant.parse("2026-11-24T08:00:00Z"),
            ZoneId.of("Asia/Tokyo")
        )
        val nextDayRepository = TemporaryTaskRepository(database, nextClock)
        val nextDay = LocalDate.now(nextClock)
        val dashboard = nextDayRepository.observeDashboard(nextDay).first()

        assertTrue(dashboard.tasks.isEmpty())
        assertEquals(1, dashboard.allTotal)
        assertEquals(1, dashboard.allCompleted)
        assertEquals(100, dashboard.overallCompletionPercent)
    }

    @Test
    fun temporaryTasksDoNotAffectFixedHabitCompletionStatistics() = runBlocking {
        habitRepository.createHabit("每日阅读", HabitFrequency.DAILY, emptySet())
        val before = habitRepository.observeDashboard(today).first()
        assertEquals(1, before.allTotal)
        repository.createTask("整理桌面")
        repository.createTask("回复邮件")
        repository.setCompletion(repository.observeDashboard(today).first().tasks.first().id, true)

        val after = habitRepository.observeDashboard(today).first()
        val taskStats = repository.observeDashboard(today).first()
        assertEquals(1, after.allTotal)
        assertEquals(0, after.allCompleted)
        assertEquals(50, taskStats.todayCompletionPercent)
        assertEquals(1, taskStats.allCompleted)
        assertEquals(2, taskStats.allTotal)
    }

    @Test
    fun migrationFromVersionTwoCreatesTaskTableAndPreservesHabitData() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val databaseName = "dailyfit-task-migration-test.db"
        context.deleteDatabase(databaseName)
        val oldDatabase = context.openOrCreateDatabase(databaseName, 0, null)
        oldDatabase.execSQL("""
            CREATE TABLE habits (
                id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, frequency TEXT NOT NULL,
                customDays TEXT NOT NULL, isActive INTEGER NOT NULL, createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """.trimIndent())
        oldDatabase.execSQL("""
            CREATE TABLE habit_records (
                id TEXT NOT NULL PRIMARY KEY, habitId TEXT NOT NULL, businessDate TEXT NOT NULL,
                habitNameSnapshot TEXT NOT NULL, isCompleted INTEGER NOT NULL, completedAt INTEGER,
                createdAt INTEGER NOT NULL, FOREIGN KEY(habitId) REFERENCES habits(id)
                ON UPDATE NO ACTION ON DELETE RESTRICT
            )
        """.trimIndent())
        oldDatabase.execSQL("CREATE UNIQUE INDEX index_habit_records_habitId_businessDate ON habit_records(habitId, businessDate)")
        oldDatabase.execSQL("CREATE INDEX index_habit_records_businessDate ON habit_records(businessDate)")
        oldDatabase.execSQL("""
            CREATE TABLE app_metadata (`key` TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL)
        """.trimIndent())
        oldDatabase.execSQL("INSERT INTO app_metadata (`key`, value) VALUES ('kept', 'yes')")
        oldDatabase.execSQL("""
            INSERT INTO habits (id, name, frequency, customDays, isActive, createdAt, updatedAt)
            VALUES ('habit-1', '阅读', 'DAILY', '', 1, 1, 1)
        """.trimIndent())
        oldDatabase.version = 2
        oldDatabase.close()

        val migrated = Room.databaseBuilder(context, DailyFitDatabase::class.java, databaseName)
            .addMigrations(
                DailyFitDatabase.MIGRATION_1_2,
                DailyFitDatabase.MIGRATION_2_3,
                DailyFitDatabase.MIGRATION_3_4,
                DailyFitDatabase.MIGRATION_4_5,
                DailyFitDatabase.MIGRATION_5_6,
                DailyFitDatabase.MIGRATION_6_7
            )
            .build()
        try {
            assertEquals("yes", migrated.appMetadataDao().getValue("kept"))
            assertEquals("阅读", migrated.habitDao().getById("habit-1")!!.name)
            assertTrue(migrated.temporaryTaskDao().observeAll().first().isEmpty())
        } finally {
            migrated.close()
            context.deleteDatabase(databaseName)
        }
    }
}

private class MutableTaskClock(
    var currentInstant: Instant,
    private val currentZone: ZoneId
) : Clock() {
    override fun getZone(): ZoneId = currentZone
    override fun withZone(zone: ZoneId): Clock = MutableTaskClock(currentInstant, zone)
    override fun instant(): Instant = currentInstant
}
