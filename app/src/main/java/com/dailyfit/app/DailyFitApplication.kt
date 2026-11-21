package com.dailyfit.app
import android.app.Application
import androidx.room.Room
import com.dailyfit.app.data.local.DailyFitDatabase
class DailyFitApplication : Application() {
    val database: DailyFitDatabase by lazy {
        Room.databaseBuilder(applicationContext, DailyFitDatabase::class.java, DailyFitDatabase.DATABASE_NAME).build()
    }
}
