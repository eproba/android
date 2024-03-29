package com.czaplicki.eproba.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [User::class, Exam::class], version = 7)
@TypeConverters(ScoutConverter::class, TaskConverter::class, ZonedDateTimeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun examDao(): ExamDao
}