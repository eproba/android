package com.czaplicki.eproba.db

import android.graphics.drawable.Icon
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.czaplicki.eproba.R
import com.google.gson.annotations.SerializedName
import java.sql.Timestamp
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

@Entity(tableName = "worksheets")
class Worksheet(
    @PrimaryKey
    var id: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000"),
    var name: String? = null,
    @SerializedName("user_id") var userId: UUID? = null,
    var supervisor: UUID? = null,
    @SerializedName("deleted") var isDeleted: Boolean = false,
    @SerializedName("is_archived") var isArchived: Boolean = false,
    @SerializedName("updated_at") var lastUpdate: ZonedDateTime = Timestamp(0).toInstant()
        .atZone(ZoneId.systemDefault()),
    var tasks: MutableList<Task> = mutableListOf()
) {

    @Ignore
    var first_name: String? = null

    @Ignore
    var last_name: String? = null

    @Ignore
    var nickname: String? = null

    @Ignore
    var team: String? = null


    override fun toString(): String {
        return "Worksheet(id=$id. name=$name, first_name=$first_name, last_name=$last_name, nickname=$nickname, team=$team, tasks=$tasks)"
    }

    fun toFormattedString(): String {
        return "$name\nImię: $first_name\nNazwisko: $last_name\nPseudonim: $nickname\nDrużyna: $team\nZadania:\n${tasks.joinToString { "\n$it" }}"
    }


    fun toJson(): String {
        return """{
"name": "${name?.replace("\"", "\\\"")}",
"user_id": "$userId",
"tasks": 
    [${tasks.joinToString { "\n{\"task\":\"${it.task.replace("\"", "\\\"")}\"}" }}
    ]
}"""
    }
}

data class Task(
    val id: UUID,
    val task: String,
    val description: String = "",
    var status: Int = 0,
    var approver: UUID? = null,
    @SerializedName("approval_date")
    var approvalDate: ZonedDateTime? = ZonedDateTime.now(),
) {
    class Status {
        companion object {
            const val PENDING = 0
            const val AWAITING_APPROVAL = 1
            const val APPROVED = 2
            const val REJECTED = 3
        }
    }


    val statusIcon: Icon
        get() = when (status) {
            0 -> Icon.createWithResource(
                "com.czaplicki.eproba",
                R.drawable.radio_button_unchecked_24px
            )

            1 -> Icon.createWithResource("com.czaplicki.eproba", R.drawable.schedule_24px)
            2 -> Icon.createWithResource("com.czaplicki.eproba", R.drawable.check_circle_24px)
            3 -> Icon.createWithResource("com.czaplicki.eproba", R.drawable.cancel_24px)
            else -> Icon.createWithResource("com.czaplicki.eproba", R.drawable.ic_help)
        }
}