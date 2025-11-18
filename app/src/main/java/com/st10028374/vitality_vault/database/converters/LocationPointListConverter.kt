package com.st10028374.vitality_vault.database.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.st10028374.vitality_vault.routes.models.LocationPoint

/**
 * Type converter for List<LocationPoint>
 */
class LocationPointListConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromLocationPointList(value: List<LocationPoint>?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toLocationPointList(value: String): List<LocationPoint> {
        val listType = object : TypeToken<List<LocationPoint>>() {}.type
        return gson.fromJson(value, listType)
    }
}