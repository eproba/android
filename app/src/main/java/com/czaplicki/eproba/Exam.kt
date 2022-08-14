package com.czaplicki.eproba

import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime
import java.util.*

class Exam {
    var id: Int? = null
    var name: String? = null
    var scout: Int? = null
    var supervisor: Int? = null

    @SerializedName("is_archived")
    var isArchived: Boolean = false

    var tasks: MutableList<Task> = mutableListOf()

    var first_name: String? = null
    var last_name: String? = null
    var nickname: String? = null
    var team: String? = null
    var tasksTableTopCoordinate: Int? = null
    var averageLineHeight: Float? = null

    override fun toString(): String {
        return "Exam(name=$name, first_name=$first_name, last_name=$last_name, nickname=$nickname, team=$team, tasks=$tasks)"
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
        if (averageLineHeight == null) {
            averageLineHeight = height.toFloat()
        } else {
            averageLineHeight = (averageLineHeight!! + height) / 2
        }
    }

    fun toJson(): String {
        return """{
"name": "${name?.replace("\"", "\\\"")}",
"tasks": 
    [${tasks.joinToString { "\n{\"task\":\"${it.task.replace("\"", "\\\"")}\"}" }}
    ]
}"""
    }
}

data class Task(
    val task: String,
    val description: String = "",
    val status: Int = 0,
    val approver: Int? = null,
    @SerializedName("approval_date")
    val approvalDate: LocalDateTime = LocalDateTime.now(),
)