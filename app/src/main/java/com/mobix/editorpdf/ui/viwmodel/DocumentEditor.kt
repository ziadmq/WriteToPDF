package com.mobix.editorpdf.ui.viwmodel

import android.app.Application
import com.mobix.editorpdf.ui.component.Graph

class DocumentEditor: Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.provide(this)
    }
}