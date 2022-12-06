package com.czaplicki.eproba.api

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.czaplicki.eproba.EprobaApplication
import com.czaplicki.eproba.LoggedOutScreen
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import kotlinx.coroutines.runBlocking
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.internal.http.HTTP_UNAUTHORIZED
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
                            .getString("server", "https://eproba.pl")
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

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        var accessToken: String? = app.authStateManager.current.accessToken
        if (accessToken == null || app.authStateManager.current.needsTokenRefresh) {
            runBlocking {
                app.authService.performTokenRequest(app.authStateManager.current.createTokenRefreshRequest()) { tokenResponse, ex ->
                    if (tokenResponse != null) {
                        app.authStateManager.updateAfterTokenResponse(tokenResponse, ex)
                        accessToken = tokenResponse.accessToken
                    }
                }
            }
        }
        val request: Request = newRequestWithAccessToken(chain.request(), accessToken!!)
        var response = chain.proceed(request)

        if (response.code == HTTP_UNAUTHORIZED) {
            try {
                runBlocking {
                    accessToken = refreshToken()
                }
                val request: Request = newRequestWithAccessToken(chain.request(), accessToken!!)
                return chain.proceed(request)
            } catch (e: AuthorizationException) {
                if (e == AuthorizationException.TokenRequestErrors.INVALID_GRANT) {
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
        } else {
            return response
        }

        return Response.Builder() // This is a dummy response to satisfy the compiler
            .code(418)
            .body("".toResponseBody(null))
            .protocol(Protocol.HTTP_3)
            .message("Dummy response")
            .request(chain.request())
            .build()
    }

    private fun newRequestWithAccessToken(request: Request, accessToken: String): Request {
        return request.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()
    }

    private suspend fun refreshToken(): String = suspendCoroutine {
        if (app.authStateManager.current.refreshToken == null) {
            it.resumeWithException(AuthorizationException.AuthorizationRequestErrors.INVALID_REQUEST)
        }
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

//class AccessTokenAuthenticator : Authenticator {
//    val app = EprobaApplication.instance
//
//
//    override fun authenticate(route: Route?, response: Response): Request? {
//        var accessToken: String?
//        if (!isRequestWithAccessToken(response)) {
//            return null
//        }
//        runBlocking {
//            accessToken = refreshToken()
//        }
//
//        return newRequestWithAccessToken(response.request, accessToken!!)
//
//    }
//
//    private fun isRequestWithAccessToken(response: Response): Boolean {
//        val header = response.request.header("Authorization")
//        return header != null && header.startsWith("Bearer")
//    }
//
//    fun newRequestWithAccessToken(request: Request, accessToken: String): Request {
//        return request.newBuilder()
//            .header("Authorization", "Bearer $accessToken")
//            .build()
//    }
//
//    private suspend fun refreshToken(): String = suspendCoroutine {
//        if (app.authStateManager.current.refreshToken == null) {
//            it.resumeWithException(AuthorizationException.AuthorizationRequestErrors.INVALID_REQUEST)
//        }
//        app.authService.performTokenRequest(app.authStateManager.current.createTokenRefreshRequest()) { tokenResponse, ex ->
//            if (ex?.code == AuthorizationException.TokenRequestErrors.INVALID_GRANT.code) {
//                app.authStateManager.replace(AuthState())
//                it.resumeWithException(ex)
//                return@performTokenRequest
//            }
//            if (tokenResponse != null) {
//                app.authStateManager.updateAfterTokenResponse(tokenResponse, ex)
//            }
//            if (tokenResponse != null) {
//                tokenResponse.accessToken?.let { it1 -> it.resume(it1) }
//            } else {
//                it.resume("")
//            }
//        }
//    }
//}