package com.czaplicki.eproba.api

import android.content.Context
import androidx.preference.PreferenceManager
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.ZonedDateTime


class EprobaApi {
    private var retrofit: Retrofit? = null
    private var accessToken: String? = null

    fun create(context: Context, token: String?): Retrofit? {
        if (retrofit == null || accessToken != token) {
            accessToken = token
            val client: OkHttpClient = OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
                val requestBuilder: okhttp3.Request.Builder = chain.request().newBuilder()

                if (accessToken != null) {
                    requestBuilder.addHeader("Authorization", "Bearer $accessToken")
                }

                chain.proceed(requestBuilder.build())
            }).build()
            val gson = GsonBuilder().registerTypeAdapter(
                ZonedDateTime::class.java,
                JsonDeserializer { json, _, _ ->
                    ZonedDateTime.parse(
                        json.asJsonPrimitive.asString
                    )
                }).create()
            retrofit = Retrofit.Builder()
                .client(client)
                .baseUrl(
                    "${
                        PreferenceManager.getDefaultSharedPreferences(context)
                            .getString("server", "https://dev.eproba.pl")
                    }/api/"
                )
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        }
        return retrofit
    }
}