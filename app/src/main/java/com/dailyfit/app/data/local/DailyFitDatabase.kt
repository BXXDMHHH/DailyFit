package com.dailyfit.app.data.local
import androidx.room.Database
import androidx.room.RoomDatabase
/** V1.0 database foundation. Feature entities and explicit migrations are added per stage. */
@Database(entities = [], version = 1, exportSchema = true)
abstract class DailyFitDatabase : RoomDatabase() {
    companion object { const val DATABASE_NAME = "dailyfit.db" }
}
