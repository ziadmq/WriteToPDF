package com.example.writetopdf

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.writetopdf.domain.models.Document
import com.example.writetopdf.domain.models.FormattingData
import com.example.writetopdf.domain.models.ParagraphStyleData
import com.example.writetopdf.domain.models.SpanStyleData
import com.example.writetopdf.ui.theme.*
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    viewModel: DocumentViewModel,
    document: Document,
    navigateToHome: () -> Unit = {}
) {
    val context = LocalContext.current
    val richTextState = rememberRichTextState()
    val isSaveDialogOpen = remember { mutableStateOf(false) }
    val titleState = remember { mutableStateOf(document.title) }

    // Tools
    var showColorPicker by remember { mutableStateOf(false) }
    var showSizePicker by remember { mutableStateOf(false) }

    // Image Picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // FIXED: Use setHtml to append the image tag instead of addText
            val currentHtml = richTextState.toHtml()
            val imageTag = "<br>[IMAGE:${it.toString()}]<br>"
            richTextState.setHtml(currentHtml + imageTag)
        }
    }

    // Permissions
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        (LocalContext.current as? Activity)?.let {
            ActivityCompat.requestPermissions(it, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        }
    }

    val currentSpanStyle = richTextState.currentSpanStyle
    val currentParagraphStyle = richTextState.currentParagraphStyle
    val json = Json { allowSpecialFloatingPointValues = true; ignoreUnknownKeys = true; isLenient = true }

    // Restore Data
    LaunchedEffect(document) {
        richTextState.setText(document.content)
        document.formatting?.let { formattingJson ->
            try {
                val formattingData = json.decodeFromString<FormattingData>(formattingJson)
                formattingData.paragraphStyles.forEach { paragraphStyle ->
                    val relevantSpans = formattingData.spanStyles.filter { spanStyle ->
                        spanStyle.start < paragraphStyle.end && spanStyle.end > paragraphStyle.start
                    }.sortedBy { it.start }

                    if (relevantSpans.isNotEmpty()) {
                        relevantSpans.forEach { spanStyle ->
                            val color = spanStyle.color?.let { Color(it) } ?: Color.Unspecified
                            val style = SpanStyle(
                                fontWeight = if (spanStyle.fontWeight == "Bold") FontWeight.Bold else FontWeight.Normal,
                                fontStyle = if (spanStyle.fontStyle == "Italic") FontStyle.Italic else FontStyle.Normal,
                                textDecoration = if (spanStyle.textDecoration == "Underline") TextDecoration.Underline else TextDecoration.None,
                                fontSize = if (spanStyle.fontSize != null && !spanStyle.fontSize.isNaN()) spanStyle.fontSize.sp else TextUnit.Unspecified,
                                color = color
                            )
                            richTextState.addSpanStyle(style, TextRange(spanStyle.start, spanStyle.end))
                        }
                    }
                    val style = when (paragraphStyle.textAlign) {
                        "Left" -> TextAlign.Left
                        "Center" -> TextAlign.Center
                        "Right" -> TextAlign.Right
                        "Justify" -> TextAlign.Justify
                        else -> null
                    }?.let { ParagraphStyle(textAlign = it) }
                    if (style != null) richTextState.toggleParagraphStyle(style)
                }
            } catch (e: Exception) {
                Log.e("EditorScreen", "Error restoring: ${e.message}")
            }
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = GalaxyBackground,
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = titleState.value,
                        onValueChange = { titleState.value = it },
                        modifier = Modifier.height(50.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, color = GalaxyTextPrimary),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = GalaxyAccentTeal,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = GalaxyTextPrimary,
                            unfocusedTextColor = GalaxyTextPrimary
                        ),
                        placeholder = { Text("Untitled", color = GalaxyTextSecondary) }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        handleSave(document, titleState.value, richTextState, json, viewModel)
                        navigateToHome()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GalaxyTextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { isSaveDialogOpen.value = true }) {
                        Icon(Icons.Default.Check, contentDescription = "Export PDF", tint = GalaxyAccentTeal)
                    }
                    IconButton(onClick = {
                        handleSave(document, titleState.value, richTextState, json, viewModel)
                        Toast.makeText(context, "Saved to Galaxy", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = GalaxyAccentPurple)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GalaxyBackground)
            )
        },
        bottomBar = {
            Column(modifier = Modifier.background(GalaxySurface)) {
                if(showColorPicker) {
                    ColorPickerRow(onColorSelected = {
                        richTextState.toggleSpanStyle(SpanStyle(color = it))
                        showColorPicker = false
                    })
                }
                if(showSizePicker) {
                    SizePickerRow(onSizeSelected = {
                        richTextState.toggleSpanStyle(SpanStyle(fontSize = it.sp))
                        showSizePicker = false
                    })
                }
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Add Image Button
                    IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Icon(Icons.Default.Build, contentDescription = "Image", tint = GalaxyAccentTeal)
                    }

                    Divider(modifier = Modifier.height(24.dp).width(1.dp), color = GalaxyTextSecondary)

                    ToolBtn(isActive = currentSpanStyle.fontWeight == FontWeight.Bold, iconRes = R.drawable.bold_solid) {
                        richTextState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    }
                    ToolBtn(isActive = currentSpanStyle.fontStyle == FontStyle.Italic, iconRes = R.drawable.italic_solid) {
                        richTextState.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    }
                    ToolBtn(isActive = currentSpanStyle.textDecoration == TextDecoration.Underline, iconRes = R.drawable.underline_solid) {
                        richTextState.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                    }
                    Divider(modifier = Modifier.height(24.dp).width(1.dp), color = GalaxyTextSecondary)
                    IconButton(onClick = { showColorPicker = !showColorPicker; showSizePicker = false }) {
                        Icon(Icons.Default.Home, contentDescription = "Color", tint = if(showColorPicker) GalaxyAccentTeal else GalaxyTextPrimary)
                    }
                    IconButton(onClick = { showSizePicker = !showSizePicker; showColorPicker = false }) {
                        Icon(Icons.Default.Delete, contentDescription = "Size", tint = if(showSizePicker) GalaxyAccentTeal else GalaxyTextPrimary)
                    }
                    Divider(modifier = Modifier.height(24.dp).width(1.dp), color = GalaxyTextSecondary)
                    ToolBtn(isActive = currentParagraphStyle.textAlign == TextAlign.Left, iconRes = R.drawable.align_left_solid) {
                        richTextState.toggleParagraphStyle(ParagraphStyle(textAlign = TextAlign.Left))
                    }
                    ToolBtn(isActive = currentParagraphStyle.textAlign == TextAlign.Center, iconRes = R.drawable.align_center_solid) {
                        richTextState.toggleParagraphStyle(ParagraphStyle(textAlign = TextAlign.Center))
                    }
                    ToolBtn(isActive = currentParagraphStyle.textAlign == TextAlign.Right, iconRes = R.drawable.align_right_solid) {
                        richTextState.toggleParagraphStyle(ParagraphStyle(textAlign = TextAlign.Right))
                    }
                }
            }
        }
    ) { paddingValues ->
        if (isSaveDialogOpen.value) {
            AlertDialog(
                containerColor = GalaxySurface,
                titleContentColor = GalaxyTextPrimary,
                textContentColor = GalaxyTextSecondary,
                onDismissRequest = { isSaveDialogOpen.value = false },
                title = { Text("Export to PDF") },
                text = { Text("Save as ${titleState.value}.pdf?") },
                confirmButton = {
                    Button(
                        onClick = {
                            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) true else {
                                ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                            }
                            if (hasPermission) {
                                exportToPdf(context, titleState.value, richTextState)
                            } else {
                                Toast.makeText(context, "Permission Required", Toast.LENGTH_SHORT).show()
                            }
                            isSaveDialogOpen.value = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GalaxyAccentTeal)
                    ) { Text("Export") }
                },
                dismissButton = { TextButton(onClick = { isSaveDialogOpen.value = false }) { Text("Cancel", color = GalaxyTextSecondary) } }
            )
        }

        Box(
            modifier = Modifier.padding(paddingValues).fillMaxSize().background(GalaxyBackground),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                modifier = Modifier.padding(16.dp).fillMaxWidth().aspectRatio(0.707f).shadow(16.dp).verticalScroll(rememberScrollState()),
                color = Color.White,
                shape = RoundedCornerShape(4.dp)
            ) {
                RichTextEditor(state = richTextState, modifier = Modifier.fillMaxSize().padding(32.dp), placeholder = { Text("Start creating...", color = Color.Gray) })
            }
        }
    }
}

