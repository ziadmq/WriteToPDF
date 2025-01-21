package com.example.documenteditor.domain

import androidx.room.TypeConverter
import com.example.documenteditor.domain.models.FormattingData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {

    // The Kotlin function fromFormattingData is used to serialize a nullable FormattingData object into a JSON string.
    @TypeConverter
    fun fromFormattingData(value: FormattingData?): String {
        return Json.encodeToString(value ?: FormattingData(emptyList(), emptyList()))
    }

    //   It is responsible for converting a String (JSON-formatted) back into a FormattingData object.
    @TypeConverter
    fun toFormattingData(value: String): FormattingData {
        return try {
            Json.decodeFromString(value)
        } catch (e: Exception) {
            FormattingData(emptyList(), emptyList())
        }
    }
}