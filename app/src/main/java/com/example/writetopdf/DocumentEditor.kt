package com.example.documenteditor

import android.app.Application

class DocumentEditor: Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.provide(this)
    }
}