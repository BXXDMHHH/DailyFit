package com.dailyfit.app.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(tableName = "app_metadata")
data class AppMetadataEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Dao
interface AppMetadataDao {
    @Query("SELECT value FROM app_metadata WHERE `key` = :key LIMIT 1")
    suspend fun getValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: AppMetadataEntity)
}

@Database(
    entities = [
        AppMetadataEntity::class,
        HabitEntity::class,
        HabitRecordEntity::class,
        TemporaryTaskEntity::class,
        WeeklyWorkoutPlanVersionEntity::class,
        WeeklyWorkoutPlanDayEntity::class,
        DailyWorkoutPlanEntity::class,
        ExerciseEntity::class,
        WorkoutPlanExerciseEntity::class,
        WorkoutSessionEntity::class,
        WorkoutSessionExerciseEntity::class,
        WorkoutSessionSetEntity::class,
        WorkoutHistoryEntity::class,
        WorkoutHistoryExerciseEntity::class,
        WorkoutHistorySetEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class DailyFitDatabase : RoomDatabase() {
    abstract fun appMetadataDao(): AppMetadataDao
    abstract fun habitDao(): HabitDao
    abstract fun habitRecordDao(): HabitRecordDao
    abstract fun temporaryTaskDao(): TemporaryTaskDao
    abstract fun weeklyWorkoutPlanDao(): WeeklyWorkoutPlanDao
    abstract fun dailyWorkoutPlanDao(): DailyWorkoutPlanDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun workoutPlanExerciseDao(): WorkoutPlanExerciseDao

    companion object {
        const val DATABASE_NAME = "dailyfit.db"

        /** Fresh installs need the same database-level immutability as migrated installs. */
        val CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                listOf("workout_history", "workout_history_exercises", "workout_history_sets").forEach { table ->
                    db.execSQL("CREATE TRIGGER IF NOT EXISTS immutable_${table}_update BEFORE UPDATE ON ${table} BEGIN SELECT RAISE(ABORT, 'Workout history is immutable'); END")
                    db.execSQL("CREATE TRIGGER IF NOT EXISTS immutable_${table}_delete BEFORE DELETE ON ${table} BEGIN SELECT RAISE(ABORT, 'Workout history is immutable'); END")
                }
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS habits (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        frequency TEXT NOT NULL,
                        customDays TEXT NOT NULL,
                        isActive INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS habit_records (
                        id TEXT NOT NULL PRIMARY KEY,
                        habitId TEXT NOT NULL,
                        businessDate TEXT NOT NULL,
                        habitNameSnapshot TEXT NOT NULL,
                        isCompleted INTEGER NOT NULL,
                        completedAt INTEGER,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(habitId) REFERENCES habits(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_habit_records_habitId_businessDate ON habit_records(habitId, businessDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_habit_records_businessDate ON habit_records(businessDate)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS temporary_tasks (
                        id TEXT NOT NULL PRIMARY KEY,
                        businessDate TEXT NOT NULL,
                        title TEXT NOT NULL,
                        isCompleted INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        completedAt INTEGER
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_temporary_tasks_businessDate ON temporary_tasks(businessDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_temporary_tasks_businessDate_isCompleted ON temporary_tasks(businessDate, isCompleted)")
            }
        }


        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS workout_sessions (id TEXT NOT NULL PRIMARY KEY, planId TEXT, businessDate TEXT NOT NULL, title TEXT NOT NULL, status TEXT NOT NULL, startedAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sessions_businessDate ON workout_sessions(businessDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_sessions_status ON workout_sessions(status)")
                db.execSQL("CREATE TABLE IF NOT EXISTS workout_session_exercises (id TEXT NOT NULL PRIMARY KEY, sessionId TEXT NOT NULL, exerciseId TEXT, exerciseNameSnapshot TEXT NOT NULL, categorySnapshot TEXT NOT NULL, equipmentSnapshot TEXT NOT NULL, notesSnapshot TEXT NOT NULL, sortOrder INTEGER NOT NULL, targetSets INTEGER NOT NULL, targetReps TEXT NOT NULL, targetWeight REAL NOT NULL, isTemporary INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_session_exercises_sessionId ON workout_session_exercises(sessionId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_session_exercises_exerciseId ON workout_session_exercises(exerciseId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS workout_session_sets (id TEXT NOT NULL PRIMARY KEY, sessionExerciseId TEXT NOT NULL, setNumber INTEGER NOT NULL, targetReps INTEGER NOT NULL, targetWeight REAL NOT NULL, actualReps INTEGER, actualWeight REAL, isCompleted INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_session_sets_sessionExerciseId ON workout_session_sets(sessionExerciseId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS workout_history (id TEXT NOT NULL PRIMARY KEY, sessionId TEXT NOT NULL, planId TEXT, businessDate TEXT NOT NULL, titleSnapshot TEXT NOT NULL, status TEXT NOT NULL, startedAt INTEGER NOT NULL, completedAt INTEGER NOT NULL, durationSeconds INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_history_businessDate ON workout_history(businessDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_history_completedAt ON workout_history(completedAt)")
                db.execSQL("CREATE TABLE IF NOT EXISTS workout_history_exercises (id TEXT NOT NULL PRIMARY KEY, historyId TEXT NOT NULL, sourceExerciseId TEXT, exerciseNameSnapshot TEXT NOT NULL, categorySnapshot TEXT NOT NULL, equipmentSnapshot TEXT NOT NULL, notesSnapshot TEXT NOT NULL, sortOrder INTEGER NOT NULL, targetSets INTEGER NOT NULL, targetReps TEXT NOT NULL, targetWeight REAL NOT NULL, isTemporary INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_history_exercises_historyId ON workout_history_exercises(historyId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS workout_history_sets (id TEXT NOT NULL PRIMARY KEY, historyExerciseId TEXT NOT NULL, setNumber INTEGER NOT NULL, targetReps INTEGER NOT NULL, targetWeight REAL NOT NULL, actualReps INTEGER, actualWeight REAL, isCompleted INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_history_sets_historyExerciseId ON workout_history_sets(historyExerciseId)")
                listOf("workout_history","workout_history_exercises","workout_history_sets").forEach { table ->
                    db.execSQL("CREATE TRIGGER IF NOT EXISTS immutable_${table}_update BEFORE UPDATE ON ${table} BEGIN SELECT RAISE(ABORT, 'Workout history is immutable'); END")
                    db.execSQL("CREATE TRIGGER IF NOT EXISTS immutable_${table}_delete BEFORE DELETE ON ${table} BEGIN SELECT RAISE(ABORT, 'Workout history is immutable'); END")
                }
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS workout_plan_exercises (
                        id TEXT NOT NULL PRIMARY KEY,
                        ownerType TEXT NOT NULL,
                        ownerId TEXT NOT NULL,
                        exerciseId TEXT,
                        exerciseNameSnapshot TEXT NOT NULL,
                        categorySnapshot TEXT NOT NULL,
                        equipmentSnapshot TEXT NOT NULL,
                        notesSnapshot TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        targetSets INTEGER NOT NULL,
                        targetReps TEXT NOT NULL,
                        targetWeight REAL NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_plan_exercises_ownerType_ownerId ON workout_plan_exercises(ownerType, ownerId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_plan_exercises_exerciseId ON workout_plan_exercises(exerciseId)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS weekly_workout_plan_versions (
                        id TEXT NOT NULL PRIMARY KEY,
                        effectiveFromDate TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_weekly_workout_plan_versions_effectiveFromDate_createdAt ON weekly_workout_plan_versions(effectiveFromDate, createdAt)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS weekly_workout_plan_days (
                        id TEXT NOT NULL PRIMARY KEY,
                        versionId TEXT NOT NULL,
                        dayOfWeek INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        isRestDay INTEGER NOT NULL,
                        FOREIGN KEY(versionId) REFERENCES weekly_workout_plan_versions(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_weekly_workout_plan_days_versionId_dayOfWeek ON weekly_workout_plan_days(versionId, dayOfWeek)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_weekly_workout_plan_days_versionId ON weekly_workout_plan_days(versionId)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS daily_workout_plans (
                        id TEXT NOT NULL PRIMARY KEY,
                        businessDate TEXT NOT NULL,
                        title TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        status TEXT NOT NULL,
                        isPrimary INTEGER NOT NULL,
                        sourceVersionId TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_daily_workout_plans_businessDate ON daily_workout_plans(businessDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_daily_workout_plans_businessDate_isPrimary ON daily_workout_plans(businessDate, isPrimary)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS exercises (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        category TEXT NOT NULL,
                        equipment TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_exercises_name ON exercises(name)")
            }
        }
    }
}
