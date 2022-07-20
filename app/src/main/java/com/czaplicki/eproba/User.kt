package com.czaplicki.eproba

import com.google.gson.annotations.SerializedName

data class User(
    val id: Int,
    @SerializedName("first_name")
    val firstName: String,
    @SerializedName("last_name")
    val lastName: String,
    val nickname: String,
    val email: String,
    val scout: Scout
) {
    val fullName: String
        get() = "$firstName $lastName"

    val fullNameWithNickname: String
        get() = "$fullName \"$nickname\""
}

data class Scout(
    @SerializedName("patrol")
    val patrolId: Int,
    @SerializedName("patrol_name")
    val patrolName: String,
    @SerializedName("team")
    val teamId: Int,
    @SerializedName("team_name")
    val teamName: String,
    val rank: String,
    val function: Int,
) {

    val functionName: String
        get() = when (function) {
            0 -> "Druh"
            1 -> "Podzastępowy"
            2 -> "Zastępowy"
            3 -> "Przyboczny"
            4 -> "Drużynowy"
            5 -> "Wyższa funkcja"
            else -> "Nieznana"
        }
}