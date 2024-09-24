package com.czaplicki.eproba.db

import com.google.gson.annotations.SerializedName
import java.time.ZonedDateTime

class FCMDevice(
    val id: Long? = null,
    val name: String? = null,
    @SerializedName("registration_id")
    val token: String,
    @SerializedName("device_id")
    val deviceId: String? = null,
    val active: Boolean? = true,
    @SerializedName("date_created")
    val registrationDate: ZonedDateTime? = null,
    val type: String = "android",
) {

    fun toJSONString(): String {
        return "{\"name\": \"${name?.replace("\"", "\\\"")}\", \"registration_id\": \"$token\", \"device_id\": \"$deviceId\", \"type\": \"$type\"}"
    }
}