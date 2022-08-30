package com.czaplicki.eproba.api

import com.czaplicki.eproba.Exam
import com.czaplicki.eproba.Task
import com.czaplicki.eproba.User
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

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
    fun getExams(): Call<List<Exam>>

    @GET("exam/")
    fun getExam(id: Int): Call<Exam>

    @PATCH("exam/{exam_id}/task/{task_id}/")
    fun updateTaskStatus(
        @Path("exam_id") examId: Int,
        @Path("task_id") taskId: Int,
        @Body status: RequestBody
    ): Call<Task>
}