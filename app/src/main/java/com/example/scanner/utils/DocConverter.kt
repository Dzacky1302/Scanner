package com.example.scanner.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DocConverter {

    suspend fun pdfToDocx(
        context: Context,
        pdfUri: Uri,
        outputFileName: String = "Converted_${System.currentTimeMillis()}.docx"
    ): File = withContext(Dispatchers.IO) {
        val outputDir = FileUtils.getOutputDir(context)
        val outputFile = File(outputDir, if (outputFileName.endsWith(".docx")) outputFileName else "$outputFileName.docx")

        val pageTexts = extractTextFromPdfPages(context, pdfUri)

        val docXml = StringBuilder()
        docXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        docXml.append("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">\n")
        docXml.append("<w:body>\n")

        for ((index, pageText) in pageTexts.withIndex()) {
            docXml.append("<w:p><w:r><w:rPr><w:b/></w:rPr><w:t>--- Halaman ${index + 1} ---</w:t></w:r></w:p>\n")
            val lines = pageText.split("\n")
            for (line in lines) {
                if (line.isNotBlank()) {
                    val escapedLine = escapeXml(line)
                    docXml.append("<w:p><w:r><w:t>$escapedLine</w:t></w:r></w:p>\n")
                }
            }
            docXml.append("<w:p/>\n")
        }

        docXml.append("</w:body>\n")
        docXml.append("</w:document>")

        val contentTypesXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">\n" +
                "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>\n" +
                "<Default Extension=\"xml\" ContentType=\"application/xml\"/>\n" +
                "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>\n" +
                "</Types>"

        val relsXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>\n" +
                "</Relationships>"

        FileOutputStream(outputFile).use { fos ->
            ZipOutputStream(fos).use { zos ->
                addZipEntry(zos, "[Content_Types].xml", contentTypesXml)
                addZipEntry(zos, "_rels/.rels", relsXml)
                addZipEntry(zos, "word/document.xml", docXml.toString())
            }
        }

        return@withContext outputFile
    }

    suspend fun pdfToXlsx(
        context: Context,
        pdfUri: Uri,
        outputFileName: String = "Converted_${System.currentTimeMillis()}.xlsx"
    ): File = withContext(Dispatchers.IO) {
        val outputDir = FileUtils.getOutputDir(context)
        val outputFile = File(outputDir, if (outputFileName.endsWith(".xlsx")) outputFileName else "$outputFileName.xlsx")

        val pageTexts = extractTextFromPdfPages(context, pdfUri)

        val sheetXml = StringBuilder()
        sheetXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        sheetXml.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n")
        sheetXml.append("<sheetData>\n")

        var rowIndex = 1

        for ((pIndex, pageText) in pageTexts.withIndex()) {
            sheetXml.append("<row r=\"$rowIndex\">\n")
            sheetXml.append("<c r=\"A$rowIndex\" t=\"inlineStr\"><is><t>Halaman ${pIndex + 1}</t></is></c>\n")
            sheetXml.append("</row>\n")
            rowIndex++

            val lines = pageText.split("\n")
            for (line in lines) {
                if (line.isNotBlank()) {
                    val cells = line.split("\\s{2,}|\\t|,".toRegex())
                    sheetXml.append("<row r=\"$rowIndex\">\n")
                    for ((cIndex, cellValue) in cells.withIndex()) {
                        val colLetter = getColumnLetter(cIndex)
                        val escapedValue = escapeXml(cellValue.trim())
                        sheetXml.append("<c r=\"$colLetter$rowIndex\" t=\"inlineStr\"><is><t>$escapedValue</t></is></c>\n")
                    }
                    sheetXml.append("</row>\n")
                    rowIndex++
                }
            }
            rowIndex++
        }

        sheetXml.append("</sheetData>\n")
        sheetXml.append("</worksheet>")

        val contentTypesXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">\n" +
                "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>\n" +
                "<Default Extension=\"xml\" ContentType=\"application/xml\"/>\n" +
                "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>\n" +
                "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>\n" +
                "</Types>"

        val relsXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>\n" +
                "</Relationships>"

        val workbookXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">\n" +
                "<sheets><sheet name=\"Sheet1\" sheetId=\"1\" r:id=\"rId1\"/></sheets>\n" +
                "</workbook>"

        val wbRelsXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>\n" +
                "</Relationships>"

        FileOutputStream(outputFile).use { fos ->
            ZipOutputStream(fos).use { zos ->
                addZipEntry(zos, "[Content_Types].xml", contentTypesXml)
                addZipEntry(zos, "_rels/.rels", relsXml)
                addZipEntry(zos, "xl/workbook.xml", workbookXml)
                addZipEntry(zos, "xl/_rels/workbook.xml.rels", wbRelsXml)
                addZipEntry(zos, "xl/worksheets/sheet1.xml", sheetXml.toString())
            }
        }

        return@withContext outputFile
    }

    suspend fun pdfToPptx(
        context: Context,
        pdfUri: Uri,
        outputFileName: String = "Converted_${System.currentTimeMillis()}.pptx"
    ): File = withContext(Dispatchers.IO) {
        val outputDir = FileUtils.getOutputDir(context)
        val outputFile = File(outputDir, if (outputFileName.endsWith(".pptx")) outputFileName else "$outputFileName.pptx")

        val pageTexts = extractTextFromPdfPages(context, pdfUri)

        val presentationXml = StringBuilder()
        presentationXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        presentationXml.append("<p:presentation xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">\n")
        presentationXml.append("<p:sldIdLst>\n")

        for (i in pageTexts.indices) {
            presentationXml.append("<p:sldId id=\"${256 + i}\" r:id=\"rId${i + 1}\"/>\n")
        }

        presentationXml.append("</p:sldIdLst>\n")
        presentationXml.append("<p:sldSz cx=\"9144000\" cy=\"6858000\"/>\n")
        presentationXml.append("</p:presentation>")

        FileOutputStream(outputFile).use { fos ->
            ZipOutputStream(fos).use { zos ->
                val contentTypes = StringBuilder()
                contentTypes.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
                contentTypes.append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">\n")
                contentTypes.append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>\n")
                contentTypes.append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>\n")
                contentTypes.append("<Override PartName=\"/ppt/presentation.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml\"/>\n")

                for (i in pageTexts.indices) {
                    contentTypes.append("<Override PartName=\"/ppt/slides/slide${i + 1}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.presentationml.slide+xml\"/>\n")
                }
                contentTypes.append("</Types>")
                addZipEntry(zos, "[Content_Types].xml", contentTypes.toString())

                val relsXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                        "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                        "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"ppt/presentation.xml\"/>\n" +
                        "</Relationships>"
                addZipEntry(zos, "_rels/.rels", relsXml)

                addZipEntry(zos, "ppt/presentation.xml", presentationXml.toString())

                val presRels = StringBuilder()
                presRels.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
                presRels.append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n")
                for (i in pageTexts.indices) {
                    presRels.append("<Relationship Id=\"rId${i + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide\" Target=\"slides/slide${i + 1}.xml\"/>\n")
                }
                presRels.append("</Relationships>")
                addZipEntry(zos, "ppt/_rels/presentation.xml.rels", presRels.toString())

                for ((i, pageText) in pageTexts.withIndex()) {
                    val slideXml = StringBuilder()
                    slideXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
                    slideXml.append("<p:sld xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\" xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\">\n")
                    slideXml.append("<p:cSld><p:spTree><p:nvGrpSpPr><p:cNvPr id=\"1\" name=\"\"/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr/>\n")

                    slideXml.append("<p:sp><p:nvSpPr><p:cNvPr id=\"2\" name=\"Title\"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr><p:spPr/>\n")
                    slideXml.append("<p:txBody><a:bodyPr/><a:lstStyle/><a:p><a:r><a:t>Halaman ${i + 1}</a:t></a:r></a:p></p:txBody></p:sp>\n")

                    slideXml.append("<p:sp><p:nvSpPr><p:cNvPr id=\"3\" name=\"Content\"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr><p:spPr/>\n")
                    slideXml.append("<p:txBody><a:bodyPr/><a:lstStyle/>\n")

                    val lines = pageText.split("\n")
                    for (line in lines) {
                        if (line.isNotBlank()) {
                            val escaped = escapeXml(line)
                            slideXml.append("<a:p><a:r><a:t>$escaped</a:t></a:r></a:p>\n")
                        }
                    }

                    slideXml.append("</p:txBody></p:sp>\n")
                    slideXml.append("</p:spTree></p:cSld></p:sld>")

                    addZipEntry(zos, "ppt/slides/slide${i + 1}.xml", slideXml.toString())
                }
            }
        }

        return@withContext outputFile
    }

    private suspend fun extractTextFromPdfPages(context: Context, pdfUri: Uri): List<String> {
        val pageTexts = mutableListOf<String>()
        val tempFile = FileUtils.copyUriToTempFile(context, pdfUri, "extract_txt_", ".pdf")

        try {
            val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                var text = ""
                try {
                    text = OcrManager.recognizeTextFromBitmap(bitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                if (text.isBlank()) {
                    text = "Halaman ${i + 1} (Gambar Dokumen)"
                }

                pageTexts.add(text)
                bitmap.recycle()
            }

            renderer.close()
            pfd.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            tempFile.delete()
        }

        if (pageTexts.isEmpty()) {
            pageTexts.add("Teks dokumen tidak dapat diekstrak.")
        }

        return pageTexts
    }

    private fun addZipEntry(zos: ZipOutputStream, path: String, content: String) {
        val entry = ZipEntry(path)
        zos.putNextEntry(entry)
        val writer = OutputStreamWriter(zos, Charsets.UTF_8)
        writer.write(content)
        writer.flush()
        zos.closeEntry()
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun getColumnLetter(colIndex: Int): String {
        var temp = colIndex
        var letter = ""
        while (temp >= 0) {
            letter = (('A'.code + (temp % 26)).toChar()) + letter
            temp = (temp / 26) - 1
        }
        return letter
    }
}
