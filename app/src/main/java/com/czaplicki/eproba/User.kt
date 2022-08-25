package com.czaplicki.eproba

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "user")
data class User(
    @PrimaryKey
    val id: Int,
    @ColumnInfo(name = "first_name")
    @SerializedName("first_name")
    val firstName: String?,
    @ColumnInfo(name = "last_name")
    @SerializedName("last_name")
    val lastName: String?,
    val nickname: String?,
    val email: String?,
    val scout: Scout
) {
    val fullName: String
        get() = "$firstName $lastName"

    val fullNameWithNickname: String
        get() = "$fullName \"$nickname\""
}

data class Scout(
    @ColumnInfo(name = "patrol")
    @SerializedName("patrol")
    val patrolId: Int?,
    @ColumnInfo(name = "patrol_name")
    @SerializedName("patrol_name")
    val patrolName: String?,
    @ColumnInfo(name = "team")
    @SerializedName("team")
    val teamId: Int?,
    @ColumnInfo(name = "team_name")
    @SerializedName("team_name")
    val teamName: String?,
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