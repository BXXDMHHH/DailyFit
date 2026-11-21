package com.dailyfit.app

import android.app.Application
import androidx.room.Room
import com.dailyfit.app.data.local.DailyFitDatabase
import com.dailyfit.app.data.repository.HabitRepository
import com.dailyfit.app.data.repository.TemporaryTaskRepository
import com.dailyfit.app.data.repository.TrainingPlanRepository

class DailyFitApplication : Application() {
    val database: DailyFitDatabase by lazy {
        Room.databaseBuilder(applicationContext, DailyFitDatabase::class.java, DailyFitDatabase.DATABASE_NAME)
            .addMigrations(
                DailyFitDatabase.MIGRATION_1_2,
                DailyFitDatabase.MIGRATION_2_3,
                DailyFitDatabase.MIGRATION_3_4
            )
            .build()
    }

    val habitRepository: HabitRepository by lazy { HabitRepository(database) }
    val temporaryTaskRepository: TemporaryTaskRepository by lazy { TemporaryTaskRepository(database) }
    val trainingPlanRepository: TrainingPlanRepository by lazy { TrainingPlanRepository(database) }
}
