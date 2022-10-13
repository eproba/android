package com.czaplicki.eproba.api

import com.czaplicki.eproba.db.Exam
import com.czaplicki.eproba.db.Task
import com.czaplicki.eproba.db.User
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface EprobaService {

    @GET("user/")
    fun getUserInfo(): Call<User>

    @GET("user/{id}/")
    fun getUserInfo(@Path("id") id: Long): Call<User>

    @GET("users/")
    fun getUsersPublicInfo(): Call<List<User>>

    @GET("exam/?user")
    fun getUserExams(): Call<List<Exam>>

    @GET("exam/?templates")
    fun getTemplates(): Call<List<Exam>>

    @GET("exam/")
    suspend fun getExamsList(): List<Exam>

    @GET("exam/")
    fun getExams(): Call<List<Exam>>

    @GET("exam/tasks/tbc/")
    fun getTasksTBC(): Call<List<Exam>>

    @GET("exam/{id}/")
    fun getExam(@Path("id") id: Long): Call<Exam>

    @DELETE("exam/{id}/")
    fun deleteExam(@Path("id") id: Long): Call<Void>

    @PATCH("exam/{exam_id}/task/{task_id}/")
    fun updateTaskStatus(
        @Path("exam_id") examId: Long,
        @Path("task_id") taskId: Long,
        @Body status: RequestBody
    ): Call<Task>

    @PATCH("exam/{exam_id}/")
    fun updateExam(
        @Path("exam_id") examId: Long,
        @Body body: RequestBody
    ): Call<Exam>
}