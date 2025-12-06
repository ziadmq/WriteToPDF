package com.mobix.editorpdf

import android.app.Application

class DocumentEditor: Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.provide(this)
    }
}