package com.czaplicki.eproba.api

import com.google.gson.annotations.SerializedName

data class AppConfig(
    val ads: Boolean,
    val maintenance: Boolean,
    @SerializedName("min_version")
    val minVersion: Int,
)

enum class APIState {
    OK,
    MAINTENANCE,
    UPDATE_REQUIRED,
    ERROR,
}