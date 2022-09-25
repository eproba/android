package com.czaplicki.eproba.db

import androidx.room.TypeConverter
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class TaskConverter {
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(
            ZonedDateTime::class.java,
            JsonDeserializer { json, _, _ ->
                ZonedDateTime.parse(
                    json.asJsonPrimitive.asString
                )
            })
        .registerTypeAdapter(
            ZonedDateTime::class.java,
            JsonSerializer { src: ZonedDateTime?, _, _ ->
                JsonPrimitive(src?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))

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