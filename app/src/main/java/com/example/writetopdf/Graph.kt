package com.example.writetopdf

import android.content.Context
import androidx.room.Room
import com.example.writetopdf.data.local.DocumentDatabase
import com.example.writetopdf.data.repository.DocumentRepository

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