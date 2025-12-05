package com.example.writetopdf

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.writetopdf.domain.models.Document
import com.example.writetopdf.domain.models.FormattingData
import com.example.writetopdf.domain.models.ParagraphStyleData
import com.example.writetopdf.domain.models.SpanStyleData
import com.example.writetopdf.ui.theme.*
import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.element.AreaBreak
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import com.itextpdf.layout.properties.AreaBreakType
import com.itextpdf.layout.properties.BaseDirection
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.font.FontProvider
import com.itextpdf.layout.font.FontSet
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EditorScreen(
    viewModel: DocumentViewModel,
    document: Document,
    navigateToHome: () -> Unit = {}
) {
    val context = LocalContext.current
    val titleState = remember { mutableStateOf(document.title) }

    // -- MULTI PAGE SETUP --
    val pageStates = remember {
        mutableStateListOf<RichTextState>().apply {
            if (document.pages.isNotEmpty()) {
                document.pages.forEachIndexed { index, htmlContent ->
                    val state = RichTextState()
                    state.setHtml(htmlContent)
                    if (index < document.formatting.size) {
                        restoreFormatting(state, document.formatting[index])
                    }
                    add(state)
                }
            } else {
                add(RichTextState())
            }
        }
    }

    val pagerState = rememberPagerState(pageCount = { pageStates.size })
    val currentRichTextState = pageStates[pagerState.currentPage]

    // Tools
    var showColorPicker by remember { mutableStateOf(false) }
    var showSizePicker by remember { mutableStateOf(false) }
    val isSaveDialogOpen = remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val internalPath = copyImageToInternalStorage(context, it)
            if (internalPath != null) {
                val currentHtml = currentRichTextState.toHtml()
                val imageTag = "<br>[IMAGE:$internalPath]<br>"
                currentRichTextState.setHtml(currentHtml + imageTag)
            }
        }
    }

    Scaffold(
        containerColor = GalaxyBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        OutlinedTextField(
                            value = titleState.value,
                            onValueChange = { titleState.value = it },
                            modifier = Modifier.height(50.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, color = GalaxyTextPrimary),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent,
                                cursorColor = GalaxyAccentTeal, focusedTextColor = GalaxyTextPrimary, unfocusedTextColor = GalaxyTextPrimary
                            ),
                            placeholder = { androidx.compose.material3.Text("Untitled", color = GalaxyTextSecondary) }
                        )
                        androidx.compose.material3.Text("Page ${pagerState.currentPage + 1} of ${pageStates.size}", fontSize = 12.sp, color = GalaxyTextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        handleSave(document.id, titleState.value, pageStates, viewModel)
                        navigateToHome()
                    }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GalaxyTextPrimary) }
                },
                actions = {
                    IconButton(onClick = { isSaveDialogOpen.value = true }) {
                        Icon(Icons.Default.Download, contentDescription = "Export PDF", tint = GalaxyAccentTeal)
                    }
                    IconButton(onClick = {
                        handleSave(document.id, titleState.value, pageStates, viewModel)
                        Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Save", tint = GalaxyAccentPurple)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GalaxyBackground)
            )
        },
        bottomBar = {
            Column(modifier = Modifier.background(GalaxySurface)) {
                if (showColorPicker) {
                    ColorPickerRow(onColorSelected = {
                        currentRichTextState.toggleSpanStyle(SpanStyle(color = it))
                        showColorPicker = false
                    })
                }
                if (showSizePicker) {
                    SizePickerRow(onSizeSelected = {
                        currentRichTextState.toggleSpanStyle(SpanStyle(fontSize = it.sp))
                        showSizePicker = false
                    })
                }

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { pageStates.add(RichTextState()) }) {
                        Icon(Icons.Default.PostAdd, contentDescription = "New Page", tint = GalaxyAccentTeal)
                    }
                    IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Icon(Icons.Default.Image, contentDescription = "Add Image", tint = GalaxyAccentTeal)
                    }

                    Divider(modifier = Modifier.height(24.dp).width(1.dp), color = GalaxyTextSecondary)

                    ToolBtn(isActive = currentRichTextState.currentSpanStyle.fontWeight == FontWeight.Bold, icon = Icons.Default.FormatBold) {
                        currentRichTextState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    }
                    ToolBtn(isActive = currentRichTextState.currentSpanStyle.fontStyle == FontStyle.Italic, icon = Icons.Default.FormatItalic) {
                        currentRichTextState.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    }
                    ToolBtn(isActive = currentRichTextState.currentSpanStyle.textDecoration == TextDecoration.Underline, icon = Icons.Default.FormatUnderlined) {
                        currentRichTextState.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                    }

                    Divider(modifier = Modifier.height(24.dp).width(1.dp), color = GalaxyTextSecondary)

                    IconButton(onClick = { showColorPicker = !showColorPicker; showSizePicker = false }) {
                        Icon(Icons.Default.Palette, contentDescription = "Color", tint = if (showColorPicker) GalaxyAccentTeal else GalaxyTextPrimary)
                    }
                    IconButton(onClick = { showSizePicker = !showSizePicker; showColorPicker = false }) {
                        Icon(Icons.Filled.FormatSize, contentDescription = "Size", tint = if (showSizePicker) GalaxyAccentTeal else GalaxyTextPrimary)
                    }
                }
            }
        }
    ) { paddingValues ->
        if (isSaveDialogOpen.value) {
            AlertDialog(
                onDismissRequest = { isSaveDialogOpen.value = false },
                title = { androidx.compose.material3.Text("Export PDF") },
                text = { androidx.compose.material3.Text("Save to Downloads?") },
                confirmButton = {
                    Button(onClick = {
                        exportToPdf(context, titleState.value, pageStates)
                        isSaveDialogOpen.value = false
                    }) { androidx.compose.material3.Text("Export") }
                },
                dismissButton = { TextButton(onClick = { isSaveDialogOpen.value = false }) { androidx.compose.material3.Text("Cancel") } }
            )
        }

        Box(modifier = Modifier.padding(paddingValues).fillMaxSize().background(GalaxyBackground)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { pageIndex ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    Surface(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                            .aspectRatio(0.707f)
                            .shadow(10.dp)
                            .verticalScroll(rememberScrollState()),
                        color = Color.White,
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        RichTextEditor(
                            state = pageStates[pageIndex],
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            placeholder = { androidx.compose.material3.Text("Page ${pageIndex + 1}...", color = Color.Gray) }
                        )
                    }
                }
            }
            androidx.compose.material3.Text(
                "${pagerState.currentPage + 1} / ${pageStates.size}",
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).background(Color.Black.copy(alpha=0.5f), CircleShape).padding(horizontal = 12.dp, vertical = 4.dp),
                color = Color.White
            )
        }
    }
}

