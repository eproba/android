package com.czaplicki.eproba.api

import android.os.Build
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.czaplicki.eproba.BuildConfig
import com.czaplicki.eproba.DemotedScreen
import com.czaplicki.eproba.EprobaApplication
import com.czaplicki.eproba.MainActivity
import com.czaplicki.eproba.PromotedScreen
import com.czaplicki.eproba.R
import com.czaplicki.eproba.db.Worksheet
import com.czaplicki.eproba.db.FCMDevice
import com.czaplicki.eproba.db.User
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Instant
import java.util.UUID
import kotlin.properties.Delegates

class EprobaApiHelper {
    val app: EprobaApplication = EprobaApplication.instance
    val api: EprobaApi = app.api
    val service: EprobaService = app.service
    private val sharedPreferences = app.sharedPreferences
    val gson = Gson()
    private val worksheetDao = app.database.worksheetDao()
    private val userDao = app.database.userDao()
    var user: User? = gson.fromJson(sharedPreferences.getString("user", null), User::class.java)
    var lastWorksheetsUpdate by Delegates.observable(
        sharedPreferences.getLong(
            "lastWorksheetsUpdate",
            0L
        )
    ) { _, _, newValue ->
        sharedPreferences.edit().putLong("lastWorksheetsUpdate", newValue).apply()
    }
    var lastUserWorksheetsUpdate by Delegates.observable(
        sharedPreferences.getLong(
            "lastUserWorksheetsUpdate",
            0L
        )
    ) { _, _, newValue ->
        sharedPreferences.edit().putLong("lastUserWorksheetsUpdate", newValue).apply()
    }

    data class WorksheetUpdate(
        val addedWorksheets: MutableList<Worksheet> = mutableListOf(),
        val updatedWorksheets: MutableList<Worksheet> = mutableListOf(),
        val deletedWorksheets: MutableList<Worksheet> = mutableListOf()
    )

    suspend fun getAndProcessAppConfig(): APIState {
        try {
            val appConfig = service.getAppConfig()

            sharedPreferences.edit().putBoolean("ads", appConfig.ads).apply()


            if (appConfig.maintenance) {
                return APIState.MAINTENANCE
            }

            if (BuildConfig.VERSION_CODE < appConfig.minVersion) {
                return APIState.UPDATE_REQUIRED
            }

            return APIState.OK

        } catch (e: Exception) {
            return APIState.ERROR
        }
    }

    suspend fun getWorksheets(userOnly: Boolean = false, ignoreLastSync: Boolean = false): WorksheetUpdate {
        var worksheets: MutableList<Worksheet> = worksheetDao.getAllNow() as MutableList<Worksheet>
        val worksheetsUpdate = WorksheetUpdate()
        try {
            if ((!userOnly && lastWorksheetsUpdate + 2592000L < Instant.now().epochSecond) || (userOnly && lastUserWorksheetsUpdate + 2592000L < Instant.now().epochSecond) || ignoreLastSync) {
                worksheetsUpdate.deletedWorksheets.addAll(worksheets)
                worksheets = if (userOnly) {
                    service.getUserWorksheets()
                } else {
                    service.getWorksheets()
                } as MutableList<Worksheet>
                worksheetDao.nukeTable()
                worksheetDao.insert(*worksheets.filter { !it.isDeleted }.toTypedArray())
                worksheetsUpdate.addedWorksheets.addAll(worksheets.filter { !it.isDeleted })
            } else {
                for (worksheet in if (userOnly) {
                    service.getUserWorksheets(lastUserWorksheetsUpdate)
                } else {
                    service.getWorksheets(lastWorksheetsUpdate)
                }) {
                    if (worksheet.isDeleted) {
                        worksheetDao.delete(worksheet)
                        worksheetsUpdate.deletedWorksheets.add(worksheet)
                    } else {
                        if (worksheetDao.getNow(worksheet.id) == null) {
                            worksheetDao.insert(worksheet)
                            worksheetsUpdate.addedWorksheets.add(worksheet)
                        } else {
                            worksheetDao.update(worksheet)
                            worksheetsUpdate.updatedWorksheets.add(worksheet)
                        }
                    }
                }
            }
            if (userOnly) {
                lastUserWorksheetsUpdate = Instant.now().epochSecond
            } else {
                lastWorksheetsUpdate = Instant.now().epochSecond
                lastUserWorksheetsUpdate = Instant.now().epochSecond
            }
        } catch (e: Exception) {
            Log.e("EprobaApiHelper", "getWorksheets: ", e)
            if (e is java.lang.IllegalStateException && e.message?.startsWith("Expected BEGIN_ARRAY but was BEGIN_OBJECT") == true) {
                worksheetDao.nukeTable() // Clear the table if the API returns an object instead of an array, as it means the API is down and the data may be corrupted, so it's better to start fresh. Worksheets will be re-downloaded on the next sync.
            }
        }
        getMissingUsers(worksheetDao.getAllNow())
        return worksheetsUpdate
    }

