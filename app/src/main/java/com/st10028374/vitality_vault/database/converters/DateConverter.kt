package com.st10028374.vitality_vault.database.converters

import androidx.room.TypeConverter
import java.util.Date

/**
 * Type converter for Date
 */
class DateConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}