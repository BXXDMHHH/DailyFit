package com.dailyfit.app

import android.app.Application
import androidx.room.Room
import com.dailyfit.app.data.local.DailyFitDatabase
import com.dailyfit.app.data.repository.HabitRepository
import com.dailyfit.app.data.repository.TemporaryTaskRepository

class DailyFitApplication : Application() {
    val database: DailyFitDatabase by lazy {
        Room.databaseBuilder(applicationContext, DailyFitDatabase::class.java, DailyFitDatabase.DATABASE_NAME)
            .addMigrations(DailyFitDatabase.MIGRATION_1_2, DailyFitDatabase.MIGRATION_2_3)
            .build()
    }

    val habitRepository: HabitRepository by lazy { HabitRepository(database) }
    val temporaryTaskRepository: TemporaryTaskRepository by lazy { TemporaryTaskRepository(database) }
}
