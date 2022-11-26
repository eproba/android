package com.czaplicki.eproba

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.room.Room
import com.czaplicki.eproba.api.EprobaApi
import com.czaplicki.eproba.api.EprobaService
import com.czaplicki.eproba.db.AppDatabase
import com.google.android.material.color.DynamicColors
import net.openid.appauth.AuthorizationService

class EprobaApplication : Application() {

    private val activeActivityCallbacks = ActiveActivityLifecycleCallbacks()

    val authStateManager: AuthStateManager by lazy {
        AuthStateManager.getInstance(this)
    }

    val authService: AuthorizationService by lazy {
        AuthorizationService(this)
    }

    val api: EprobaApi by lazy {
        EprobaApi()
    }

    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "eproba.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        registerActivityLifecycleCallbacks(activeActivityCallbacks)
        // Apply dynamic color
        DynamicColors.applyToActivitiesIfAvailable(this)
    }

    val service: EprobaService by lazy {
        api.create(this)!!.create(EprobaService::class.java)
    }

    override fun onTerminate() {
        unregisterActivityLifecycleCallbacks(activeActivityCallbacks)
        super.onTerminate()
    }

    fun getActiveActivity(): Activity? = activeActivityCallbacks.getActiveActivity()

    companion object {
        lateinit var instance: EprobaApplication
            private set
    }
}

class ActiveActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {

    private var activeActivity: Activity? = null

    fun getActiveActivity(): Activity? = activeActivity

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        activeActivity = activity
    }

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {
        activeActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        if (activity === activeActivity) {
            activeActivity = null
        }
    }
}