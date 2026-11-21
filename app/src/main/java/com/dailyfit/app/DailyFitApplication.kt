package com.dailyfit.app

import android.app.Application
import androidx.room.Room
import com.dailyfit.app.data.local.DailyFitDatabase
import com.dailyfit.app.data.repository.HabitRepository

class DailyFitApplication : Application() {
    val database: DailyFitDatabase by lazy {
        Room.databaseBuilder(applicationContext, DailyFitDatabase::class.java, DailyFitDatabase.DATABASE_NAME)
            .addMigrations(DailyFitDatabase.MIGRATION_1_2)
            .build()
    }

    val habitRepository: HabitRepository by lazy { HabitRepository(database) }
}
