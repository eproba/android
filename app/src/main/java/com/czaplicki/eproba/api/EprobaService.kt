package com.czaplicki.eproba.api

import com.czaplicki.eproba.Exam
import com.czaplicki.eproba.User
import retrofit2.Call
import retrofit2.http.GET

interface EprobaService {

    @GET("user/")
    fun getUserInfo(): Call<User>

    @GET("exam/?user")
    fun getUserExams(): Call<List<Exam>>

    @GET("exam/?templates")
    fun getTemplates(): Call<List<Exam>>

    @GET("exam/")
    fun getExams(): Call<List<Exam>>

    @GET("exam/")
    fun getExam(id: Int): Call<Exam>
}