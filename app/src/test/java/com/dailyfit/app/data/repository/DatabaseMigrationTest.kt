package com.dailyfit.app.data.repository

import androidx.room.Room
import com.dailyfit.app.data.local.DailyFitDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DatabaseMigrationTest {
    @Test
    fun migrationFromVersionFourToSevenPreservesExistingDataAndInstallsHistoryGuards() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val name = "dailyfit-v4-to-v6-migration-test.db"
        context.deleteDatabase(name)

        val old = context.openOrCreateDatabase(name, 0, null)
        val schema = listOf(
            "CREATE TABLE app_metadata (`key` TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL)",
            "CREATE TABLE habits (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, frequency TEXT NOT NULL, customDays TEXT NOT NULL, isActive INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)",
            "CREATE TABLE habit_records (id TEXT NOT NULL PRIMARY KEY, habitId TEXT NOT NULL, businessDate TEXT NOT NULL, habitNameSnapshot TEXT NOT NULL, isCompleted INTEGER NOT NULL, completedAt INTEGER, createdAt INTEGER NOT NULL, FOREIGN KEY(habitId) REFERENCES habits(id) ON UPDATE NO ACTION ON DELETE RESTRICT)",
            "CREATE UNIQUE INDEX index_habit_records_habitId_businessDate ON habit_records(habitId, businessDate)",
            "CREATE INDEX index_habit_records_businessDate ON habit_records(businessDate)",
            "CREATE TABLE temporary_tasks (id TEXT NOT NULL PRIMARY KEY, businessDate TEXT NOT NULL, title TEXT NOT NULL, isCompleted INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, completedAt INTEGER)",
            "CREATE INDEX index_temporary_tasks_businessDate ON temporary_tasks(businessDate)",
            "CREATE INDEX index_temporary_tasks_businessDate_isCompleted ON temporary_tasks(businessDate, isCompleted)",
            "CREATE TABLE weekly_workout_plan_versions (id TEXT NOT NULL PRIMARY KEY, effectiveFromDate TEXT NOT NULL, createdAt INTEGER NOT NULL)",
            "CREATE INDEX index_weekly_workout_plan_versions_effectiveFromDate_createdAt ON weekly_workout_plan_versions(effectiveFromDate, createdAt)",
            "CREATE TABLE weekly_workout_plan_days (id TEXT NOT NULL PRIMARY KEY, versionId TEXT NOT NULL, dayOfWeek INTEGER NOT NULL, title TEXT NOT NULL, notes TEXT NOT NULL, isRestDay INTEGER NOT NULL, FOREIGN KEY(versionId) REFERENCES weekly_workout_plan_versions(id) ON UPDATE NO ACTION ON DELETE CASCADE)",
            "CREATE UNIQUE INDEX index_weekly_workout_plan_days_versionId_dayOfWeek ON weekly_workout_plan_days(versionId, dayOfWeek)",
            "CREATE INDEX index_weekly_workout_plan_days_versionId ON weekly_workout_plan_days(versionId)",
            "CREATE TABLE daily_workout_plans (id TEXT NOT NULL PRIMARY KEY, businessDate TEXT NOT NULL, title TEXT NOT NULL, notes TEXT NOT NULL, status TEXT NOT NULL, isPrimary INTEGER NOT NULL, sourceVersionId TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)",
            "CREATE INDEX index_daily_workout_plans_businessDate ON daily_workout_plans(businessDate)",
            "CREATE INDEX index_daily_workout_plans_businessDate_isPrimary ON daily_workout_plans(businessDate, isPrimary)",
            "CREATE TABLE exercises (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, category TEXT NOT NULL, equipment TEXT NOT NULL, notes TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)",
            "CREATE INDEX index_exercises_name ON exercises(name)"
        )
        schema.forEach(old::execSQL)
        old.execSQL("INSERT INTO app_metadata (`key`, value) VALUES ('migration-check', 'preserved')")
        old.execSQL("INSERT INTO habits VALUES ('habit-1', '每日阅读', 'DAILY', '', 1, 1, 1)")
        old.execSQL("INSERT INTO habit_records VALUES ('record-1', 'habit-1', '2026-11-21', '每日阅读', 1, 2, 1)")
        old.execSQL("INSERT INTO temporary_tasks VALUES ('task-1', '2026-11-21', '旧任务', 1, 1, 2, 2)")
        old.execSQL("INSERT INTO weekly_workout_plan_versions VALUES ('version-1', '2026-11-20', 1)")
        old.execSQL("INSERT INTO weekly_workout_plan_days VALUES ('day-1', 'version-1', 5, '上肢训练', '', 0)")
        old.execSQL("INSERT INTO daily_workout_plans VALUES ('plan-1', '2026-11-21', '上肢训练', '', 'COMPLETED', 1, 'version-1', 1, 2)")
        old.execSQL("INSERT INTO exercises VALUES ('exercise-1', '卧推', '胸部', '杠铃', '', 1, 2)")
        old.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
        old.execSQL("INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, 'bc0e63f12ed8b439483de76f6c9bd375')")
        old.version = 4
        old.close()

        val migrated = Room.databaseBuilder(context, DailyFitDatabase::class.java, name)
            .addMigrations(DailyFitDatabase.MIGRATION_4_5, DailyFitDatabase.MIGRATION_5_6, DailyFitDatabase.MIGRATION_6_7)
            .build()
        try {
            assertEquals("preserved", migrated.appMetadataDao().getValue("migration-check"))
            assertEquals("每日阅读", migrated.habitDao().getById("habit-1")?.name)
            assertEquals("2026-11-21", migrated.habitRecordDao().getById("record-1")?.businessDate)
            assertEquals("旧任务", migrated.temporaryTaskDao().observeAll().first().single().title)
            assertEquals("上肢训练", migrated.weeklyWorkoutPlanDao().getDays("version-1").single().title)
            assertEquals("上肢训练", migrated.dailyWorkoutPlanDao().getById("plan-1")?.title)
            assertEquals("卧推", migrated.exerciseDao().getById("exercise-1")?.name)
            assertTrue(migrated.workoutPlanExerciseDao().getForOwner("DAILY_PLAN", "plan-1").isEmpty())
            assertTrue(migrated.workoutTemplateDao().observeAll().first().isEmpty())

            val triggers = migrated.openHelper.readableDatabase.query(
                "SELECT COUNT(*) FROM sqlite_master WHERE type='trigger' AND name LIKE 'immutable_workout_history%'"
            )
            triggers.use {
                assertTrue(it.moveToFirst())
                assertEquals(6, it.getInt(0))
            }
        } finally {
            migrated.close()
            context.deleteDatabase(name)
        }
    }
}
