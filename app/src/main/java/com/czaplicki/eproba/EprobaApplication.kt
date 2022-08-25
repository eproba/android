package com.czaplicki.eproba

import android.app.Application
import androidx.room.Room
import com.google.android.material.color.DynamicColors

class EprobaApplication: Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "eproba.db")
            .fallbackToDestructiveMigration()
            .build()
    }
    override fun onCreate() {
        super.onCreate()
        // Apply dynamic color
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}