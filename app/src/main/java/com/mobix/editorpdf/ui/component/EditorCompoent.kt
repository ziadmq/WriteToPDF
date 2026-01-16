package com.mobix.editorpdf.ui.component

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobix.editorpdf.domain.models.Document
import com.mobix.editorpdf.domain.models.FormattingData
import com.mobix.editorpdf.domain.models.ParagraphStyleData
import com.mobix.editorpdf.domain.models.SpanStyleData
import com.mobix.editorpdf.domain.models.TableData
import com.mobix.editorpdf.ui.theme.GalaxyAccentPurple
import com.mobix.editorpdf.ui.theme.GalaxyAccentTeal
import com.mobix.editorpdf.ui.theme.GalaxyBackground
import com.mobix.editorpdf.ui.theme.GalaxySurface
import com.mobix.editorpdf.ui.theme.GalaxyTextPrimary
import com.mobix.editorpdf.ui.theme.GalaxyTextSecondary
import com.mobix.editorpdf.ui.viwmodel.DocumentViewModel
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate


fun handleSave(docId: Int, title: String, pageStates: List<RichTextState>, viewModel: DocumentViewModel) {
    val json = Json { ignoreUnknownKeys = true }
    val pagesHtml = pageStates.map { state -> try { state.toHtml() } catch (e: Exception) { "" } }
    val formattingJsonList = pageStates.map { state ->
        try {
            val data = FormattingData(
                spanStyles = state.annotatedString.spanStyles.map { style ->
                    SpanStyleData(
                        start = style.start, end = style.end,
                        fontWeight = if (style.item.fontWeight != null && style.item.fontWeight!!.weight >= 700) "Bold" else null,
                        fontStyle = if (style.item.fontStyle == FontStyle.Italic) "Italic" else null,
                        textDecoration = if (style.item.textDecoration == TextDecoration.Underline) "Underline" else null,
                        fontSize = if (style.item.fontSize != TextUnit.Unspecified) style.item.fontSize.value else null,
                        color = style.item.color.takeIf { it != Color.Unspecified }?.toArgb(),
                        background = style.item.background.takeIf { it != Color.Unspecified }?.toArgb()
                    )
                },
                paragraphStyles = state.annotatedString.paragraphStyles.map { style ->
                    ParagraphStyleData(start = style.start, end = style.end, textAlign = style.item.textAlign?.toString() ?: "Left")
                }
            )
            json.encodeToString(data)
        } catch (e: Exception) { "" }
    }
    val newDoc = Document(id = docId, title = title.ifBlank { "Untitled" }, pages = pagesHtml, formatting = formattingJsonList, lastUpdated = LocalDate.now().toString())
    if (docId == 0) viewModel.addDocument(newDoc) else viewModel.updateDocument(newDoc)
}

fun restoreFormatting(state: RichTextState, jsonString: String) {
    try {
        val json = Json { ignoreUnknownKeys = true }
        val data = json.decodeFromString<FormattingData>(jsonString)
        data.paragraphStyles.forEach { p ->
            val align = when (p.textAlign) {
                "Left", "TextAlign.Left" -> TextAlign.Left
                "Center", "TextAlign.Center" -> TextAlign.Center
                "Right", "TextAlign.Right" -> TextAlign.Right
                else -> TextAlign.Left
            }
            state.toggleParagraphStyle(androidx.compose.ui.text.ParagraphStyle(textAlign = align))
        }
        data.spanStyles.forEach { s ->
            val color = if (s.color != null) Color(s.color) else Color.Unspecified
            val background = if (s.background != null) Color(s.background) else Color.Unspecified
            val style = SpanStyle(
                fontWeight = if (s.fontWeight == "Bold") FontWeight.Bold else null,
                fontStyle = if (s.fontStyle == "Italic") FontStyle.Italic else null,
                textDecoration = if (s.textDecoration == "Underline") TextDecoration.Underline else null,
                fontSize = if (s.fontSize != null) s.fontSize.sp else TextUnit.Unspecified,
                color = color, background = background
            )
            state.addSpanStyle(style, TextRange(s.start, s.end))
        }
    } catch (_: Exception) { }
}

fun copyImageToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val fileName = "img_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)
        inputStream.close(); outputStream.close()
        file.absolutePath
    } catch (_: Exception) { null }
}

@Composable
fun ToolBtn(isActive: Boolean, icon: ImageVector, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.background(if (isActive) GalaxyAccentPurple.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(8.dp))) {
        Icon(imageVector = icon, contentDescription = null, tint = if (isActive) GalaxyAccentPurple else GalaxyTextPrimary)
    }
}

@Composable
fun ColorPickerRow(onColorSelected: (Color) -> Unit) {
    val colors = listOf(Color.Black, Color.Red, GalaxyAccentPurple, GalaxyAccentTeal, Color(0xFFFF9800), Color.Green, Color.Blue, Color.Gray)
    Row(modifier = Modifier.fillMaxWidth().background(GalaxySurface).padding(8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        colors.forEach { color ->
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(color).border(1.dp, Color.White, CircleShape).clickable { onColorSelected(color) })
        }
    }
}

@Composable
fun SizePickerRow(onSizeSelected: (Float) -> Unit) {
    val sizes = listOf(12f, 14f, 16f, 18f, 20f, 24f, 30f, 36f, 48f)
    Row(modifier = Modifier.fillMaxWidth().background(GalaxySurface).padding(8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        sizes.forEach { size ->
            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(GalaxyBackground).border(1.dp, GalaxyTextSecondary.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).clickable { onSizeSelected(size) }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("${size.toInt()}", fontSize = 14.sp, color = GalaxyTextPrimary)
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableEditorDialog(onDismiss: () -> Unit, onConfirm: (TableData) -> Unit) {
    var rowsCount by remember { mutableIntStateOf(2) }
    var colsCount by remember { mutableIntStateOf(2) }

    val cellStates = remember {
        mutableStateListOf<SnapshotStateList<String>>().apply {
            repeat(10) { add(mutableStateListOf<String>().apply { repeat(10) { add("") } }) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Table Editor", color = GalaxyTextPrimary) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Rows: $rowsCount", fontSize = 12.sp)
                        Row {
                            IconButton(onClick = { if (rowsCount < 8) rowsCount++ }) { Icon(Icons.Default.Add, "Add Row") }
                            IconButton(onClick = { if (rowsCount > 1) rowsCount-- }) { Icon(Icons.Default.Remove, "Remove Row") }
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Cols: $colsCount", fontSize = 12.sp)
                        Row {
                            IconButton(onClick = { if (colsCount < 5) colsCount++ }) { Icon(Icons.Default.Add, "Add Col") }
                            IconButton(onClick = { if (colsCount > 1) colsCount-- }) { Icon(Icons.Default.Remove, "Remove Col") }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                    repeat(rowsCount) { r ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            repeat(colsCount) { c ->
                                OutlinedTextField(
                                    value = cellStates[r][c],
                                    onValueChange = { cellStates[r][c] = it }, // الآن سيتم التحديث فوراً
                                    modifier = Modifier.weight(1f).padding(2.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = GalaxySurface, unfocusedContainerColor = GalaxySurface)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val finalCells = cellStates.take(rowsCount).map { it.take(colsCount).toList() }
                onConfirm(TableData(rowsCount, colsCount, finalCells))
            }) { Text("Insert Table") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = GalaxySurface
    )
}