package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdf_records")
data class PdfRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val filePath: String,
    val toolTag: String,
    val pageCount: Int,
    val fileSizeBytes: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isProtected: Boolean = false
)

@Entity(tableName = "saved_signatures")
data class SignatureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val filePath: String,
    val createdAt: Long = System.currentTimeMillis()
)
