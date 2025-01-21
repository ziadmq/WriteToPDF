package com.example.documenteditor.domain.models

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.time.LocalDate
@Parcelize
@Entity(tableName = "documents")
data class Document(
    @PrimaryKey(autoGenerate = true)
    val id: Int,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "content")
    var content: String,

    @ColumnInfo(name = "last_updated")
    val lastUpdated: String,

    @ColumnInfo(name = "formatting")
    var formatting: String? = null

) : Parcelable