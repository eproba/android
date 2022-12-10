package com.czaplicki.eproba.db

import androidx.room.TypeConverter
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class ZonedDateTimeConverter {
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
            JsonSerializer { src: ZonedDateTime, _, _ ->
                JsonPrimitive(src.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))

            })
        .create()

    @TypeConverter
    fun StringToZonedDateTime(string: String?): ZonedDateTime {
        return gson.fromJson(string, object : TypeToken<ZonedDateTime>() {}.type)
    }

    @TypeConverter
    fun ZonedDateTimeToString(exams: ZonedDateTime): String? {
        return gson.toJson(exams)
    }
}