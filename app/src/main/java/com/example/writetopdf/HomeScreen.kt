package com.example.writetopdf

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.writetopdf.domain.models.Document
import com.example.writetopdf.ui.theme.*
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: DocumentViewModel,
    navigateToEditor: (document: Document) -> Unit = {}
) {
    val allDocuments = viewModel.allDocuments.collectAsState(initial = emptyList()).value
    var searchQuery by remember { mutableStateOf("") } // ✅ Search State

    // ✅ Filter Logic
    val filteredDocuments = allDocuments.filter { doc ->
        doc.title.contains(searchQuery, ignoreCase = true) ||
                doc.content.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        containerColor = GalaxyBackground,
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(GalaxyGradient)
                    .clickable {
                        val document = Document(
                            id = 0,
                            title = "Untitled Design",
                            content = "",
                            lastUpdated = LocalDate.now().toString()
                        )
                        navigateToEditor(document)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header & Search
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Hello, Creator",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = GalaxyTextPrimary
                        )
                        Text(
                            text = "What will you design today?",
                            fontSize = 14.sp,
                            color = GalaxyTextSecondary
                        )
                    }
                }

                // ✅ Search Bar UI
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(GalaxySurface, CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = GalaxyTextSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = LocalTextStyle.current.copy(color = GalaxyTextPrimary, fontSize = 16.sp),
                            cursorBrush = SolidColor(GalaxyAccentTeal),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text("Search designs...", color = GalaxyTextSecondary.copy(alpha = 0.5f))
                                }
                                innerTextField()
                            }
                        )
                    }
                }
            }

            // Grid
            if (filteredDocuments.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if(searchQuery.isEmpty()) "Your galaxy is empty.\nTap + to start." else "No results found.",
                        color = GalaxyTextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredDocuments) { document ->
                        GalaxyDocumentItem(
                            document = document,
                            onClick = { navigateToEditor(document) },
                            onDelete = { viewModel.deleteDocument(document.id) },
                            onRename = { updatedDoc -> viewModel.updateDocument(updatedDoc) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GalaxyDocumentItem(
    document: Document,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: (Document) -> Unit
) {
    val moreMenuExpanded = remember { mutableStateOf(false) }
    val renameDialogOpen = remember { mutableStateOf(false) }
    val newTitle = remember { mutableStateOf(document.title) }

    if (renameDialogOpen.value) {
        AlertDialog(
            containerColor = GalaxySurface,
            titleContentColor = GalaxyTextPrimary,
            textContentColor = GalaxyTextSecondary,
            onDismissRequest = { renameDialogOpen.value = false },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(
                    value = newTitle.value,
                    onValueChange = { newTitle.value = it },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = GalaxyTextPrimary,
                        unfocusedTextColor = GalaxyTextPrimary,
                        cursorColor = GalaxyAccentTeal,
                        focusedBorderColor = GalaxyAccentTeal,
                        unfocusedBorderColor = GalaxyTextSecondary
                    ),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRename(document.copy(title = newTitle.value))
                        renameDialogOpen.value = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GalaxyAccentPurple)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameDialogOpen.value = false }) { Text("Cancel", color = GalaxyTextSecondary) }
            }
        )
    }

    Column(
        modifier = Modifier
            .clickable { onClick() }
            .background(Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(12.dp))
                .background(GalaxySurface)
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
        ) {
            // Remove Image tags for preview text to keep it clean
            val cleanText = document.content.replace(Regex("\\[IMAGE:.*?\\]"), "[Image]")

            Text(
                text = cleanText.ifEmpty { "Empty Canvas" },
                color = GalaxyTextSecondary.copy(alpha = 0.5f),
                fontSize = 8.sp,
                lineHeight = 10.sp,
                modifier = Modifier.padding(12.dp),
                maxLines = 8,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(Brush.verticalGradient(colors = listOf(Color.Transparent, GalaxyBackground.copy(alpha=0.8f))))
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = document.title, color = GalaxyTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = document.lastUpdated, color = GalaxyTextSecondary, fontSize = 10.sp)
            }
            Box {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = GalaxyTextSecondary, modifier = Modifier.size(20.dp).clickable { moreMenuExpanded.value = true })
                DropdownMenu(
                    expanded = moreMenuExpanded.value,
                    onDismissRequest = { moreMenuExpanded.value = false },
                    modifier = Modifier.background(GalaxySurface)
                ) {
                    DropdownMenuItem(text = { Text("Rename", color = GalaxyTextPrimary) }, onClick = { renameDialogOpen.value = true; moreMenuExpanded.value = false }, leadingIcon = { Icon(Icons.Default.Edit, null, tint = GalaxyAccentTeal) })
                    DropdownMenuItem(text = { Text("Delete", color = Color(0xFFFF5252)) }, onClick = { onDelete(); moreMenuExpanded.value = false }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFFF5252)) })
                }
            }
        }
    }
}