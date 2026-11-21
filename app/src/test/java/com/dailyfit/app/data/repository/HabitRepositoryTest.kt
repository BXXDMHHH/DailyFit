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
class HabitRepositoryTest {
    private lateinit var database: DailyFitDatabase
    private lateinit var repository: HabitRepository
    private val fixedClock = Clock.fixed(Instant.parse("2026-11-23T08:00:00Z"), ZoneId.of("Asia/Tokyo"))
    private val today = LocalDate.now(fixedClock)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            DailyFitDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = HabitRepository(database, fixedClock)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun createsOnlyScheduledTodayRecordsAndIsIdempotent() = runBlocking {
        repository.createHabit("阅读", HabitFrequency.DAILY, emptySet())
        repository.createHabit("工作日运动", HabitFrequency.WEEKDAYS, emptySet())
        repository.createHabit("周一训练", HabitFrequency.CUSTOM, setOf(java.time.DayOfWeek.MONDAY))

        repository.ensureTodayRecords()
        val records = repository.observeDashboard(today).first().todayHabits
        assertEquals(3, records.size)
        repository.ensureTodayRecords()
        assertEquals(3, repository.observeDashboard(today).first().todayHabits.size)
    }

    @Test
    fun customHabitOnAnotherWeekdayRemainsVisibleInHabitManagement() = runBlocking {
        repository.createHabit("周三拉伸", HabitFrequency.CUSTOM, setOf(java.time.DayOfWeek.WEDNESDAY))
        val dashboard = repository.observeDashboard(today).first()
        assertTrue(dashboard.todayHabits.isEmpty())
        assertEquals("周三拉伸", dashboard.activeHabits.single().name)
    }

    @Test
    fun editingHabitDoesNotRewriteExistingRecordSnapshot() = runBlocking {
        repository.createHabit("阅读", HabitFrequency.DAILY, emptySet())
        val before = repository.observeDashboard(today).first().todayHabits.single().record

        repository.updateHabit(
            before.habitId,
            "编程学习",
            HabitFrequency.WEEKDAYS,
            emptySet()
        )

        val after = repository.observeDashboard(today).first().todayHabits.single().record
        assertEquals("阅读", after.habitNameSnapshot)
        assertEquals(before.id, after.id)
    }

    @Test
    fun deactivationKeepsHistoricalRecordButStopsFutureGeneration() = runBlocking {
        repository.createHabit("阅读", HabitFrequency.DAILY, emptySet())
        val item = repository.observeDashboard(today).first().todayHabits.single()
        repository.deactivateHabit(item.habit.id)

        assertTrue(repository.observeHistory().first().any { it.id == item.record.id })
        assertTrue(repository.observeDashboard(today).first().todayHabits.isEmpty())
    }

    @Test
    fun yesterdayRecordCannotBeCheckedInAndIsNeverAutoBackfilled() = runBlocking {
        repository.createHabit("阅读", HabitFrequency.DAILY, emptySet())
        val yesterday = today.minusDays(1)
        val todayRecord = repository.observeDashboard(today).first().todayHabits.single().record

        assertTrue(repository.setTodayCompletion(todayRecord.id, true))
        val pastRecord = com.dailyfit.app.data.local.HabitRecordEntity(
            id = "past-record",
            habitId = todayRecord.habitId,
            businessDate = yesterday.toString(),
            habitNameSnapshot = "阅读",
            createdAt = fixedClock.millis()
        )
        database.habitRecordDao().insertIfMissing(pastRecord)
        assertFalse(repository.setTodayCompletion(pastRecord.id, true))
        assertFalse(database.habitRecordDao().getById(pastRecord.id)!!.isCompleted)
        // The synchronizer only generates the current local date; it never backfills yesterday.
        repository.ensureTodayRecords()
        assertEquals(1, repository.observeHistory().first().count { it.businessDate == today.toString() })
        assertEquals(1, repository.observeHistory().first().count { it.businessDate == yesterday.toString() })
    }

