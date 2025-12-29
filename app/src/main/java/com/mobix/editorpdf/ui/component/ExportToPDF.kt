package com.mobix.editorpdf.ui.component

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.element.AreaBreak
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import com.itextpdf.layout.properties.AreaBreakType
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.VerticalAlignment
import com.mobix.editorpdf.ui.view.TransparentYellow
import com.mohamedrejeb.richeditor.model.RichTextState
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

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
                val uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), cv)
                uri?.let { context.contentResolver.openOutputStream(it) }
            } else {
                val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), outputName)
                FileOutputStream(file)
            }

        if (outputStream == null) return

        val writer = PdfWriter(outputStream)
        val pdf = PdfDocument(writer)
        val doc = com.itextpdf.layout.Document(pdf)

        pageStates.forEachIndexed { pageIndex, state ->
            if (pageIndex > 0) doc.add(AreaBreak(AreaBreakType.NEXT_PAGE))

            val annotated = state.annotatedString
            val fullText = annotated.text
            val spans = annotated.spanStyles

            val imageTagRegex = "\\[IMAGE:([^]]+)]".toRegex()
            val parts = imageTagRegex.split(fullText)
            val images = imageTagRegex.findAll(fullText).toList()

            var pointer = 0

            parts.forEachIndexed { index, part ->
                if (part.isNotEmpty()) {
                    val paragraph = Paragraph()
                    var i = 0
                    while (i < part.length) {
                        val ch = part[i]
                        val globalIndex = pointer + i
                        val overlappingSpans = spans.filter { globalIndex >= it.start && globalIndex < it.end }

                        val textObj = Text(ch.toString())

                        if (overlappingSpans.isNotEmpty()) {
                            if (overlappingSpans.any { it.item.fontWeight != null && it.item.fontWeight!!.weight >= 700 }) textObj.setBold()
                            if (overlappingSpans.any { it.item.fontStyle == FontStyle.Italic }) textObj.setItalic()
                            if (overlappingSpans.any { it.item.textDecoration == TextDecoration.Underline }) textObj.setUnderline()

                            overlappingSpans.lastOrNull { it.item.fontSize != TextUnit.Unspecified }
                                ?.let { textObj.setFontSize(it.item.fontSize.value) }

                            overlappingSpans.lastOrNull { it.item.color != Color.Unspecified }
                                ?.let {
                                    val c = it.item.color.toArgb()
                                    textObj.setFontColor(DeviceRgb(android.graphics.Color.red(c), android.graphics.Color.green(c), android.graphics.Color.blue(c)))
                                }

                            // FIX: Added alpha > 0f check to avoid black background blocks on formatted text
                            overlappingSpans.lastOrNull { it.item.background != Color.Unspecified && it.item.background.alpha > 0f }
                                ?.let {
                                    val c = it.item.background.toArgb()
                                    if (c == TransparentYellow.toArgb()) {
                                        textObj.setBackgroundColor(DeviceRgb(255, 255, 0))
                                    } else {
                                        textObj.setBackgroundColor(DeviceRgb(android.graphics.Color.red(c), android.graphics.Color.green(c), android.graphics.Color.blue(c)))
                                    }
                                }
                        }
                        paragraph.add(textObj)
                        i++
                    }
                    paragraph.setTextAlignment(TextAlignment.LEFT)
                    doc.add(paragraph)
                }

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
                    } catch (_: Exception) {}
                    pointer += part.length + match.value.length
                } else {
                    pointer += part.length
                }
            }

            val pageNumText = "${pageIndex + 1} / ${pageStates.size}"
            doc.showTextAligned(Paragraph(pageNumText), 297.5f, 20f, pageIndex + 1, TextAlignment.CENTER, VerticalAlignment.BOTTOM, 0f)
        }

        doc.close()
        outputStream.close()
        Toast.makeText(context, "Saved: $outputName", Toast.LENGTH_LONG).show()

    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
