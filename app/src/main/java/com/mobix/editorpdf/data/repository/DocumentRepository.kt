package com.mobix.editorpdf.data.repository

import com.mobix.editorpdf.data.local.DocumentDAO
import com.mobix.editorpdf.domain.models.Document
import kotlinx.coroutines.flow.Flow

class DocumentRepository(private val documentDao: DocumentDAO) {
    fun getAllDocuments(): Flow<List<Document>> {
        return documentDao.getAllDocuments()
    }

    suspend fun getDocumentById(id: Int): Document {
        return documentDao.getDocumentById(id)
    }

    suspend fun deleteDocumentById(id: Int) {
        documentDao.deleteDocumentById(id)
    }

    suspend fun insertDocument(document: Document) {
        documentDao.insertDocument(document)
    }

    suspend fun updateDocument(document: Document) {
        documentDao.updateDocument(document)
    }
}


