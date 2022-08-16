package com.czaplicki.eproba.api

import android.content.Context
import androidx.preference.PreferenceManager
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import java.time.ZonedDateTime


class EprobaApi {
    private var retrofit: Retrofit? = null

    fun getRetrofitInstance(context: Context, token: String): Retrofit? {
        if (retrofit == null) {
            val client: OkHttpClient = OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
                val newRequest: okhttp3.Request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
                chain.proceed(newRequest)
            }).build()
            val gson = GsonBuilder().registerTypeAdapter(
                LocalDateTime::class.java,
                JsonDeserializer { json, _, _ ->
                    ZonedDateTime.parse(
                        json.asJsonPrimitive.asString
                    ).toLocalDateTime()
                }).create()
            retrofit = Retrofit.Builder()
                .client(client)
                .baseUrl(
                    "${
                        PreferenceManager.getDefaultSharedPreferences(context)
                            .getString("server", "https://scouts-exams.herokuapp.com")
                    }/api/"
                )
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        }
        return retrofit
    }
}