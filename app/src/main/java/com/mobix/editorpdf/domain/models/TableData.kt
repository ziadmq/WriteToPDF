package com.mobix.editorpdf.domain.models

data class TableData(
    val rows: Int,
    val cols: Int,
    val cells: List<List<String>>
)