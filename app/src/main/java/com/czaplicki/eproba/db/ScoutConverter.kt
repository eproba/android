package com.czaplicki.eproba.db

import androidx.room.TypeConverter
import com.google.gson.Gson

class ScoutConverter {
    @TypeConverter
    fun StringToScout(string: String?): Scout {
        return Gson().fromJson(string, Scout::class.java)
    }

    @TypeConverter
    fun ScoutToString(scout: Scout): String? {
        return Gson().toJson(scout)
    }
}