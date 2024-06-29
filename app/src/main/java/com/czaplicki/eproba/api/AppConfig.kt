package com.czaplicki.eproba.api

import com.google.gson.annotations.SerializedName

data class AppConfig(
    val ads: Boolean,
    val maintenance: Boolean,
    @SerializedName("min_version")
    val minVersion: Int,
    @SerializedName("the_end")
    val theEnd: Boolean,
    @SerializedName("end_messages")
    val endMessages: List<String>,
)

enum class APIState {
    OK,
    MAINTENANCE,
    UPDATE_REQUIRED,
    ERROR,
    END_OF_LIFE,
}