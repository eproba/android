package com.czaplicki.eproba.db

import androidx.room.TypeConverter
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TaskConverter {
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(
            LocalDateTime::class.java,
            JsonDeserializer { json, _, _ ->
                LocalDateTime.parse(
                    json.asJsonPrimitive.asString
                )
            })
        .registerTypeAdapter(
            LocalDateTime::class.java,
            JsonSerializer { src: LocalDateTime?, _, _ ->
                JsonPrimitive(src?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))

            })
        .create()

    @TypeConverter
    fun StringToTasks(string: String?): MutableList<Task> {
        return gson.fromJson(string, object : TypeToken<ArrayList<Task>>() {}.type)
    }

    @TypeConverter
    fun TasksToString(tasks: List<Task>): String? {
        return gson.toJson(tasks)
    }
}