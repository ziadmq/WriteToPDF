package com.example.writetopdf

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import com.example.writetopdf.domain.models.Document

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun Navigation(navController: NavHostController = rememberNavController()){
    val viewModel: DocumentViewModel = viewModel()
    NavHost(
        navController = navController,
        startDestination = "home"
    ){
        composable("home"){
            HomeScreen(viewModel) { document ->
                navController.currentBackStackEntry?.savedStateHandle?.set("document", document)
                navController.navigate("editor")
            }
        }
        composable("editor"){
            // ✅ FIXED: We now provide a listOf("") for the pages argument
            val document = navController.previousBackStackEntry?.savedStateHandle?.get<Document>("document")
                ?: Document(0, "Untitled", listOf(""), "")

            EditorScreen(viewModel, document){
                navController.navigate("home")
            }
        }
    }
}