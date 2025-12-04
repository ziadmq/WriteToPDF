package com.example.writetopdf

import android.content.Context
import androidx.room.Room
import com.example.writetopdf.data.DocumentDatabase
import com.example.writetopdf.data.DocumentRepository

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