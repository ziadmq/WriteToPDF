package com.mobix.editorpdf.ui.viwmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobix.editorpdf.data.repository.DocumentRepository
import com.mobix.editorpdf.domain.models.Document
import com.mobix.editorpdf.ui.component.Graph
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