fun handleSave(originalDoc: Document, title: String, richTextState: RichTextState, json: Json, viewModel: DocumentViewModel) {
    val formattingData = FormattingData(
        spanStyles = richTextState.annotatedString.spanStyles.map { style ->
            SpanStyleData(
                start = style.start,
                end = style.end,
                fontWeight = if (style.item.fontWeight == FontWeight.Bold) "Bold" else null,
                fontStyle = if (style.item.fontStyle == FontStyle.Italic) "Italic" else null,
                textDecoration = if (style.item.textDecoration == TextDecoration.Underline) "Underline" else null,
                fontSize = style.item.fontSize?.value,
                color = style.item.color.takeIf { it != Color.Unspecified }?.toArgb()
            )
        },
        paragraphStyles = richTextState.annotatedString.paragraphStyles.map { style ->
            ParagraphStyleData(start = style.start, end = style.end, textAlign = when (style.item.textAlign) { TextAlign.Left -> "Left"; TextAlign.Center -> "Center"; TextAlign.Right -> "Right"; TextAlign.Justify -> "Justify"; else -> null })
        }
    )

    val newDoc = originalDoc.copy(
        title = title,
        content = richTextState.toText(),
        formatting = json.encodeToString(formattingData),
        lastUpdated = java.time.LocalDate.now().toString()
    )

    if (originalDoc.id == 0) {
        viewModel.addDocument(newDoc)
    } else {
        viewModel.updateDocument(newDoc)
    }
}

