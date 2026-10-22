package com.example.scanner.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.scanner.model.ScannedDocument
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {

    fun getOutputDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "ScannerPro")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getCacheTempDir(context: Context): File {
        val dir = File(context.cacheDir, "temp_scanner")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(Locale.getDefault(), "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun shareFile(context: Context, file: File, mimeType: String = "*/*") {
        val uri = getUriForFile(context, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan file"))
    }

    fun getFileNameFromUri(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndex("_display_name")
                    if (columnIndex != -1) {
                        result = it.getString(columnIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "document_${System.currentTimeMillis()}"
    }

    fun copyUriToTempFile(context: Context, uri: Uri, prefix: String = "temp_", suffix: String = ".tmp"): File {
        val tempFile = File.createTempFile(prefix, suffix, getCacheTempDir(context))
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }

    fun getAllSavedDocuments(context: Context): List<ScannedDocument> {
        val outputDir = getOutputDir(context)
        val files = outputDir.listFiles() ?: return emptyList()
        return files.filter { it.isFile }.sortedByDescending { it.lastModified() }.map { file ->
            ScannedDocument(
                id = file.name,
                name = file.name,
                path = file.absolutePath,
                uri = getUriForFile(context, file),
                sizeFormatted = formatFileSize(file.length()),
                dateModifiedFormatted = formatDate(file.lastModified()),
                extension = file.extension.lowercase(Locale.getDefault()),
                file = file
            )
        }
    }
}
