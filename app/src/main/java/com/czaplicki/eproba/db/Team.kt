package com.czaplicki.eproba.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "teams")
data class Team(
    @PrimaryKey
    val id: Long,
    val name: String,
    @ColumnInfo(name = "short_name")
    @SerializedName("short_name")
    val shortName: String? = null,
)

@Entity(tableName = "patrols")
data class Patrol(
    val id: Long,
    val name: String,
    val teamId: Long,
)