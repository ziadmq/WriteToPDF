package com.example.writetopdf

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.mobix.editorpdf.Navigation
import com.mobix.editorpdf.ui.theme.DocumentEditorTheme
import com.mobix.editorpdf.R


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        // ✅ Switch back to normal theme (from Splash)
        setTheme(R.style.Theme_DocumentEditor)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkAndRequestPermissions()
        setContent {
            DocumentEditorTheme {
                Navigation()
            }
        }
    }

    private fun checkAndRequestPermissions() {
        // ✅ 1. Check if we have ALREADY asked for permissions
        val sharedPref = getPreferences(Context.MODE_PRIVATE)
        val hasAsked = sharedPref.getBoolean("has_asked_permissions", false)

        if (hasAsked) {
            return // Stop here if we already asked once (even if denied)
        }

        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For API 33 and above, request specific media permissions
            arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            // For older versions, request storage permissions
            arrayOf(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())

            // ✅ 2. Save that we have asked, so we don't ask again
            with(sharedPref.edit()) {
                putBoolean("has_asked_permissions", true)
                apply()
            }
        }
    }

    // Register the permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allPermissionsGranted = permissions.all { it.value }
        if (allPermissionsGranted) {
            // All permissions granted
        } else {
            // Permissions denied - app will continue but might not save files
        }
    }
}