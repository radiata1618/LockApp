package com.app.lockapp.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.app.lockapp.data.LockTime

@Database(entities = [LockTime::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmPatternDao(): LockTime
}