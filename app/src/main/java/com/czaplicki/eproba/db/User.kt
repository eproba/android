package com.czaplicki.eproba.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.UUID

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: UUID,
    @ColumnInfo(name = "first_name")
    @SerializedName("first_name")
    val firstName: String?,
    @ColumnInfo(name = "last_name")
    @SerializedName("last_name")
    val lastName: String?,
    val nickname: String?,
    val email: String?,
    @ColumnInfo(name = "patrol")
    @SerializedName("patrol")
    val patrolId: UUID?,
    @ColumnInfo(name = "patrol_name")
    @SerializedName("patrol_name")
    val patrolName: String?,
    @ColumnInfo(name = "team")
    @SerializedName("team")
    val teamId: UUID?,
    @ColumnInfo(name = "team_name")
    @SerializedName("team_name")
    val teamName: String?,
    val rank: String,
    val function: Int,) {
    val fullName: String
        get() = "$firstName $lastName"

    val fullNameWithNickname: String
        get() = "$fullName \"$nickname\""

    val nicknameWithRank: String
        get() = if (rank != " ") "$rank $nickname" else "$nickname"


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
