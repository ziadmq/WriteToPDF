package com.mobix.editorpdf.ui.view

import android.app.Activity
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobix.editorpdf.domain.models.Document
import com.mobix.editorpdf.ui.theme.*
import com.mobix.editorpdf.ui.component.exportToPdf
import com.mobix.editorpdf.ui.viwmodel.DocumentViewModel
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mobix.editorpdf.ui.component.AdManager
import androidx.compose.material.icons.filled.GridOn
import com.mobix.editorpdf.domain.models.TableData
import com.mobix.editorpdf.ui.component.ColorPickerRow
import com.mobix.editorpdf.ui.component.SizePickerRow
import com.mobix.editorpdf.ui.component.TableEditorDialog
import com.mobix.editorpdf.ui.component.ToolBtn
import com.mobix.editorpdf.ui.component.copyImageToInternalStorage
import com.mobix.editorpdf.ui.component.handleSave
import com.mobix.editorpdf.ui.component.restoreFormatting

val TransparentYellow = Color(0xFFFFFF00).copy(alpha = 0.5f)



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
    var showTableEditor by remember { mutableStateOf(false) }

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

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
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

    fun serializeTable(data: TableData): String {
        val flattenedCells = data.cells.flatten().joinToString("|")
        return "<br>[TABLE:${data.rows}:${data.cols}:$flattenedCells]<br>"
    }

    if (showTableEditor) {
        TableEditorDialog(
            onDismiss = { showTableEditor = false },
            onConfirm = { updatedData ->
                val tableTag = serializeTable(updatedData)
                val currentHtml = currentRichTextState.toHtml()
                currentRichTextState.setHtml(currentHtml + tableTag)
                showTableEditor = false
            }
        )
    }

    Scaffold(
        containerColor = GalaxyBackground,
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = titleState.value,
                        onValueChange = { titleState.value = it },
                        modifier = Modifier.height(70.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, color = GalaxyTextPrimary),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = GalaxyAccentTeal,
                            focusedTextColor = GalaxyTextPrimary,
                            unfocusedTextColor = GalaxyTextPrimary
                        ),
                        placeholder = { Text("Untitled", color = GalaxyTextSecondary) }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        handleSave(document.id, titleState.value, pageStates, viewModel)
                        navigateToHome()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GalaxyTextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { exportToPdf(context, titleState.value, pageStates) }) {
                        Icon(Icons.Default.Download, contentDescription = "Export PDF", tint = GalaxyAccentTeal)
                    }
                    IconButton(onClick = {
                        val activity = context as? Activity
                        if (activity != null) {
                            AdManager.showInterstitial(activity) {
                                handleSave(document.id, titleState.value, pageStates, viewModel)
                                Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show()
                                navigateToHome()
                            }
                        } else {
                            handleSave(document.id, titleState.value, pageStates, viewModel)
                            navigateToHome()
                        }
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
                    IconButton(onClick = {
                        imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Icon(Icons.Default.Image, contentDescription = "Add Image", tint = GalaxyAccentTeal)
                    }
                    IconButton(onClick = { showTableEditor = true }) {
                        Icon(Icons.Default.GridOn, contentDescription = "Add Table", tint = GalaxyAccentTeal)
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

                    val isHighlighted = currentRichTextState.currentSpanStyle.background == TransparentYellow
                    ToolBtn(isActive = isHighlighted, icon = Icons.Default.FormatColorFill) {
                        if (isHighlighted) currentRichTextState.toggleSpanStyle(SpanStyle(background = Color.Unspecified))
                        else currentRichTextState.toggleSpanStyle(SpanStyle(background = TransparentYellow))
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
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize().background(GalaxyBackground)) {
            Column {
                HorizontalPager(state = pagerState, modifier = Modifier.weight(6f)) { pageIndex ->
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        Surface(
                            modifier = Modifier.padding(8.dp).fillMaxWidth().aspectRatio(0.707f).shadow(10.dp),
                            color = Color.White, shape = RoundedCornerShape(2.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
                                RichTextEditor(
                                    state = pageStates[pageIndex],
                                    modifier = Modifier.fillMaxSize().padding(6.dp).background(Color.White).padding(bottom = 30.dp).verticalScroll(rememberScrollState()),
                                    placeholder = { Text("Page ${pageIndex + 1}...", color = Color.Gray) }
                                )
                                Text(
                                    text = "${pageIndex + 1}", color = Color.LightGray, fontSize = 12.sp,
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
                                )
                            }
                        }
                    }
                }
                Column(Modifier.fillMaxWidth().weight(2f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("${pagerState.currentPage + 1} / ${pageStates.size}", fontSize = 14.sp, color = Color.Gray)
                }
            }
        }
    }
}

