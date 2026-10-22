package com.example.scanner.utils

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipUtils {

    suspend fun compressFilesToZip(
        context: Context,
        fileUris: List<Uri>,
        outputZipName: String = "Archive_${System.currentTimeMillis()}.zip"
    ): File = withContext(Dispatchers.IO) {
        val outputDir = FileUtils.getOutputDir(context)
        val zipFile = File(outputDir, if (outputZipName.endsWith(".zip")) outputZipName else "$outputZipName.zip")

        FileOutputStream(zipFile).use { fos ->
            ZipOutputStream(fos).use { zos ->
                val buffer = ByteArray(8192)

                for (i in fileUris.indices) {
                    val uri = fileUris[i]
                    val fileName = FileUtils.getFileNameFromUri(context, uri)
                    val tempFile = FileUtils.copyUriToTempFile(context, uri, "zip_src_${i}_", "")

                    try {
                        FileInputStream(tempFile).use { fis ->
                            val entry = ZipEntry(fileName)
                            zos.putNextEntry(entry)

                            var length: Int
                            while (fis.read(buffer).also { length = it } > 0) {
                                zos.write(buffer, 0, length)
                            }
                            zos.closeEntry()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        tempFile.delete()
                    }
                }
            }
        }

        return@withContext zipFile
    }
}