    private suspend fun getMissingUsers(worksheets: List<Worksheet>) {
        val worksheetsUserIds: MutableSet<UUID> = mutableSetOf()
        val users = userDao.getAll()
        worksheets.forEach {
            if (it.userId != null) worksheetsUserIds.add(it.userId!!)
            if (it.supervisor != null) worksheetsUserIds.add(it.supervisor!!)
            if (it.tasks.isNotEmpty()) it.tasks.forEach { task ->
                if (task.approver != null) worksheetsUserIds.add(task.approver!!)
            }
        }
        worksheetsUserIds.filter { id -> users.find { it.id == id } == null }.forEach { id ->
            try {
                userDao.insert(service.getUser(id))
            } catch (e: Exception) {
                Log.e("EprobaApiHelper", "getMissingUsers: ", e)
            }
        }
    }

    suspend fun getUser(): User {
        try {
            val newUser = service.getUser()
            if (user != null && newUser.function != user?.function) {
                handleUserFunctionChange(user!!.function, newUser.function)
            }
            user = newUser
            sharedPreferences.edit().putString("user", gson.toJson(user)).apply()
        } catch (e: Exception) {
            Log.e("EprobaApiHelper", "getUser: ", e)
        }
        return user as User
    }

    suspend fun getUsers(): List<User> {
        try {
            val users = service.getUsers()
            userDao.nukeTable()
            userDao.insert(*users.toTypedArray())
            val newUser = users.find { it.id == user?.id }
            if (newUser != null && user != null && (gson.toJson(newUser) != gson.toJson(user))
            ) {
                if (newUser.function != user?.function) {
                    handleUserFunctionChange(user!!.function, newUser.function)
                }
                user = newUser
                sharedPreferences.edit().putString("user", gson.toJson(user)).apply()
            }
            return users
        } catch (e: Exception) {
            Log.e("EprobaApiHelper", "getUsersCall: ", e)
            return userDao.getAll()
        }
    }

    private fun handleUserFunctionChange(oldFunction: Int, newFunction: Int) {
        val activity = app.currentActivity
        if (activity is AppCompatActivity) {
            if (oldFunction < newFunction && oldFunction < 2 && newFunction >= 2) {
                PromotedScreen().show(
                    activity.supportFragmentManager,
                    "promoted"
                )
                if (activity is MainActivity) {
                    activity.bottomNavigation.visibility = View.VISIBLE
                }
            } else if (oldFunction > newFunction && oldFunction >= 2 && newFunction < 2) {
                DemotedScreen().show(
                    activity.supportFragmentManager,
                    "demoted"
                )
                when (activity) {
                    is MainActivity -> {
                        activity.navController.navigate(R.id.navigation_your_worksheets)
                        activity.bottomNavigation.visibility = View.GONE
                    }
                }
            }
        }
    }

    suspend fun getWorksheetsWithTasksToApprove(): List<Worksheet> {
        return try {
            val worksheets = service.getTasksTBC()
            getMissingUsers(worksheets)
            worksheets
        } catch (e: Exception) {
            Log.e("EprobaApiHelper", "getTasksToApprove: ", e)
            listOf()
        }
    }

    suspend fun registerFCMToken(token: String) {
        val deviceName = gson.toJson(
            mapOf(
                "os" to mapOf(
                    "name" to "Android",
                    "version" to Build.VERSION.RELEASE
                ),
                "device" to mapOf(
                    "type" to "mobile",
                    "vendor" to Build.MANUFACTURER,
                    "model" to Build.MODEL
                ),
                "browser" to mapOf(
                    "name" to "app",
                    "version" to BuildConfig.VERSION_NAME,
                    "major" to BuildConfig.VERSION_CODE,
                    "type" to "release"
                )
            )
        )
        val device = FCMDevice(
            name = deviceName,
            token = token,
        )
        try {
            service.registerFCMDevice(
                device.toJSONString().toRequestBody("application/json".toMediaType())
            )
        } catch (e: Exception) {
            Log.e("EprobaApiHelper", "registerFCMToken: ", e)
        }
    }


}