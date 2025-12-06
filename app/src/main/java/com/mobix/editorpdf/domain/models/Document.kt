package com.mobix.editorpdf.domain.models

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "documents")
data class Document(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "title")
    val title: String,

    // ✅ CHANGED: This must be a List, not a String
    @ColumnInfo(name = "pages")
    val pages: List<String> = listOf(""),

    @ColumnInfo(name = "last_updated")
    val lastUpdated: String,

    @ColumnInfo(name = "formatting")
    val formatting: List<String> = listOf()
) : Parcelable