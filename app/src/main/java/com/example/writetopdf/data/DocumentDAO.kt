package com.example.writetopdf.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.writetopdf.domain.models.Document
import kotlinx.coroutines.flow.Flow

@Dao
abstract class DocumentDAO {
    @Query("SELECT * FROM documents")
    abstract fun getAllDocuments(): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE id = :id")
    abstract suspend fun getDocumentById(id: Int): Document

//    @Query("SELECT * FROM documents WHERE title = :title")
//    abstract fun getDocumentByTitle(title: String): Document

    @Query("DELETE FROM documents WHERE id = :id")
    abstract suspend fun deleteDocumentById(id: Int)

    @Insert
    abstract suspend fun insertDocument(document: Document)

    @Update
    abstract suspend fun updateDocument(document: Document)
}