    @Test
    fun dateChangeCreatesNewUncheckedRecordAndPreservesPreviousDay() = runBlocking {
        val clock = MutableTestClock(
            Instant.parse("2026-11-23T08:00:00Z"),
            ZoneId.of("Asia/Tokyo")
        )
        val context = RuntimeEnvironment.getApplication()
        val testDatabase = Room.inMemoryDatabaseBuilder(context, DailyFitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val testRepository = HabitRepository(testDatabase, clock)
            testRepository.createHabit("每日阅读", HabitFrequency.DAILY, emptySet())
            val firstDate = LocalDate.now(clock)
            val firstRecord = testRepository.observeDashboard(firstDate).first().todayHabits.single().record
            assertTrue(testRepository.setTodayCompletion(firstRecord.id, true))

            clock.currentInstant = Instant.parse("2026-11-24T08:00:00Z")
            val nextDate = LocalDate.now(clock)
            testRepository.ensureTodayRecords()
            val nextRecord = testRepository.observeDashboard(nextDate).first().todayHabits.single().record

            assertEquals(firstDate.plusDays(1), nextDate)
            assertFalse(nextRecord.isCompleted)
            val history = testRepository.observeHistory().first()
            assertTrue(history.single { it.id == firstRecord.id }.isCompleted)
            assertEquals(2, history.size)
        } finally {
            testDatabase.close()
        }
    }

    @Test
    fun migrationFromVersionOnePreservesExistingMetadata() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val databaseName = "dailyfit-migration-test.db"
        context.deleteDatabase(databaseName)
        val oldDatabase = context.openOrCreateDatabase(databaseName, 0, null)
        oldDatabase.execSQL("CREATE TABLE app_metadata (`key` TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL)")
        oldDatabase.execSQL("INSERT INTO app_metadata (`key`, value) VALUES ('existing', 'preserved')")
        oldDatabase.version = 1
        oldDatabase.close()

        val migrated = Room.databaseBuilder(context, DailyFitDatabase::class.java, databaseName)
            .addMigrations(
                DailyFitDatabase.MIGRATION_1_2,
                DailyFitDatabase.MIGRATION_2_3,
                DailyFitDatabase.MIGRATION_3_4,
                DailyFitDatabase.MIGRATION_4_5,
                DailyFitDatabase.MIGRATION_5_6
            )
            .build()
        assertEquals("preserved", migrated.appMetadataDao().getValue("existing"))
        migrated.close()
        context.deleteDatabase(databaseName)
        Unit
    }

    @Test
    fun completionCanBeCheckedAndUncheckedForToday() = runBlocking {
        repository.createHabit("阅读", HabitFrequency.DAILY, emptySet())
        val record = repository.observeDashboard(today).first().todayHabits.single().record
        assertTrue(repository.setTodayCompletion(record.id, true))
        assertTrue(repository.observeDashboard(today).first().todayHabits.single().record.isCompleted)
        assertTrue(repository.setTodayCompletion(record.id, false))
        assertFalse(repository.observeDashboard(today).first().todayHabits.single().record.isCompleted)
    }

    @Test
    fun completionStatisticsUseOnlyHabitRecords() = runBlocking {
        repository.createHabit("阅读", HabitFrequency.DAILY, emptySet())
        repository.createHabit("拉伸", HabitFrequency.DAILY, emptySet())
        val records = repository.observeDashboard(today).first().todayHabits
        repository.setTodayCompletion(records.first().record.id, true)
        val dashboard = repository.observeDashboard(today).first()
        assertEquals(50, dashboard.todayCompletionPercent)
        assertEquals(50, dashboard.overallCompletionPercent)
    }
}


private class MutableTestClock(
    var currentInstant: Instant,
    private val currentZone: ZoneId
) : Clock() {
    override fun getZone(): ZoneId = currentZone
    override fun withZone(zone: ZoneId): Clock = MutableTestClock(currentInstant, zone)
    override fun instant(): Instant = currentInstant
}
