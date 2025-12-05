package com.example.writetopdf.domain

import androidx.room.TypeConverter
import com.example.writetopdf.domain.models.FormattingData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    // ✅ 1. Converters for the Pages (List<String>)
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return json.encodeToString(value ?: emptyList())
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ✅ 2. Converters for Formatting (FormattingData)
    @TypeConverter
    fun fromFormattingData(value: FormattingData?): String {
        return json.encodeToString(value ?: FormattingData(emptyList(), emptyList()))
    }

    @TypeConverter
    fun toFormattingData(value: String): FormattingData {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            FormattingData(emptyList(), emptyList())
        }
    }
}