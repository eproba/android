package com.czaplicki.eproba.api

import com.czaplicki.eproba.db.Worksheet
import com.czaplicki.eproba.db.FCMDevice
import com.czaplicki.eproba.db.Task
import com.czaplicki.eproba.db.User
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*
import java.util.UUID

interface EprobaService {

    @GET("api-config/")
    suspend fun getAppConfig(): ApiConfig

    @GET("user/")
    fun getUserCall(): Call<User>

    @GET("user/")
    suspend fun getUser(): User

    @GET("users/{id}/")
    suspend fun getUser(@Path("id") id: UUID): User

    @GET("users/")
    suspend fun getUsers(): List<User>

    @GET("worksheets/?user")
    suspend fun getUserWorksheets(): List<Worksheet>

    @GET("worksheets/?user")
    suspend fun getUserWorksheets(@Query("last_sync") lastSync: Long): List<Worksheet>

    @GET("worksheets/?templates")
    suspend fun getTemplates(): List<Worksheet>

    @GET("worksheets/?archived")
    suspend fun getArchivedWorksheets(): List<Worksheet>

    @GET("worksheets/")
    suspend fun getWorksheets(): List<Worksheet>

    @GET("worksheets/")
    suspend fun getWorksheets(@Query("last_sync") lastSync: Long): List<Worksheet>

    @GET("worksheets/tasks/tbc/")
    suspend fun getTasksTBC(): List<Worksheet>

    @GET("worksheets/{id}/")
    suspend fun getWorksheet(@Path("id") id: UUID): Worksheet

    @DELETE("worksheets/{id}/")
    suspend fun deleteWorksheet(@Path("id") id: UUID): Response<Void>

    @DELETE("worksheets/{id}/?archived")
    suspend fun deleteArchivedWorksheet(@Path("id") id: UUID): Response<Void>

    @PATCH("worksheets/{worksheet_id}/task/{task_id}/")
    fun updateTaskStatus(
        @Path("worksheet_id") worksheetId: UUID,
        @Path("task_id") taskId: UUID,
        @Body status: RequestBody
    ): Call<Task>

    @POST("worksheets/{worksheet_id}/task/{task_id}/submit")
    suspend fun submitTask(
        @Path("worksheet_id") worksheetId: UUID,
        @Path("task_id") taskId: UUID,
        @Body approver: RequestBody
    ): Task

    @POST("worksheets/{worksheet_id}/task/{task_id}/unsubmit")
    suspend fun unsubmitTask(
        @Path("worksheet_id") worksheetId: UUID,
        @Path("task_id") taskId: UUID
    ): Task

    @PATCH("worksheets/{worksheet_id}/")
    suspend fun updateWorksheet(
        @Path("worksheet_id") worksheetId: UUID,
        @Body body: RequestBody
    ): Worksheet

    @PATCH("worksheets/{worksheet_id}/?archived")
    suspend fun updateWorksheetInArchive(
        @Path("worksheet_id") worksheetId: UUID,
        @Body body: RequestBody
    ): Worksheet

    @POST("worksheets/")
    suspend fun createWorksheet(@Body body: RequestBody): Worksheet

    @POST("fcm/devices/")
    suspend fun registerFCMDevice(@Body body: RequestBody): FCMDevice
}