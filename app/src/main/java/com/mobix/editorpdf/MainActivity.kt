package com.example.writetopdf

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import com.mobix.editorpdf.Navigation
import com.mobix.editorpdf.ui.theme.DocumentEditorTheme
import com.mobix.editorpdf.R

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_DocumentEditor)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // REMOVED: checkAndRequestPermissions()

        setContent {
            DocumentEditorTheme {
                Navigation()
            }
        }
    }
}