package com.example.writetopdf.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.writetopdf.domain.Converters
import com.example.writetopdf.domain.models.Document

@Database(
    entities = [Document::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DocumentDatabase: RoomDatabase() {
    abstract fun documentDao(): DocumentDAO
}