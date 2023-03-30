package com.czaplicki.eproba.api

import com.czaplicki.eproba.db.Exam
import com.czaplicki.eproba.db.FCMDevice
import com.czaplicki.eproba.db.Task
import com.czaplicki.eproba.db.User
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface EprobaService {

    @GET("user/")
    fun getUserCall(): Call<User>

    @GET("user/")
    suspend fun getUser(): User

    @GET("users/{id}/")
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
    suspend fun deleteExam(@Path("id") id: Long): Response<Void>

    @DELETE("exam/{id}/?archived")
    suspend fun deleteArchivedExam(@Path("id") id: Long): Response<Void>

    @PATCH("exam/{exam_id}/task/{task_id}/")
    fun updateTaskStatus(
        @Path("exam_id") examId: Long,
        @Path("task_id") taskId: Long,
        @Body status: RequestBody
    ): Call<Task>

    @POST("exam/{exam_id}/task/{task_id}/submit")
    suspend fun submitTask(
        @Path("exam_id") examId: Long,
        @Path("task_id") taskId: Long,
        @Body approver: RequestBody
    ): Task

    @POST("exam/{exam_id}/task/{task_id}/unsubmit")
    suspend fun unsubmitTask(
        @Path("exam_id") examId: Long,
        @Path("task_id") taskId: Long
    ): Task

    @PATCH("exam/{exam_id}/")
    suspend fun updateExam(
        @Path("exam_id") examId: Long,
        @Body body: RequestBody
    ): Exam

    @PATCH("exam/{exam_id}/?archived")
    suspend fun updateExamInArchive(
        @Path("exam_id") examId: Long,
        @Body body: RequestBody
    ): Exam

    @POST("exam/")
    suspend fun createExam(@Body body: RequestBody): Exam

    @POST("fcm/devices/")
    suspend fun registerFCMDevice(@Body body: RequestBody): FCMDevice
}