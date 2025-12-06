package com.mobix.editorpdf.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mobix.editorpdf.domain.Converters
import com.mobix.editorpdf.domain.models.Document

@Database(
    entities = [Document::class], // Ensure this says Document::class, NOT DocumentEntity::class
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DocumentDatabase: RoomDatabase() {
    abstract fun documentDao(): DocumentDAO
}