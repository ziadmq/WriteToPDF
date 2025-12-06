package com.mobix.editorpdf

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mobix.editorpdf.domain.models.Document

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun Navigation(navController: NavHostController = rememberNavController()) {
    val viewModel: DocumentViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "splash" // ✅ Changed start destination to Splash
    ) {
        // 1. Splash Screen Route
        composable("splash") {
            SplashScreen(navController = navController)
        }

        // 2. Home Screen Route
        composable("home") {
            HomeScreen(viewModel) { document ->
                navController.currentBackStackEntry?.savedStateHandle?.set("document", document)
                navController.navigate("editor")
            }
        }

        // 3. Editor Screen Route
        composable("editor") {
            val document = navController.previousBackStackEntry?.savedStateHandle?.get<Document>("document")
                ?: Document(0, "Untitled", listOf(""), "")

            EditorScreen(viewModel, document) {
                navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                }
            }
        }
    }
}