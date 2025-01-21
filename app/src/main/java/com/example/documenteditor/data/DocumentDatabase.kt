package com.example.documenteditor.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.documenteditor.domain.Converters
import com.example.documenteditor.domain.models.Document

@Database(
    entities = [Document::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DocumentDatabase: RoomDatabase() {
    abstract fun documentDao(): DocumentDAO
}