package com.feuerwehr.checklist.data.local.converter

import androidx.room.TypeConverter
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * Room type converters for kotlinx.datetime types
 */
class DateTimeConverters {
    
    @TypeConverter
    fun fromInstant(instant: Instant?): String? = instant?.toString()
    
    @TypeConverter
    fun toInstant(instantString: String?): Instant? = 
        instantString?.let { Instant.parse(it) }
    
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()
    
    @TypeConverter
    fun toLocalDate(dateString: String?): LocalDate? = 
        dateString?.let { LocalDate.parse(it) }
}