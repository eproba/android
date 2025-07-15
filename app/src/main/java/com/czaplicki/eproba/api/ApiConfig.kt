package com.czaplicki.eproba.api

import com.google.gson.annotations.SerializedName
import java.time.ZonedDateTime
import java.util.Date

data class ApiConfig(
    val ads: Boolean,
    @SerializedName("api_maintenance")
    val maintenance: Boolean,
    @SerializedName("min_version")
    val minVersion: Int,
    @SerializedName("eol_date")
    val eolDate: Date,
    @SerializedName("eol_screen_enabled")
    val eolScreenEnabled: Boolean,
    @SerializedName("eol_screen_title")
    val eolScreenTitle: String,
    @SerializedName("eol_screen_description")
    val eolScreenDescription: String,
    @SerializedName("eol_screen_button_text")
    val eolScreenButtonText: String,
    @SerializedName("eol_screen_button_url")
    val eolScreenButtonUrl: String,
)

enum class APIState {
    OK,
    MAINTENANCE,
    UPDATE_REQUIRED,
    ERROR,
    EOL,
}