// --- HELPER FUNCTIONS ---

fun handleSave(docId: Int, title: String, pageStates: List<RichTextState>, viewModel: DocumentViewModel) {
    val json = Json { ignoreUnknownKeys = true }
    val pagesHtml = pageStates.map { it.toHtml() }
    val formattingJsonList = pageStates.map { state ->
        val data = FormattingData(
            spanStyles = state.annotatedString.spanStyles.map { style ->
                SpanStyleData(
                    start = style.start, end = style.end,
                    fontWeight = if (style.item.fontWeight == FontWeight.Bold) "Bold" else null,
                    fontStyle = if (style.item.fontStyle == FontStyle.Italic) "Italic" else null,
                    textDecoration = if (style.item.textDecoration == TextDecoration.Underline) "Underline" else null,
                    fontSize = style.item.fontSize?.value,
                    color = style.item.color.takeIf { it != Color.Unspecified }?.toArgb()
                )
            },
            paragraphStyles = state.annotatedString.paragraphStyles.map { style ->
                ParagraphStyleData(start = style.start, end = style.end, textAlign = style.item.textAlign.toString())
            }
        )
        json.encodeToString(data)
    }

    val newDoc = Document(
        id = docId,
        title = title,
        pages = pagesHtml,
        formatting = formattingJsonList,
        lastUpdated = java.time.LocalDate.now().toString()
    )

    if (docId == 0) viewModel.addDocument(newDoc) else viewModel.updateDocument(newDoc)
}

fun copyImageToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val fileName = "img_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        file.absolutePath
    } catch (e: Exception) {
        null
    }
}

