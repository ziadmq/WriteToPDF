package com.example.writetopdf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.writetopdf.data.repository.DocumentRepository
import com.example.writetopdf.domain.models.Document
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class DocumentViewModel(
    private val documentRepository: DocumentRepository = Graph.documentRepository
): ViewModel() {
    lateinit var allDocuments: Flow<List<Document>>

    init {
        viewModelScope.launch {
            allDocuments = documentRepository.getAllDocuments()
        }
    }

    fun addDocument(document: Document){
        viewModelScope.launch {
            documentRepository.insertDocument(document)
        }
    }

    fun updateDocument(document: Document){
        viewModelScope.launch {
            documentRepository.updateDocument(document)
        }
    }

    fun deleteDocument(id: Int){
        viewModelScope.launch {
            documentRepository.deleteDocumentById(id)
        }
    }

    suspend fun getDocumentById(id: Int): Document {
        return documentRepository.getDocumentById(id)
    }

}