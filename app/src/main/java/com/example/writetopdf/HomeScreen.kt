package com.example.documenteditor

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.documenteditor.domain.models.Document
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: DocumentViewModel,
    navigateToEditor: (document: Document) -> Unit = {}
) {
    val allDocuments = viewModel.allDocuments.collectAsState(initial = emptyList()).value

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "WriteToPDF",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    Image(
                        painter = painterResource(id = R.drawable.doc_logo),
                        contentDescription = null,
                        modifier = Modifier.size(50.dp)
                    )
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E88E5) // Blue color for the app bar
                )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .height(220.dp)
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF42A5F5),
                                Color(0xFF1E88E5)
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        val document = Document(
                            id = 0,
                            title = "New Document",
                            content = "This is a new document...",
                            lastUpdated = LocalDate
                                .now()
                                .toString()
                        )
                        viewModel.addDocument(document)
                        navigateToEditor(document)
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(50.dp),
                        tint = Color.White
                    )
                    Text(
                        text = "Create New Document",
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                items(allDocuments) { document ->
                    DocumentItem(
                        document = document,
                        navigateToEditor,
                        deleteDocument = { viewModel.deleteDocument(document.id) },
                        updateDocument = { updatedDoc -> viewModel.updateDocument(updatedDoc) }
                    )
                }
            }
        }
    }
}

@Composable
fun DocumentItem(
    document: Document,
    navigateToEditor: (document: Document) -> Unit,
    deleteDocument: () -> Unit,
    updateDocument: (document: Document) -> Unit
) {
    val moreDropDownExpanded = remember { mutableStateOf(false) }
    val titleState = remember { mutableStateOf("") }
    val isDialogOpen = remember { mutableStateOf(false) }

    val previewText = document.content.trim()
        .split("\\s+".toRegex())
        .take(10)
        .joinToString(" ")

    if (isDialogOpen.value) {
        AlertDialog(
            onDismissRequest = { isDialogOpen.value = false },
            title = { Text(text = "Enter Document Title") },
            text = {
                Column {
                    TextField(
                        value = titleState.value,
                        onValueChange = { titleState.value = it },
                        placeholder = { Text(text = "Document Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    updateDocument(
                        document.copy(title = titleState.value)
                    )
                    isDialogOpen.value = false
                }) {
                    Text(text = "Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { isDialogOpen.value = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .height(240.dp)
            .width(180.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(10.dp)
            )
            .background(Color.White, shape = RoundedCornerShape(10.dp))
            .clickable { navigateToEditor(document) }
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFBBDEFB),
                                Color(0xFF90CAF9)
                            )
                        ),
                        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                    )
            ) {
                Text(
                    text = "$previewText...",
                    fontSize = 14.sp,
                    color = Color.Black.copy(alpha = 0.7f),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.Center)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = document.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = document.lastUpdated,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Box {
                        DropdownMenu(
                            expanded = moreDropDownExpanded.value,
                            onDismissRequest = { moreDropDownExpanded.value = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                onClick = {
                                    isDialogOpen.value = true
                                    moreDropDownExpanded.value = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    deleteDocument()
                                    moreDropDownExpanded.value = false
                                }
                            )
                        }

                        IconButton(onClick = { moreDropDownExpanded.value = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun PreviewDocumentItem() {
    val sampleDocument = Document(
        id = 1,
        title = "Sample Document",
        content = "This is a preview of the document content.",
        lastUpdated = "2023-09-25"
    )

    DocumentItem(
        document = sampleDocument,
        navigateToEditor = { /* No-op for preview */ },
        deleteDocument = { /* No-op for preview */ },
        updateDocument = {}
    )
}
