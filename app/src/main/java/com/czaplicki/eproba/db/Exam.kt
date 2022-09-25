package com.czaplicki.eproba.db

import android.graphics.drawable.Icon
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.czaplicki.eproba.R
import com.google.gson.annotations.SerializedName
import java.time.ZonedDateTime
import java.util.*

@Entity(tableName = "exams")
class Exam(
    @PrimaryKey
    var id: Int? = null,
    var name: String? = null,
    @SerializedName("user") var userId: Int? = null,
    var supervisor: Int? = null,
    @SerializedName("is_archived") var isArchived: Boolean = false,
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

    @Ignore
    var tasksTableTopCoordinate: Int? = null

    @Ignore
    var averageLineHeight: Float? = null

    override fun toString(): String {
        return "Exam(id=$id. name=$name, first_name=$first_name, last_name=$last_name, nickname=$nickname, team=$team, tasks=$tasks)"
    }

    fun toFormattedString(): String {
        return "$name\nImię: $first_name\nNazwisko: $last_name\nPseudonim: $nickname\nDrużyna: $team\nZadania:\n${tasks.joinToString { "\n$it" }}"
    }

    fun setFirstName(firstName: String) {
        if (firstName.isNotEmpty()) {
            first_name =
                firstName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
    }

    fun setLastName(lastName: String) {
        if (lastName.isNotEmpty()) {
            last_name =
                lastName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
    }

    @JvmName("setNickname1")
    fun setNickname(nickname: String) {
        if (nickname.isNotEmpty()) {
            this.nickname =
                nickname.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
    }

    @JvmName("setTeam1")
    fun setTeam(team: String) {
        if (team.isNotEmpty()) {
            this.team = team
        }
    }

    fun setTaskTableTopCoordinate(y: Int) {
        tasksTableTopCoordinate = y
    }

    fun updateAverageLineHeight(height: Int) {
        averageLineHeight = if (averageLineHeight == null) {
            height.toFloat()
        } else {
            (averageLineHeight!! + height) / 2
        }
    }

    fun toJson(): String {
        return """{
"name": "${name?.replace("\"", "\\\"")}",
"user": $userId,
"tasks": 
    [${tasks.joinToString { "\n{\"task\":\"${it.task.replace("\"", "\\\"")}\"}" }}
    ]
}"""
    }
}

data class Task(
    val id: Int,
    val task: String,
    val description: String = "",
    var status: Int = 0,
    var approver: Int? = null,
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