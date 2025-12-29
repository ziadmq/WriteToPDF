package com.mobix.editorpdf.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobix.editorpdf.domain.models.Document

@Composable
fun DocumentCard(
    doc: Document,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .height(200.dp)
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(18.dp))
            .clickable { onOpen() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFECEBFF)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {

            Row {
                Text(
                    doc.title,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 16.sp
                )
                IconButton(
                    onClick = { onDelete() },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Delete document",
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ✅ FIXED: Use the first page for the preview text
            val firstPageContent = doc.pages.firstOrNull() ?: ""
            // Remove HTML tags for cleaner preview
            val cleanPreview = firstPageContent.replace(Regex("<[^>]*>"), "")

            Text(
                cleanPreview.take(40) + "...",
                color = Color(0xFF6B6C7E),
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                doc.lastUpdated,
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 11.sp
            )
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
    val titleState = remember { mutableStateOf(document.title) }
    val isDialogOpen = remember { mutableStateOf(false) }

    // ✅ FIXED: Use pages list instead of content string
    val firstPage = document.pages.firstOrNull() ?: ""
    val previewText = firstPage.trim()
        .replace(Regex("<[^>]*>"), "") // Remove HTML
        .split("\\s+".toRegex())
        .take(18)
        .joinToString(" ")
        .ifBlank { "Tap to start writing..." }

    if (isDialogOpen.value) {
        AlertDialog(
            onDismissRequest = { isDialogOpen.value = false },
            title = { Text(text = "Rename document") },
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
                    if (titleState.value.isNotBlank()) {
                        updateDocument(
                            document.copy(title = titleState.value)
                        )
                    }
                    isDialogOpen.value = false
                }) {
                    Text(text = "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { isDialogOpen.value = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .height(220.dp)
            .clickable { navigateToEditor(document) }
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Preview area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFE3F2FD),
                                Color(0xFFBBDEFB)
                            )
                        ),
                        shape = RoundedCornerShape(
                            topStart = 14.dp,
                            topEnd = 14.dp
                        )
                    )
                    .padding(10.dp)
            ) {
                Text(
                    text = previewText,
                    fontSize = 13.sp,
                    color = Color(0xFF1A2330).copy(alpha = 0.8f),
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Title + date + menu
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = document.title.ifBlank { "Untitled document" },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = Color(0xFF102A43),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = document.lastUpdated,
                        fontSize = 11.sp,
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

                        IconButton(
                            onClick = { moreDropDownExpanded.value = true },
                            modifier = Modifier.size(24.dp)
                        ) {
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