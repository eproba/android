package com.czaplicki.eproba.api

import com.google.gson.annotations.SerializedName

data class ApiConfig(
    val ads: Boolean,
    @SerializedName("api_maintenance")
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