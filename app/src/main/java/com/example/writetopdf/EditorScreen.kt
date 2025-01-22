package com.example.documenteditor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.documenteditor.domain.models.Document
import com.example.documenteditor.domain.models.FormattingData
import com.example.documenteditor.domain.models.ParagraphStyleData
import com.example.documenteditor.domain.models.SpanStyleData
import com.google.accompanist.flowlayout.FlowRow
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bouncycastle.math.raw.Mod
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    viewModel: DocumentViewModel,
    document: Document,
    navigateToHome: () -> Unit = {}
){
    val context = LocalContext.current

    val richTextState = rememberRichTextState()

    val isDialogOpen = remember {
        mutableStateOf(false)
    }

    val titleState = remember {
        mutableStateOf("")
    }

    val sizeDropDownExpanded = remember {
        mutableStateOf(false)
    }

    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED){
        (LocalContext.current as? Activity)?.let {
            ActivityCompat.requestPermissions(
                it,
                arrayOf(
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                1
            )
        }
    }

    val fontSizes = listOf(12.sp, 16.sp, 20.sp, 24.sp, 28.sp) // Define font sizes
    val selectedFontSize = remember { mutableStateOf(fontSizes.first()) } // Keep track of selected font size

    val currentSpanStyle = richTextState.currentSpanStyle
    val currentParagraphStyle = richTextState.currentParagraphStyle
    val isBold = currentSpanStyle.fontWeight == FontWeight.Bold
    val isItalic = currentSpanStyle.fontStyle == FontStyle.Italic
//    val isUnderlineActive = currentSpanStyle.textDecoration == TextDecoration.Underline
    val isLeftAlignActive = currentParagraphStyle.textAlign == TextAlign.Left
    val isCenterAlignActive = currentParagraphStyle.textAlign == TextAlign.Center
    val isRightAlignActive = currentParagraphStyle.textAlign == TextAlign.Right
    val isJustifyAlignActive = currentParagraphStyle.textAlign == TextAlign.Justify

    val json = Json {
        allowSpecialFloatingPointValues = true  // This allows NaN values
        ignoreUnknownKeys = true  // This helps with backwards compatibility
        isLenient = true  // Makes the parser more forgiving
    }

    LaunchedEffect(document) {
        richTextState.setText(document.content)

        // Restore formatting if available
        document.formatting?.let { formattingJson ->
            try {
                val formattingData = json.decodeFromString<FormattingData>(formattingJson)
                formattingData.paragraphStyles.forEach { paragraphStyle ->

                    // Find all span styles that intersect with this paragraph
                    val relevantSpans = formattingData.spanStyles.filter { spanStyle ->
                        spanStyle.start < paragraphStyle.end && spanStyle.end > paragraphStyle.start
                    }.sortedBy { it.start }

                    if (relevantSpans.isNotEmpty()) {
                        // Apply span styles
                        relevantSpans.forEach { spanStyle ->
                            val style = SpanStyle(
                                fontWeight = when (spanStyle.fontWeight) {
                                    "Bold" -> FontWeight.Bold
                                    else -> FontWeight.Normal
                                },
                                fontStyle = when (spanStyle.fontStyle) {
                                    "Italic" -> FontStyle.Italic
                                    else -> FontStyle.Normal
                                },
                                fontSize = when {
                                    spanStyle.fontSize == null -> TextUnit.Unspecified
                                    spanStyle.fontSize.isNaN() -> TextUnit.Unspecified
                                    spanStyle.fontSize < 0 -> TextUnit.Unspecified
                                    else -> spanStyle.fontSize.sp
                                }
                            )
                            richTextState.addSpanStyle(style, TextRange(spanStyle.start, spanStyle.end) )
                        }
                    }
                    // Apply paragraph styles
                    val style = when (paragraphStyle.textAlign) {
                        "Left" -> TextAlign.Left
                        "Center" -> TextAlign.Center
                        "Right" -> TextAlign.Right
                        "Justify" -> TextAlign.Justify
                        else -> null
                    }?.let {
                        ParagraphStyle(
                            textAlign = it
                        )
                    }
                    if (style != null) {
                        richTextState.toggleParagraphStyle(style)
                    }
                }
            } catch (e: Exception) {
                Log.e("EditorScreen", "Error restoring formatting: ${e.message}")
            }
        }
    }
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
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
                actions = {
                    IconButton(onClick = {
                        isDialogOpen.value = true
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.download_solid),
                            contentDescription = "save",
                            modifier = Modifier.size(24.dp),
                            tint = Color.White // Set icon color to white
                        )
                    }
                    IconButton(onClick = {
                        val newDocument: Document = saveDocument(document, richTextState, json)
                        viewModel.updateDocument(newDocument)
                        navigateToHome()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color.White // Set icon color to white
                        )
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E88E5), // Blue color for the app bar
                    navigationIconContentColor = Color.White, // Set navigation icon color to white
                    actionIconContentColor = Color.White, // Set action icon color to white
                    titleContentColor = Color.White // Set title color to white
                )
            )
        }


    ){

        if (isDialogOpen.value){
            AlertDialog(
                onDismissRequest = {isDialogOpen.value = false},
                title ={ Text(text = "Enter document title")},
                text = {
                    Column {
                        Text(text = "Please enter a title for your document:")
                        TextField(
                            value = titleState.value,
                            onValueChange = { titleState.value = it },
                            placeholder = { Text(text = "Document Title") }
                        )
                    }
                },
                confirmButton = {
                    TextButton( onClick = {
                        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                        } else {
                            ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        }
                        if (hasPermission){
                            exportToPdf(context, titleState.value, richTextState)
                        }else{
                            Toast.makeText(context, "Please grant storage permission", Toast.LENGTH_SHORT).show()
                            redirectToSettings(context)
                        }
                        isDialogOpen.value = false
                    }){
                        Text(text = "Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        isDialogOpen.value = false
                    }) {
                        Text(text = "Cancel")
                    }
                }
            )
        }

        Column(modifier = Modifier
            .padding(it)
            .fillMaxSize()){

                FlowRow(
                    mainAxisSpacing = 10.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)) {
                    IconButton(onClick = {
                        richTextState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    },
                        modifier = Modifier
                            .background(
                                color = if (isBold) Color.Gray else Color.Transparent,
                            )
                            .padding(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.bold_solid),
                            contentDescription = "Bold",
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(onClick = {
                        richTextState.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    },
                        modifier = Modifier
                            .background(
                                color = if (isItalic) Color.Gray else Color.Transparent,
                            )
                            .padding(4.dp))
                    {
                        Icon(
                            painter = painterResource(id = R.drawable.italic_solid),
                            contentDescription = "Italic",
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Box()
                    {
                        DropdownMenu(
                            expanded = sizeDropDownExpanded.value,
                            onDismissRequest = { sizeDropDownExpanded.value = false }
                        ){
                            fontSizes.forEach { fontSize ->
                                DropdownMenuItem(
                                    text = { Text(text = "${fontSize.value.toInt()}sp")},
                                    onClick = {
                                        selectedFontSize.value = fontSize
                                        richTextState.toggleSpanStyle(SpanStyle(fontSize = fontSize))
                                        sizeDropDownExpanded.value = false
                                    }
                                )
                            }
                        }

                        // Selected font size and dropdown icon
                        Row(
                            modifier = Modifier
                                .clickable {
                                    sizeDropDownExpanded.value = true
                                } // Open dropdown on click
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "${selectedFontSize.value.value.toInt()} sp",
                                modifier = Modifier.padding(end = 4.dp),    // Space between text and icon
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold

                            )
                            Icon(imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown",
                                modifier = Modifier.size(24.dp)
                                )
                        }
                    }

                    IconButton(onClick = {
                        richTextState.toggleParagraphStyle(ParagraphStyle(textAlign = TextAlign.Left))
                    },
                        modifier = Modifier
                            .background(
                                color = if (isLeftAlignActive) Color.Gray else Color.Transparent,
                            )
                            .padding(4.dp)) {
                        Icon(
                            painter = painterResource(id = R.drawable.align_left_solid),
                            contentDescription = "Left Align",
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(onClick = {
                        richTextState.toggleParagraphStyle(ParagraphStyle(textAlign = TextAlign.Center))
                    },
                        modifier = Modifier
                            .background(
                                color = if (isCenterAlignActive) Color.Gray else Color.Transparent,
                            )
                            .padding(4.dp)) {
                        Icon(
                            painter = painterResource(id = R.drawable.align_center_solid),
                            contentDescription = "Center Align",
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(onClick = {
                        richTextState.toggleParagraphStyle(ParagraphStyle(textAlign = TextAlign.Right))
                    },
                        modifier = Modifier
                            .background(
                                color = if (isRightAlignActive) Color.Gray else Color.Transparent,
                            )
                            .padding(4.dp)) {
                        Icon(
                            painter = painterResource(id = R.drawable.align_right_solid),
                            contentDescription = "Right Align",
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(onClick = {
                        richTextState.toggleParagraphStyle(ParagraphStyle(textAlign = TextAlign.Justify))
                    },
                        modifier = Modifier
                            .background(
                                color = if (isJustifyAlignActive) Color.Gray else Color.Transparent,
                            )
                            .padding(4.dp)) {
                        Icon(
                            painter = painterResource(id = R.drawable.align_justify_solid),
                            contentDescription = "Justify Align",
                            modifier = Modifier.size(24.dp)
                        )
                    }

                }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .imePadding()
            ){
                RichTextEditor(
                    state = richTextState,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding(),
                    placeholder = { Text(text = "Start typing...") }
                )
            }
        }
    }
}

fun exportToPdf(context: Context, fileName: String, richTextState: RichTextState){
    try {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val pdfFile = File(downloadDir, "$fileName.pdf")
        val pdfWriter = PdfWriter(pdfFile)
        val pdfDocument = PdfDocument(pdfWriter)
        val document = com.itextpdf.layout.Document(pdfDocument)

        val annotatedString = richTextState.annotatedString
        val text = annotatedString.text

        annotatedString.paragraphStyles.forEach { paragraphStyles ->

            val currentParagraph = com.itextpdf.layout.element.Paragraph()
            val paragraphText = annotatedString.text.substring(paragraphStyles.start, paragraphStyles.end)

            if (paragraphText.isNotEmpty()) {
                // Split the paragraph text into segments based on span styles
                var currentPosition = paragraphStyles.start

                // Find all span styles that intersect with this paragraph
                val relevantSpans = annotatedString.spanStyles.filter { spanStyle ->
                    spanStyle.start < paragraphStyles.end && spanStyle.end > paragraphStyles.start
                }.sortedBy { it.start }

                if (relevantSpans.isEmpty()) {
                    // No styling - add plain text
                    currentParagraph.add(com.itextpdf.layout.element.Text(paragraphText))
                } else {
                    // Handle text segments with different styles
                    relevantSpans.forEachIndexed { index, currentSpan ->
                        // Handle any unstyled text before the current span
                        if (currentPosition < currentSpan.start) {
                            val plainText = annotatedString.text.substring(
                                currentPosition,
                                currentSpan.start
                            )
                            currentParagraph.add(com.itextpdf.layout.element.Text(plainText))
                        }

                        // Handle the styled text segment
                        val spanEnd = if (index < relevantSpans.size - 1) {
                            minOf(currentSpan.end, relevantSpans[index + 1].start)
                        } else {
                            minOf(currentSpan.end, paragraphStyles.end)
                        }

                        val styledText = annotatedString.text.substring(
                            maxOf(currentSpan.start, paragraphStyles.start),
                            minOf(spanEnd, paragraphStyles.end)
                        )

                        val span = com.itextpdf.layout.element.Text(styledText)

                        // Apply all styles to the text segment
                        currentSpan.item.let { style ->
                            style.fontWeight?.let { if (it == FontWeight.Bold) span.setBold() }
                            style.fontStyle?.let { if (it == FontStyle.Italic) span.setItalic() }
                            style.textDecoration?.let { if (it == TextDecoration.Underline) span.setUnderline() }
                            style.fontSize?.let { if (!it.value.isNaN()) span.setFontSize(it.value) }
                        }

                        currentParagraph.add(span)
                        currentPosition = spanEnd
                    }

                    // Handle any remaining unstyled text after the last span
                    if (currentPosition < paragraphStyles.end) {
                        val remainingText = annotatedString.text.substring(
                            currentPosition,
                            paragraphStyles.end
                        )
                        currentParagraph.add(com.itextpdf.layout.element.Text(remainingText))
                    }
                }

                // Apply paragraph alignment
                val alignment = when (paragraphStyles.item.textAlign) {
                    TextAlign.Left -> com.itextpdf.layout.properties.TextAlignment.LEFT
                    TextAlign.Center -> com.itextpdf.layout.properties.TextAlignment.CENTER
                    TextAlign.Right -> com.itextpdf.layout.properties.TextAlignment.RIGHT
                    TextAlign.Justify -> com.itextpdf.layout.properties.TextAlignment.JUSTIFIED
                    else -> com.itextpdf.layout.properties.TextAlignment.LEFT
                }
                currentParagraph.setTextAlignment(alignment)
            }
            // Add the paragraph even if empty (to preserve line breaks)
            document.add(currentParagraph)
        }
        Log.d("pdf", "exportToPdf: done")
        Toast.makeText(context, "Saved successfully", Toast.LENGTH_SHORT).show()
        document.close()

    }catch (e: Exception){
        e.printStackTrace()
        Log.d("FileError", "exportToPdf: ${e.message}")
    }
}

private fun redirectToSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}


fun saveDocument(document: Document, richTextState: RichTextState, json:Json): Document{

    val formattingData = FormattingData(
        spanStyles = richTextState.annotatedString.spanStyles.map { style ->
            SpanStyleData(
                start = style.start,
                end = style.end,
                fontWeight = when (style.item.fontWeight) {
                    FontWeight.Bold -> "Bold"
                    else -> null
                },
                fontStyle = when (style.item.fontStyle) {
                    FontStyle.Italic -> "Italic"
                    else -> null
                },
                textDecoration = when (style.item.textDecoration) {
                    TextDecoration.Underline -> "Underline"
                    else -> null
                },
                fontSize = style.item.fontSize?.value
            )
        },
        paragraphStyles = richTextState.annotatedString.paragraphStyles.map { style ->
            ParagraphStyleData(
                start = style.start,
                end = style.end,
                textAlign = when (style.item.textAlign) {
                    TextAlign.Left -> "Left"
                    TextAlign.Center -> "Center"
                    TextAlign.Right -> "Right"
                    TextAlign.Justify -> "Justify"
                    else -> null
                }
            )
        }
    )
    document.apply {
        content = richTextState.toText()
        formatting = json.encodeToString(formattingData)
    }
    return document
}
