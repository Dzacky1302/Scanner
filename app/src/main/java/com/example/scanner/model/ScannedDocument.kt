package com.example.scanner.model

import android.net.Uri
import java.io.File

data class ScannedDocument(
    val id: String,
    val name: String,
    val path: String,
    val uri: Uri,
    val sizeFormatted: String,
    val dateModifiedFormatted: String,
    val extension: String,
    val file: File
)

data class SelectedImageItem(
    val uri: Uri,
    val path: String? = null
)

data class SelectedPdfItem(
    val uri: Uri,
    val name: String,
    val size: Long
)

enum class ConversionType {
    PDF_TO_IMAGE,
    IMAGE_TO_PDF,
    PDF_TO_DOCX,
    PDF_TO_XLSX,
    PDF_TO_PPTX
}

enum class CompressionType {
    IMAGE,
    PDF,
    ZIP
}
