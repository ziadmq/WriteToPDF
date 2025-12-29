package com.example.writetopdf

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import com.google.android.gms.ads.MobileAds
import com.mobix.editorpdf.ui.navigation.Navigation
import com.mobix.editorpdf.ui.theme.DocumentEditorTheme
import com.mobix.editorpdf.R
import com.mobix.editorpdf.ui.component.AdManager

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_DocumentEditor)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MobileAds.initialize(this) {}
        AdManager.loadInterstitial(this)
        setContent {
            DocumentEditorTheme {
                Navigation()
            }
        }
    }
}