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
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.element.AreaBreak
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.properties.AreaBreakType
import com.itextpdf.layout.properties.TextAlignment
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

    // ---- PAGES STATE ----
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
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 18.sp,
                                color = GalaxyTextPrimary
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = GalaxyAccentTeal,
                                focusedTextColor = GalaxyTextPrimary,
                                unfocusedTextColor = GalaxyTextPrimary
                            ),
                            placeholder = {
                                Text(
                                    "Untitled",
                                    color = GalaxyTextSecondary
                                )
                            }
                        )
                        Text(
                            "Page ${pagerState.currentPage + 1} of ${pageStates.size}",
                            fontSize = 12.sp,
                            color = GalaxyTextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        handleSave(document.id, titleState.value, pageStates, viewModel)
                        navigateToHome()
                    }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = GalaxyTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { isSaveDialogOpen.value = true }) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Export PDF",
                            tint = GalaxyAccentTeal
                        )
                    }
                    IconButton(onClick = {
                        handleSave(document.id, titleState.value, pageStates, viewModel)
                        Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = "Save",
                            tint = GalaxyAccentPurple
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GalaxyBackground
                )
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { pageStates.add(RichTextState()) }) {
                        Icon(
                            Icons.Default.PostAdd,
                            contentDescription = "New Page",
                            tint = GalaxyAccentTeal
                        )
                    }
                    IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "Add Image",
                            tint = GalaxyAccentTeal
                        )
                    }

                    Divider(
                        modifier = Modifier
                            .height(24.dp)
                            .width(1.dp),
                        color = GalaxyTextSecondary
                    )

                    ToolBtn(
                        isActive = currentRichTextState.currentSpanStyle.fontWeight == FontWeight.Bold,
                        icon = Icons.Default.FormatBold
                    ) {
                        currentRichTextState.toggleSpanStyle(
                            SpanStyle(fontWeight = FontWeight.Bold)
                        )
                    }
                    ToolBtn(
                        isActive = currentRichTextState.currentSpanStyle.fontStyle == FontStyle.Italic,
                        icon = Icons.Default.FormatItalic
                    ) {
                        currentRichTextState.toggleSpanStyle(
                            SpanStyle(fontStyle = FontStyle.Italic)
                        )
                    }
                    ToolBtn(
                        isActive = currentRichTextState.currentSpanStyle.textDecoration == TextDecoration.Underline,
                        icon = Icons.Default.FormatUnderlined
                    ) {
                        currentRichTextState.toggleSpanStyle(
                            SpanStyle(textDecoration = TextDecoration.Underline)
                        )
                    }

                    Divider(
                        modifier = Modifier
                            .height(24.dp)
                            .width(1.dp),
                        color = GalaxyTextSecondary
                    )

                    IconButton(onClick = {
                        showColorPicker = !showColorPicker
                        showSizePicker = false
                    }) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = "Color",
                            tint = if (showColorPicker) GalaxyAccentTeal else GalaxyTextPrimary
                        )
                    }
                    IconButton(onClick = {
                        showSizePicker = !showSizePicker
                        showColorPicker = false
                    }) {
                        Icon(
                            Icons.Filled.FormatSize,
                            contentDescription = "Size",
                            tint = if (showSizePicker) GalaxyAccentTeal else GalaxyTextPrimary
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        if (isSaveDialogOpen.value) {
            AlertDialog(
                onDismissRequest = { isSaveDialogOpen.value = false },
                title = { Text("Export PDF") },
                text = { Text("Save to Downloads?") },
                confirmButton = {
                    Button(onClick = {
                        exportToPdf(context, titleState.value, pageStates)
                        isSaveDialogOpen.value = false
                    }) { Text("Export") }
                },
                dismissButton = {
                    TextButton(onClick = { isSaveDialogOpen.value = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(GalaxyBackground)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
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
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            placeholder = {
                                Text(
                                    "Page ${pageIndex + 1}...",
                                    color = Color.Gray
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

// ---------- SAVE DOCUMENT (مع حماية من الكراش) ----------

fun handleSave(
    docId: Int,
    title: String,
    pageStates: List<RichTextState>,
    viewModel: DocumentViewModel
) {
    val json = Json { ignoreUnknownKeys = true }

    val pagesHtml = pageStates.map { state ->
        try {
            state.toHtml()
        } catch (e: Exception) {
            ""
        }
    }

    // لو صار أي مشاكل في الفورماتينغ ما نخلي التطبيق يكرش
    val formattingJsonList = pageStates.map { state ->
        try {
            val data = FormattingData(
                spanStyles = state.annotatedString.spanStyles.map { style ->
                    SpanStyleData(
                        start = style.start,
                        end = style.end,
                        fontWeight = if (style.item.fontWeight == FontWeight.Bold) "Bold" else null,
                        fontStyle = if (style.item.fontStyle == FontStyle.Italic) "Italic" else null,
                        textDecoration = if (style.item.textDecoration == TextDecoration.Underline) "Underline" else null,
                        fontSize = if (style.item.fontSize != TextUnit.Unspecified)
                            style.item.fontSize.value
                        else null,
                        color = style.item.color
                            .takeIf { it != Color.Unspecified }
                            ?.toArgb()
                    )
                },
                paragraphStyles = state.annotatedString.paragraphStyles.map { style ->
                    ParagraphStyleData(
                        start = style.start,
                        end = style.end,
                        textAlign = style.item.textAlign?.toString() ?: "Left"
                    )
                }
            )
            json.encodeToString(data)
        } catch (e: Exception) {
            ""
        }
    }

    val newDoc = Document(
        id = docId,
        title = title.ifBlank { "Untitled" },
        pages = pagesHtml,
        formatting = formattingJsonList,
        lastUpdated = java.time.LocalDate.now().toString()
    )

    if (docId == 0) {
        viewModel.addDocument(newDoc)
    } else {
        viewModel.updateDocument(newDoc)
    }
}

// ---------- COPY IMAGE TO INTERNAL STORAGE ----------

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
    } catch (_: Exception) {
        null
    }
}

fun exportToPdf(context: Context, fileName: String, pageStates: List<RichTextState>) {
    try {
        val finalName = if (fileName.isBlank()) "Document" else fileName
        val outputName = "$finalName.pdf"

        val outputStream: OutputStream? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val cv = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, outputName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Files.getContentUri("external"),
                    cv
                )
                uri?.let { context.contentResolver.openOutputStream(it) }
            } else {
                val file = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    outputName
                )
                FileOutputStream(file)
            }

        if (outputStream == null) {
            Toast.makeText(context, "Failed to open file", Toast.LENGTH_LONG).show()
            return
        }

        val writer = PdfWriter(outputStream)
        val pdf = PdfDocument(writer)
        val doc = com.itextpdf.layout.Document(pdf)

        // ---------- FOR EACH PAGE ----------
        pageStates.forEachIndexed { pageIndex, state ->

            if (pageIndex > 0) doc.add(AreaBreak(AreaBreakType.NEXT_PAGE))

            val annotated = state.annotatedString
            val fullText = annotated.text
            val spans = annotated.spanStyles

            // نستخدم نفس نظام [IMAGE:path] اللي انت عامله
            val imageTagRegex = "\\[IMAGE:([^]]+)]".toRegex()
            val parts = imageTagRegex.split(fullText)
            val images = imageTagRegex.findAll(fullText).toList()

            var pointer = 0 // مؤشر داخل النص الأصلي (مع الصور)

            parts.forEachIndexed { index, part ->

                // ----- نص عادي -----
                if (part.isNotEmpty()) {
                    val paragraph = Paragraph()

                    var i = 0
                    while (i < part.length) {
                        val ch = part[i]
                        val globalIndex = pointer + i

                        // كل الـ spans اللي فوق هذا الحرف
                        val overlappingSpans = spans.filter { span ->
                            globalIndex >= span.start && globalIndex < span.end
                        }

                        val textObj = com.itextpdf.layout.element.Text(ch.toString())

                        if (overlappingSpans.isNotEmpty()) {
                            // Bold / Italic / Underline
                            if (overlappingSpans.any { it.item.fontWeight == FontWeight.Bold }) {
                                textObj.setBold()
                            }
                            if (overlappingSpans.any { it.item.fontStyle == FontStyle.Italic }) {
                                textObj.setItalic()
                            }
                            if (overlappingSpans.any {
                                    it.item.textDecoration == TextDecoration.Underline
                                }) {
                                textObj.setUnderline()
                            }

                            // Font size (آخر واحد)
                            val sizeSpan = overlappingSpans.lastOrNull {
                                it.item.fontSize != TextUnit.Unspecified
                            }
                            sizeSpan?.let {
                                textObj.setFontSize(it.item.fontSize.value)
                            }

                            // Color (آخر span فيه لون محدد)
                            val colorSpan = overlappingSpans.lastOrNull {
                                it.item.color != Color.Unspecified
                            }
                            colorSpan?.let {
                                val c = it.item.color.toArgb()
                                textObj.setFontColor(
                                    DeviceRgb(
                                        android.graphics.Color.red(c),
                                        android.graphics.Color.green(c),
                                        android.graphics.Color.blue(c)
                                    )
                                )
                            }
                        }

                        paragraph.add(textObj)
                        i++
                    }

                    paragraph.setTextAlignment(TextAlignment.LEFT)
                    doc.add(paragraph)
                }

                // ----- صورة لو فيه -----
                if (index < images.size) {
                    val match = images[index]
                    val imgPath = match.groupValues[1]

                    try {
                        val file = File(imgPath)
                        if (file.exists()) {
                            val imgData = ImageDataFactory.create(file.absolutePath)
                            val pdfImg = Image(imgData)
                            pdfImg.setAutoScale(true)
                            doc.add(pdfImg)
                        }
                    } catch (_: Exception) {
                    }

                    // نعدي على النص + وسُم الصورة الأصلي
                    pointer += part.length + match.value.length
                } else {
                    // آخر جزء بدون صورة بعده
                    pointer += part.length
                }
            }
        }

        doc.close()
        outputStream.close()

        Toast.makeText(context, "Saved: $outputName", Toast.LENGTH_LONG).show()

    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
    }
}



// ---------- RESTORE FORMAT ----------

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
            state.toggleParagraphStyle(ParagraphStyle(textAlign = align))
        }

        data.spanStyles.forEach { s ->
            val color = if (s.color != null) Color(s.color) else Color.Unspecified
            val style = SpanStyle(
                fontWeight = if (s.fontWeight == "Bold") FontWeight.Bold else null,
                fontStyle = if (s.fontStyle == "Italic") FontStyle.Italic else null,
                textDecoration = if (s.textDecoration == "Underline") TextDecoration.Underline else null,
                fontSize = if (s.fontSize != null) s.fontSize.sp else TextUnit.Unspecified,
                color = color
            )
            state.addSpanStyle(
                style,
                androidx.compose.ui.text.TextRange(s.start, s.end)
            )
        }
    } catch (_: Exception) {
        // تجاهل لو في فورمات قديم / خربان
    }
}

// ---------- UI HELPERS ----------

@Composable
fun ToolBtn(
    isActive: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.background(
            if (isActive) GalaxyAccentPurple.copy(alpha = 0.3f) else Color.Transparent,
            RoundedCornerShape(8.dp)
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) GalaxyAccentPurple else GalaxyTextPrimary
        )
    }
}

@Composable
fun ColorPickerRow(onColorSelected: (Color) -> Unit) {
    val colors = listOf(
        Color.Black,
        Color.Red,
        GalaxyAccentPurple,
        GalaxyAccentTeal,
        Color(0xFFFF9800),
        Color.Green,
        Color.Blue,
        Color.Gray
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GalaxySurface)
            .padding(8.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, Color.White, CircleShape)
                    .clickable { onColorSelected(color) }
            )
        }
    }
}

@Composable
fun SizePickerRow(onSizeSelected: (Float) -> Unit) {
    val sizes = listOf(12f, 14f, 16f, 18f, 20f, 24f, 30f, 36f, 48f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GalaxySurface)
            .padding(8.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        sizes.forEach { size ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(GalaxyBackground)
                    .border(
                        1.dp,
                        GalaxyTextSecondary.copy(alpha = 0.3f),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onSizeSelected(size) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    "${size.toInt()}",
                    fontSize = 14.sp,
                    color = GalaxyTextPrimary
                )
            }
        }
    }
}
