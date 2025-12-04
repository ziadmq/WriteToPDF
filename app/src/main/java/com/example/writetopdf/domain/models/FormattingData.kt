package com.example.writetopdf.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class FormattingData(
    val spanStyles: List<SpanStyleData>,
    val paragraphStyles: List<ParagraphStyleData>
)

@Serializable
data class SpanStyleData (
    val start: Int,
    val end: Int,
    val fontWeight: String? = null,  // "Bold" or null
    val fontStyle: String? = null,   // "Italic" or null
    val textDecoration: String? = null, // "Underline" or null
    val fontSize: Float? = null
)

@Serializable
data class ParagraphStyleData(
    val start: Int,
    val end: Int,
    val textAlign: String? = null  // "Left", "Center", "Right", "Justify"
)


