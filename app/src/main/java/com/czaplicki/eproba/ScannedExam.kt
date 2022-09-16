package com.czaplicki.eproba

import com.czaplicki.eproba.db.Exam
import java.util.*

class ScannedExam(
    var exam: Exam = Exam(),
) {

    var first_name: String? = null
    var last_name: String? = null
    var nickname: String? = null
    var team: String? = null
    var tasksTableTopCoordinate: Int? = null
    var averageLineHeight: Float? = null

    override fun toString(): String {
        return "Exam(id=${exam.id}, name=${exam.name}, first_name=$first_name, last_name=$last_name, nickname=$nickname, team=$team, tasks=${exam.tasks})"
    }

    fun toFormattedString(): String {
        return "Zadania:\n${exam.tasks.joinToString { "\n${it.task}" }}"
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
}