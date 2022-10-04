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
    fun getUserInfo(@Path("id") id: Int): Call<User>

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
    fun getExam(@Path("id") id: Int): Call<Exam>

    @DELETE("exam/{id}/")
    fun deleteExam(@Path("id") id: Int): Call<Void>

    @PATCH("exam/{exam_id}/task/{task_id}/")
    fun updateTaskStatus(
        @Path("exam_id") examId: Int,
        @Path("task_id") taskId: Int,
        @Body status: RequestBody
    ): Call<Task>
}