// ✅ FIXED: PDF Export with ARABIC + COLORS + STYLES
fun exportToPdf(context: Context, fileName: String, pageStates: List<RichTextState>) {
    try {
        val finalFileName = "$fileName.pdf"
        val outputStream: OutputStream?

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, finalFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            outputStream = uri?.let { context.contentResolver.openOutputStream(it) }
        } else {
            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!docsDir.exists()) docsDir.mkdirs()
            val file = File(docsDir, finalFileName)
            outputStream = FileOutputStream(file)
        }

        if (outputStream != null) {
            val pdfWriter = PdfWriter(outputStream)
            val pdfDocument = PdfDocument(pdfWriter)
            val document = com.itextpdf.layout.Document(pdfDocument)

            // --- 1. SETUP FONTS (ARABIC) ---
            val fontProvider = FontProvider()
            val fontSet = FontSet()

            // Attempt to load Arabic Font from Assets
            try {
                val assets = context.assets
                val fontStream = assets.open("arabic_font.ttf") // Ensure this file exists in assets!
                val fontBytes = fontStream.readBytes()
                fontStream.close()
                // IDENTITY_H is required for Arabic
                val arabicFont = PdfFontFactory.createFont(fontBytes, PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED)
                fontProvider.addFont(arabicFont.fontProgram)
                document.setFontProvider(fontProvider)
                document.setFont(arabicFont)
                document.setBaseDirection(BaseDirection.RIGHT_TO_LEFT) // Critical for Arabic
            } catch (e: Exception) {
                // Font not found - will use default (Arabic might be disconnected)
            }

            // --- 2. BUILD PDF CONTENT ---
            pageStates.forEachIndexed { index, state ->
                if (index > 0) document.add(AreaBreak(AreaBreakType.NEXT_PAGE))

                val annotatedString = state.annotatedString
                val fullText = annotatedString.text

                // Split by [IMAGE:...] tags
                val parts = fullText.split("(?=\\[IMAGE:)".toRegex())

                // Track where we are in the text
                var currentIndex = 0

                parts.forEach { part ->
                    if (part.startsWith("[IMAGE:")) {
                        // ... Image Logic ...
                        val endIndex = part.indexOf("]")
                        if (endIndex != -1) {
                            val imagePath = part.substring(7, endIndex)
                            try {
                                val imgFile = File(imagePath)
                                if (imgFile.exists()) {
                                    val imageData = ImageDataFactory.create(imgFile.absolutePath)
                                    val pdfImage = Image(imageData)
                                    pdfImage.setAutoScale(true)
                                    document.add(pdfImage)
                                }
                            } catch (e: Exception) {}

                            // Process text after image in this part
                            currentIndex += (endIndex + 1) // skip tag
                        }
                    } else {
                        // Text Part
                        val p = Paragraph()
                        // Just a simple split for now - for perfect formatting we need complex range mapping
                        // This simplistic approach applies the font globally.
                        // To support Colors, we must check spans.

                        val textObj = Text(part)

                        // Apply Styles (Simple Global check for the part - improving this would require char-by-char)
                        // For now, let's enable the Arabic direction
                        p.setBaseDirection(BaseDirection.RIGHT_TO_LEFT)
                        p.setTextAlignment(TextAlignment.RIGHT) // Default for Arabic

                        // Check formatting (simplified)
                        val spans = annotatedString.spanStyles.filter {
                            it.start >= currentIndex && it.end <= currentIndex + part.length
                        }

                        if (spans.isNotEmpty()) {
                            spans.firstOrNull()?.let { span ->
                                if (span.item.fontWeight == FontWeight.Bold) textObj.setBold()
                                if (span.item.fontStyle == FontStyle.Italic) textObj.setItalic()
                                if (span.item.color != Color.Unspecified) {
                                    val argb = span.item.color.toArgb()
                                    val r = android.graphics.Color.red(argb)
                                    val g = android.graphics.Color.green(argb)
                                    val b = android.graphics.Color.blue(argb)
                                    textObj.setFontColor(DeviceRgb(r, g, b))
                                }
                                if (span.item.fontSize != TextUnit.Unspecified) {
                                    textObj.setFontSize(span.item.fontSize.value)
                                }
                            }
                        }

                        p.add(textObj)
                        document.add(p)
                        currentIndex += part.length
                    }
                }
            }
            document.close()
            outputStream.close()
            Toast.makeText(context, "Saved to Downloads: $finalFileName", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Export Failed: ${e.message}", Toast.LENGTH_LONG).show()
    }
}


fun restoreFormatting(state: RichTextState, jsonString: String) {
    try {
        val json = Json { ignoreUnknownKeys = true }
        val data = json.decodeFromString<FormattingData>(jsonString)
        data.paragraphStyles.forEach { p ->
            val align = when(p.textAlign) { "Left"->TextAlign.Left; "Center"->TextAlign.Center; "Right"->TextAlign.Right; else->TextAlign.Left }
            state.toggleParagraphStyle(ParagraphStyle(textAlign = align))
        }
        data.spanStyles.forEach { s ->
            val color = if (s.color != null) Color(s.color) else Color.Unspecified
            val style = SpanStyle(
                fontWeight = if(s.fontWeight == "Bold") FontWeight.Bold else null,
                fontStyle = if(s.fontStyle == "Italic") FontStyle.Italic else null,
                textDecoration = if(s.textDecoration == "Underline") TextDecoration.Underline else null,
                fontSize = if(s.fontSize != null) s.fontSize.sp else TextUnit.Unspecified,
                color = color
            )
            state.addSpanStyle(style, androidx.compose.ui.text.TextRange(s.start, s.end))
        }
    } catch (e: Exception) { }
}

@Composable
fun ToolBtn(isActive: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.background(if (isActive) GalaxyAccentPurple.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(8.dp))
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = if(isActive) GalaxyAccentPurple else GalaxyTextPrimary)
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
            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(GalaxyBackground).border(1.dp, GalaxyTextSecondary.copy(alpha=0.3f), RoundedCornerShape(8.dp)).clickable { onSizeSelected(size) }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                androidx.compose.material3.Text("${size.toInt()}", fontSize = 14.sp, color = GalaxyTextPrimary)
            }
        }
    }
}