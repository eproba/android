package com.czaplicki.eproba

import android.app.Application
import androidx.room.Room
import com.czaplicki.eproba.api.EprobaApi
import com.czaplicki.eproba.api.EprobaService
import com.czaplicki.eproba.db.AppDatabase
import com.google.android.material.color.DynamicColors
import net.openid.appauth.AuthorizationService

class EprobaApplication : Application() {
    private val authStateManager: AuthStateManager by lazy {
        AuthStateManager.getInstance(this)
    }

    private val authService: AuthorizationService by lazy {
        AuthorizationService(this)
    }

    private val api: EprobaApi by lazy {
        EprobaApi()
    }

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

    fun service(): EprobaService {
        var accessToken = authStateManager.current.accessToken
        authStateManager.current.performActionWithFreshTokens(authService) { token, _, _ ->
            authStateManager.updateSavedState()
            accessToken = token
        }
        return api.create(this, accessToken)!!.create(EprobaService::class.java)
    }

    fun refreshToken() {
        authStateManager.current.performActionWithFreshTokens(authService) { _, _, _ ->
            authStateManager.updateSavedState()
        }
    }
}