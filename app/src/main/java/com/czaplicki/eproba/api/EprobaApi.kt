package com.czaplicki.eproba.api

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.czaplicki.eproba.EprobaApplication
import com.czaplicki.eproba.LoggedOutScreen
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import kotlinx.coroutines.runBlocking
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import okhttp3.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class EprobaApi {
    private var retrofit: Retrofit? = null
    lateinit var client: OkHttpClient

    fun create(context: Context): Retrofit? {
        if (retrofit == null) {
            client = OkHttpClient.Builder().callTimeout(10, SECONDS)
                .addInterceptor(AccessTokenInterceptor())
                .addInterceptor(
                    ChuckerInterceptor.Builder(context)
                        .collector(ChuckerCollector(context))
                        .maxContentLength(250000L)
                        .redactHeaders(emptySet())
                        .alwaysReadResponseBody(false)
                        .build()
                )
                .authenticator(TokenAuthenticator())
                .build()
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
                            .getString("server", "https://eproba.zhr.pl")
                    }/api/"
                )
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        }
        return retrofit
    }
}

class AccessTokenInterceptor : Interceptor {

    val app = EprobaApplication.instance
    private val authenticator = TokenAuthenticator()

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        var accessToken: String = app.authStateManager.current.accessToken
            ?: return chain.proceed(chain.request())
        synchronized(this) {
            if (app.authStateManager.current.needsTokenRefresh) {
                try {
                    runBlocking {
                        accessToken = authenticator.refreshToken()
                    }
                } catch (e: AuthorizationException) {
                    if (e == AuthorizationException.TokenRequestErrors.INVALID_GRANT || e == AuthorizationException.TokenRequestErrors.INVALID_REQUEST) {
                        app.sharedPreferences.edit().clear().apply()
                        app.getActiveActivity()?.let { activity ->
                            LoggedOutScreen().show(
                                (activity as AppCompatActivity).supportFragmentManager,
                                "loggedOut"
                            )
                        }
                        app.api.client.dispatcher.cancelAll()
                    } else {
                        throw e
                    }
                }
            }
        }
        val request: Request =
            authenticator.newRequestWithAccessToken(chain.request(), accessToken)
        return chain.proceed(request)
    }

}

class TokenAuthenticator : Authenticator {
    val app = EprobaApplication.instance


    @Synchronized
    override fun authenticate(route: Route?, response: Response): Request? {
        var accessToken: String? = app.authStateManager.current.accessToken
        val accessTokenOfRequest = response.request.header("Authorization")?.split(" ")?.get(1)
        if (!isRequestWithAccessToken(response)) {
            return null
        }
        if (accessToken != null && accessTokenOfRequest != accessToken) {
            return newRequestWithAccessToken(response.request, accessToken)
        }

        try {
            runBlocking {
                accessToken = refreshToken()
            }
            return newRequestWithAccessToken(response.request, accessToken!!)
        } catch (e: AuthorizationException) {
            if (e == AuthorizationException.TokenRequestErrors.INVALID_GRANT || e == AuthorizationException.TokenRequestErrors.INVALID_REQUEST) {
                app.sharedPreferences.edit().clear().apply()
                app.getActiveActivity()?.let { activity ->
                    LoggedOutScreen().show(
                        (activity as AppCompatActivity).supportFragmentManager,
                        "loggedOut"
                    )
                }
                app.api.client.dispatcher.cancelAll()
            } else {
                throw e
            }
        } catch (e: IllegalStateException) {
            app.sharedPreferences.edit().clear().apply()
            app.getActiveActivity()?.let { activity ->
                LoggedOutScreen().show(
                    (activity as AppCompatActivity).supportFragmentManager,
                    "loggedOut"
                )
            }
            app.api.client.dispatcher.cancelAll()
        }
        return null

    }

    private fun isRequestWithAccessToken(response: Response): Boolean {
        val header = response.request.header("Authorization")
        return header != null && header.startsWith("Bearer")
    }

    fun newRequestWithAccessToken(request: Request, accessToken: String): Request {
        return request.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()
    }

    suspend fun refreshToken(): String = suspendCoroutine {
        if (app.authStateManager.current.refreshToken == null) {
            it.resumeWithException(AuthorizationException.TokenRequestErrors.INVALID_REQUEST)
        } else {
            app.authService.performTokenRequest(app.authStateManager.current.createTokenRefreshRequest()) { tokenResponse, ex ->
                if (ex?.code == AuthorizationException.TokenRequestErrors.INVALID_GRANT.code) {
                    app.authStateManager.replace(AuthState())
                    it.resumeWithException(ex)
                    return@performTokenRequest
                }
                if (tokenResponse != null) {
                    app.authStateManager.updateAfterTokenResponse(tokenResponse, ex)
                }
                if (tokenResponse != null) {
                    tokenResponse.accessToken?.let { it1 -> it.resume(it1) }
                } else {
                    it.resume("")
                }
            }
        }
    }
}