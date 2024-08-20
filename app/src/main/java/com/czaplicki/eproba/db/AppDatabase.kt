package com.czaplicki.eproba.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [User::class, Worksheet::class], version = 10)
@TypeConverters(TaskConverter::class, ZonedDateTimeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun worksheetDao(): WorksheetDao
}