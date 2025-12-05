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
    val fontWeight: String? = null,
    val fontStyle: String? = null,
    val textDecoration: String? = null,
    val fontSize: Float? = null,
    val color: Int? = null // ✅ Added Color Field
)

@Serializable
data class ParagraphStyleData(
    val start: Int,
    val end: Int,
    val textAlign: String? = null
)