@Composable
fun ToolBtn(isActive: Boolean, iconRes: Int, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.background(if (isActive) GalaxyAccentPurple.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(8.dp)).border(1.dp, if(isActive) GalaxyAccentPurple else Color.Transparent, RoundedCornerShape(8.dp))
    ) {
        Icon(painter = painterResource(id = iconRes), contentDescription = null, modifier = Modifier.size(20.dp), tint = if(isActive) GalaxyAccentPurple else GalaxyTextPrimary)
    }
}

@Composable
fun ColorPickerRow(onColorSelected: (Color) -> Unit) {
    val colors = listOf(Color.Black, Color.Red, GalaxyAccentPurple, GalaxyAccentTeal, Color(0xFFFF9800), Color.Green)
    Row(modifier = Modifier.fillMaxWidth().background(GalaxySurface).padding(8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        colors.forEach { color ->
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(color).border(1.dp, Color.White, CircleShape).clickable { onColorSelected(color) })
        }
    }
}

@Composable
fun SizePickerRow(onSizeSelected: (Float) -> Unit) {
    val sizes = listOf(12f, 14f, 16f, 18f, 20f, 24f, 30f)
    Row(modifier = Modifier.fillMaxWidth().background(GalaxySurface).padding(8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        sizes.forEach { size ->
            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(GalaxyBackground).border(1.dp, GalaxyTextSecondary.copy(alpha=0.3f), RoundedCornerShape(8.dp)).clickable { onSizeSelected(size) }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("${size.toInt()}", fontSize = 14.sp, color = GalaxyTextPrimary)
            }
        }
    }
}

fun exportToPdf(context: Context, fileName: String, richTextState: RichTextState) {
    try {
        val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (!docsDir.exists()) docsDir.mkdirs()
        val pdfFile = File(docsDir, "$fileName.pdf")

        val pdfWriter = PdfWriter(pdfFile)
        val pdfDocument = PdfDocument(pdfWriter)
        val document = com.itextpdf.layout.Document(pdfDocument)

        val annotatedString = richTextState.annotatedString
        val fullText = annotatedString.text

        // Split text by image tag
        val parts = fullText.split("(?=\\[IMAGE:)".toRegex())

        parts.forEach { part ->
            if (part.startsWith("[IMAGE:")) {
                val endIndex = part.indexOf("]")
                if (endIndex != -1) {
                    val uriString = part.substring(7, endIndex)
                    try {
                        val uri = Uri.parse(uriString)
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val bytes = inputStream?.readBytes()
                        inputStream?.close()

                        if (bytes != null) {
                            val imageData = ImageDataFactory.create(bytes)
                            val pdfImage = com.itextpdf.layout.element.Image(imageData)
                            pdfImage.setAutoScale(true)
                            document.add(pdfImage)
                        }

                        if (endIndex + 1 < part.length) {
                            document.add(com.itextpdf.layout.element.Paragraph(part.substring(endIndex + 1)))
                        }
                    } catch (e: Exception) {
                        document.add(com.itextpdf.layout.element.Paragraph("[Error loading image]"))
                    }
                }
            } else {
                if (part.isNotEmpty()) {
                    document.add(com.itextpdf.layout.element.Paragraph(part))
                }
            }
        }

        Toast.makeText(context, "Saved to Documents/$fileName.pdf", Toast.LENGTH_LONG).show()
        document.close()
    } catch (e: Exception) {
        e.printStackTrace()
        Log.e("PDFError", "Error: ${e.message}")
        Toast.makeText(context, "Export Failed", Toast.LENGTH_SHORT).show()
    }
}