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
import com.czaplicki.eproba.db.Exam
import com.czaplicki.eproba.db.FCMDevice
import com.czaplicki.eproba.db.User
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Instant
import kotlin.properties.Delegates

class EprobaApiHelper {
    val app: EprobaApplication = EprobaApplication.instance
    val api: EprobaApi = app.api
    val service: EprobaService = app.service
    private val sharedPreferences = app.sharedPreferences
    val gson = Gson()
    private val examDao = app.database.examDao()
    private val userDao = app.database.userDao()
    var user: User? = gson.fromJson(sharedPreferences.getString("user", null), User::class.java)
    var lastExamsUpdate by Delegates.observable(
        sharedPreferences.getLong(
            "lastExamsUpdate",
            0L
        )
    ) { _, _, newValue ->
        sharedPreferences.edit().putLong("lastExamsUpdate", newValue).apply()
    }
    var lastUserExamsUpdate by Delegates.observable(
        sharedPreferences.getLong(
            "lastUserExamsUpdate",
            0L
        )
    ) { _, _, newValue ->
        sharedPreferences.edit().putLong("lastUserExamsUpdate", newValue).apply()
    }

    data class ExamUpdate(
        val addedExams: MutableList<Exam> = mutableListOf(),
        val updatedExams: MutableList<Exam> = mutableListOf(),
        val deletedExams: MutableList<Exam> = mutableListOf()
    )

    suspend fun getAndProcessAppConfig(): APIState {
        try {
            val appConfig = service.getAppConfig()

            sharedPreferences.edit().putBoolean("ads", appConfig.ads).apply()

            if (appConfig.theEnd) {
                if (appConfig.endMessages.isNotEmpty()) {
                    sharedPreferences.edit()
                        .putString("endMessages", gson.toJson(appConfig.endMessages)).apply()
                } else {
                    sharedPreferences.edit().putString("endMessages", null).apply()
                }
                sharedPreferences.edit().putBoolean("theEnd", true).apply()
                return APIState.END_OF_LIFE
            } else {
                sharedPreferences.edit().putBoolean("theEnd", false).apply()
                sharedPreferences.edit().putString("endMessages", null).apply()
            }

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

    suspend fun getExams(userOnly: Boolean = false, ignoreLastSync: Boolean = false): ExamUpdate {
        var exams: MutableList<Exam> = examDao.getAllNow() as MutableList<Exam>
        val examsUpdate = ExamUpdate()
        try {
            if ((!userOnly && lastExamsUpdate + 2592000L < Instant.now().epochSecond) || (userOnly && lastUserExamsUpdate + 2592000L < Instant.now().epochSecond) || ignoreLastSync) {
                examsUpdate.deletedExams.addAll(exams)
                exams = if (userOnly) {
                    service.getUserExams()
                } else {
                    service.getExams()
                } as MutableList<Exam>
                examDao.nukeTable()
                examDao.insert(*exams.filter { !it.isDeleted }.toTypedArray())
                examsUpdate.addedExams.addAll(exams.filter { !it.isDeleted })
            } else {
                for (exam in if (userOnly) {
                    service.getUserExams(lastUserExamsUpdate)
                } else {
                    service.getExams(lastExamsUpdate)
                }) {
                    if (exam.isDeleted) {
                        examDao.delete(exam)
                        examsUpdate.deletedExams.add(exam)
                    } else {
                        if (examDao.getNow(exam.id) == null) {
                            examDao.insert(exam)
                            examsUpdate.addedExams.add(exam)
                        } else {
                            examDao.update(exam)
                            examsUpdate.updatedExams.add(exam)
                        }
                    }
                }
            }
            if (userOnly) {
                lastUserExamsUpdate = Instant.now().epochSecond
            } else {
                lastExamsUpdate = Instant.now().epochSecond
                lastUserExamsUpdate = Instant.now().epochSecond
            }
        } catch (e: Exception) {
            Log.e("EprobaApiHelper", "getExams: ", e)
            if (e is java.lang.IllegalStateException && e.message?.startsWith("Expected BEGIN_ARRAY but was BEGIN_OBJECT") == true) {
                examDao.nukeTable() // Clear the table if the API returns an object instead of an array, as it means the API is down and the data may be corrupted, so it's better to start fresh. Exams will be re-downloaded on the next sync.
            }
        }
        getMissingUsers(examDao.getAllNow())
        return examsUpdate
    }

    private suspend fun getMissingUsers(exams: List<Exam>) {
        val examsUserIds: MutableSet<Long> = mutableSetOf()
        val users = userDao.getAll()
        exams.forEach {
            if (it.userId != null) examsUserIds.add(it.userId!!)
            if (it.supervisor != null) examsUserIds.add(it.supervisor!!)
            if (it.tasks.isNotEmpty()) it.tasks.forEach { task ->
                if (task.approver != null) examsUserIds.add(task.approver!!)
            }
        }
        examsUserIds.filter { id -> users.find { it.id == id } == null }.forEach { id ->
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
            if (user != null && newUser.scout.function != user?.scout?.function) {
                handleUserFunctionChange(user!!.scout.function, newUser.scout.function)
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
            if (newUser != null && user != null && (
                        newUser.firstName != user?.firstName ||
                                newUser.lastName != user?.lastName ||
                                newUser.nickname != user?.nickname ||
                                newUser.scout.patrolId != user?.scout?.patrolId ||
                                newUser.scout.function != user?.scout?.function ||
                                newUser.scout.rank != user?.scout?.rank ||
                                newUser.scout.patrolName != user?.scout?.patrolName ||
                                newUser.scout.teamId != user?.scout?.teamId ||
                                newUser.scout.teamName != user?.scout?.teamName)
            ) {
                if (newUser.scout.function != user?.scout?.function) {
                    handleUserFunctionChange(user!!.scout.function, newUser.scout.function)
                }
                user = User(
                    newUser.id,
                    newUser.firstName,
                    newUser.lastName,
                    newUser.nickname,
                    user!!.email,
                    newUser.scout
                )
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
                        activity.navController.navigate(R.id.navigation_your_exams)
                        activity.bottomNavigation.visibility = View.GONE
                    }
                }
            }
        }
    }

    suspend fun getExamsWithTasksToApprove(): List<Exam> {
        return try {
            val exams = service.getTasksTBC()
            getMissingUsers(exams)
            exams
        } catch (e: Exception) {
            Log.e("EprobaApiHelper", "getTasksToApprove: ", e)
            listOf()
        }
    }

    suspend fun registerFCMToken(token: String) {
        val device = FCMDevice(
            name = Build.MODEL,
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