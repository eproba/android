package com.czaplicki.eproba.api

import com.czaplicki.eproba.db.Exam
import com.czaplicki.eproba.db.FCMDevice
import com.czaplicki.eproba.db.Task
import com.czaplicki.eproba.db.User
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface EprobaService {

    @GET("user/")
    fun getUserCall(): Call<User>

    @GET("user/")
    suspend fun getUser(): User

    @GET("user/{id}/")
    suspend fun getUser(@Path("id") id: Long): User

    @GET("users/")
    suspend fun getUsers(): List<User>

    @GET("exam/?user")
    suspend fun getUserExams(): List<Exam>

    @GET("exam/?user")
    suspend fun getUserExams(@Query("last_sync") lastSync: Long): List<Exam>

    @GET("exam/?templates")
    suspend fun getTemplates(): List<Exam>

    @GET("exam/?archived")
    suspend fun getArchivedExams(): List<Exam>

    @GET("exam/")
    suspend fun getExams(): List<Exam>

    @GET("exam/")
    suspend fun getExams(@Query("last_sync") lastSync: Long): List<Exam>

    @GET("exam/tasks/tbc/")
    suspend fun getTasksTBC(): List<Exam>

    @GET("exam/{id}/")
    suspend fun getExam(@Path("id") id: Long): Exam

    @DELETE("exam/{id}/")
    fun deleteExam(@Path("id") id: Long): Call<Void>

    @PATCH("exam/{exam_id}/task/{task_id}/")
    fun updateTaskStatus(
        @Path("exam_id") examId: Long,
        @Path("task_id") taskId: Long,
        @Body status: RequestBody
    ): Call<Task>

    @POST("exam/{exam_id}/task/{task_id}/submit")
    fun submitTask(
        @Path("exam_id") examId: Long,
        @Path("task_id") taskId: Long,
        @Body approver: RequestBody
    ): Call<Task>

    @POST("exam/{exam_id}/task/{task_id}/unsubmit")
    fun unsubmitTask(
        @Path("exam_id") examId: Long,
        @Path("task_id") taskId: Long
    ): Call<Task>

    @PATCH("exam/{exam_id}/")
    fun updateExam(
        @Path("exam_id") examId: Long,
        @Body body: RequestBody
    ): Call<Exam>

    @POST("exam/")
    fun createExam(@Body body: RequestBody): Call<Exam>

    @POST("fcm/devices/")
    suspend fun registerFCMDevice(@Body body: RequestBody): FCMDevice
}