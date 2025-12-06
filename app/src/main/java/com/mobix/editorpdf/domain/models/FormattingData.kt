package com.mobix.editorpdf.domain.models

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
    val fontWeight: String? = null,
    val fontStyle: String? = null,
    val textDecoration: String? = null,
    val fontSize: Float? = null,
    val color: Int? = null,
    val background: Int? = null // ✅ Added Background Field for Highlighter
)

@Serializable
data class ParagraphStyleData(
    val start: Int,
    val end: Int,
    val textAlign: String? = null
)