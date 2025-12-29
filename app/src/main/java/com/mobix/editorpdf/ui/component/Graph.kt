package com.mobix.editorpdf.ui.component

import android.content.Context
import androidx.room.Room
import com.mobix.editorpdf.data.local.DocumentDatabase
import com.mobix.editorpdf.data.repository.DocumentRepository

object Graph {
    private lateinit var database: DocumentDatabase

    val documentRepository by lazy {
        DocumentRepository(documentDao = database.documentDao())
    }

    fun provide (context: Context){
        database = Room.databaseBuilder(context, DocumentDatabase::class.java, "documents.db")
            .fallbackToDestructiveMigration()
            .build()
    }
}