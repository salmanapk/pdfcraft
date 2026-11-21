package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.example.data.local.CompressionLevel
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class PdfInspectInfo(
    val displayName: String,
    val sizeBytes: Long,
    val pageCount: Int,
    val isPasswordProtected: Boolean
)

data class PageEditSpec(
    val originalIndex: Int,
    val rotationDelta: Int = 0
)

enum class ScanFilterMode(val label: String) {
    ORIGINAL("Original"),
    DOCUMENT_ENHANCE("Enhance"),
    GRAYSCALE("Grayscale"),
    BLACK_AND_WHITE("B&W Doc")
}

enum class PageSizePreset(val label: String) {
    A4("A4"),
    LETTER("Letter"),
    FIT("Fit Image")
}

data class ProcessedPdfResult(
    val file: File,
    val title: String,
    val toolTag: String,
    val pageCount: Int,
    val fileSizeBytes: Long,
    val originalSizeBytes: Long? = null,
    val isProtected: Boolean = false
)

data class ExportedImagesResult(
    val savedCount: Int,
    val formatLabel: String,
    val previewUris: List<Uri>
)

class PdfToolsEngine(private val context: Context) {

    init {
        PDFBoxResourceLoader.init(context.applicationContext)
    }

    suspend fun inspectPdf(uri: Uri): PdfInspectInfo = withContext(Dispatchers.IO) {
        val name = FileExportHelper.getDisplayName(context, uri)
        val size = FileExportHelper.getFileSize(context, uri)
        val temp = FileExportHelper.copyUriToTempFile(context, uri, "inspect")
        try {
            PDDocument.load(temp).use { doc ->
                PdfInspectInfo(
                    displayName = name,
                    sizeBytes = if (size > 0) size else temp.length(),
                    pageCount = doc.numberOfPages,
                    isPasswordProtected = doc.isEncrypted
                )
            }
        } catch (_: InvalidPasswordException) {
            PdfInspectInfo(
                displayName = name,
                sizeBytes = if (size > 0) size else temp.length(),
                pageCount = 0,
                isPasswordProtected = true
            )
        } finally {
            temp.delete()
        }
    }

    suspend fun inspectLocalFile(file: File): PdfInspectInfo = withContext(Dispatchers.IO) {
        try {
            PDDocument.load(file).use { doc ->
                PdfInspectInfo(
                    displayName = file.name,
                    sizeBytes = file.length(),
                    pageCount = doc.numberOfPages,
                    isPasswordProtected = doc.isEncrypted
                )
            }
        } catch (_: InvalidPasswordException) {
            PdfInspectInfo(
                displayName = file.name,
                sizeBytes = file.length(),
                pageCount = 0,
                isPasswordProtected = true
            )
        }
    }

    suspend fun renderFilePageBitmap(
        file: File,
        pageIndex: Int,
        targetWidthPx: Int = 900
    ): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (pageIndex !in 0 until renderer.pageCount) return@use null
                    renderer.openPage(pageIndex).use { page ->
                        val scale = targetWidthPx.toFloat() / page.width.coerceAtLeast(1)
                        val width = targetWidthPx.coerceAtLeast(120)
                        val height = (page.height * scale).toInt().coerceAtLeast(120)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        Canvas(bitmap).drawColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                }
            }
        }.getOrNull()
    }

    suspend fun renderUriThumbnails(
        uri: Uri,
        maxThumbnails: Int = 40,
        thumbWidthPx: Int = 300
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val temp = FileExportHelper.copyUriToTempFile(context, uri, "thumb")
        val bitmaps = mutableListOf<Bitmap>()
        try {
            ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val count = renderer.pageCount.coerceAtMost(maxThumbnails)
                    for (i in 0 until count) {
                        renderer.openPage(i).use { page ->
                            val scale = thumbWidthPx.toFloat() / page.width.coerceAtLeast(1)
                            val w = thumbWidthPx
                            val h = (page.height * scale).toInt().coerceAtLeast(80)
                            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            Canvas(bmp).drawColor(Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bitmaps.add(bmp)
                        }
                    }
                }
            }
        } finally {
            temp.delete()
        }
        bitmaps
    }

    suspend fun mergePdfs(uris: List<Uri>, outputName: String): ProcessedPdfResult =
        withContext(Dispatchers.IO) {
            require(uris.size >= 2) { "Select at least 2 PDF files to merge." }
            val tempFiles = uris.mapIndexed { idx, uri ->
                FileExportHelper.copyUriToTempFile(context, uri, "merge_$idx")
            }
            val outFile = FileExportHelper.createOutputPdfFile(context, outputName)
            try {
                val merger = PDFMergerUtility()
                merger.destinationFileName = outFile.absolutePath
                tempFiles.forEach { merger.addSource(it) }
                merger.mergeDocuments(MemoryUsageSetting.setupTempFileOnly())
                val pages = PDDocument.load(outFile).use { it.numberOfPages }
                ProcessedPdfResult(
                    file = outFile,
                    title = outFile.nameWithoutExtension,
                    toolTag = "Merged",
                    pageCount = pages,
                    fileSizeBytes = outFile.length()
                )
            } finally {
                tempFiles.forEach { it.delete() }
            }
        }

    suspend fun splitPdf(
        uri: Uri,
        zeroBasedPages: List<Int>,
        outputName: String
    ): ProcessedPdfResult = withContext(Dispatchers.IO) {
        require(zeroBasedPages.isNotEmpty()) { "Select at least one page to extract." }
        val temp = FileExportHelper.copyUriToTempFile(context, uri, "split")
        val outFile = FileExportHelper.createOutputPdfFile(context, outputName)
        try {
            PDDocument.load(temp).use { source ->
                PDDocument().use { target ->
                    zeroBasedPages.distinct()
                        .filter { it in 0 until source.numberOfPages }
                        .forEach { idx ->
                            target.importPage(source.getPage(idx))
                        }
                    require(target.numberOfPages > 0) { "No valid pages selected." }
                    target.save(outFile)
                    ProcessedPdfResult(
                        file = outFile,
                        title = outFile.nameWithoutExtension,
                        toolTag = "Split",
                        pageCount = target.numberOfPages,
                        fileSizeBytes = outFile.length()
                    )
                }
            }
        } finally {
            temp.delete()
        }
    }

    suspend fun organizePdf(
        uri: Uri,
        specs: List<PageEditSpec>,
        outputName: String
    ): ProcessedPdfResult = withContext(Dispatchers.IO) {
        require(specs.isNotEmpty()) { "Document must keep at least 1 page." }
        val temp = FileExportHelper.copyUriToTempFile(context, uri, "organize")
        val outFile = FileExportHelper.createOutputPdfFile(context, outputName)
        try {
            PDDocument.load(temp).use { source ->
                PDDocument().use { target ->
                    specs.forEach { spec ->
                        if (spec.originalIndex in 0 until source.numberOfPages) {
                            val imported = target.importPage(source.getPage(spec.originalIndex))
                            val newRot = (imported.rotation + spec.rotationDelta) % 360
                            imported.rotation = if (newRot < 0) newRot + 360 else newRot
                        }
                    }
                    target.save(outFile)
                    ProcessedPdfResult(
                        file = outFile,
                        title = outFile.nameWithoutExtension,
                        toolTag = "Organized",
                        pageCount = target.numberOfPages,
                        fileSizeBytes = outFile.length()
                    )
                }
            }
        } finally {
            temp.delete()
        }
    }

    suspend fun compressPdf(
        uri: Uri,
        level: CompressionLevel,
        outputName: String
    ): ProcessedPdfResult = withContext(Dispatchers.IO) {
        val temp = FileExportHelper.copyUriToTempFile(context, uri, "compress")
        val originalSize = temp.length()
        val outFile = FileExportHelper.createOutputPdfFile(context, outputName)
        try {
            ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    PDDocument().use { targetDoc ->
                        for (i in 0 until renderer.pageCount) {
                            renderer.openPage(i).use { page ->
                                val pdfWidth = page.width.toFloat()
                                val pdfHeight = page.height.toFloat()
                                val renderW = (pdfWidth * level.dpiScale).toInt().coerceIn(320, 1800)
                                val renderH = (pdfHeight * level.dpiScale).toInt().coerceIn(320, 2400)
                                val bmp = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)
                                Canvas(bmp).drawColor(Color.WHITE)
                                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                                val pdPage = PDPage(PDRectangle(pdfWidth, pdfHeight))
                                targetDoc.addPage(pdPage)
                                val pdImage = JPEGFactory.createFromImage(
                                    targetDoc,
                                    bmp,
                                    level.jpegQuality / 100f
                                )
                                PDPageContentStream(targetDoc, pdPage).use { cs ->
                                    cs.drawImage(pdImage, 0f, 0f, pdfWidth, pdfHeight)
                                }
                                bmp.recycle()
                            }
                        }
                        targetDoc.save(outFile)
                        ProcessedPdfResult(
                            file = outFile,
                            title = outFile.nameWithoutExtension,
                            toolTag = "Compressed",
                            pageCount = targetDoc.numberOfPages,
                            fileSizeBytes = outFile.length(),
                            originalSizeBytes = originalSize
                        )
                    }
                }
            }
        } finally {
            temp.delete()
        }
    }

    suspend fun imagesToPdf(
        imageUris: List<Uri>,
        filterMode: ScanFilterMode,
        pageSize: PageSizePreset,
        addMargin: Boolean,
        outputName: String
    ): ProcessedPdfResult = withContext(Dispatchers.IO) {
        require(imageUris.isNotEmpty()) { "Select or scan at least 1 image." }
        val outFile = FileExportHelper.createOutputPdfFile(context, outputName)
        PDDocument().use { doc ->
            for (uri in imageUris) {
                val rawBmp = decodeScaledBitmap(uri, maxDimension = 1800) ?: continue
                val filtered = applyScanFilter(rawBmp, filterMode)
                if (filtered !== rawBmp) rawBmp.recycle()

                val pageRect = when (pageSize) {
                    PageSizePreset.A4 -> PDRectangle.A4
                    PageSizePreset.LETTER -> PDRectangle.LETTER
                    PageSizePreset.FIT -> {
                        val maxPt = 595f
                        val scale = maxPt / filtered.width.coerceAtLeast(1)
                        PDRectangle(maxPt, filtered.height * scale)
                    }
                }
                val page = PDPage(pageRect)
                doc.addPage(page)

                val margin = if (addMargin && pageSize != PageSizePreset.FIT) 28f else 0f
                val availW = pageRect.width - margin * 2f
                val availH = pageRect.height - margin * 2f
                val scale = minOf(availW / filtered.width, availH / filtered.height)
                val drawW = filtered.width * scale
                val drawH = filtered.height * scale
                val x = (pageRect.width - drawW) / 2f
                val y = (pageRect.height - drawH) / 2f

                val pdImage = JPEGFactory.createFromImage(doc, filtered, 0.86f)
                PDPageContentStream(doc, page).use { cs ->
                    cs.drawImage(pdImage, x, y, drawW, drawH)
                }
                filtered.recycle()
            }
            require(doc.numberOfPages > 0) { "Could not decode any selected images." }
            doc.save(outFile)
            ProcessedPdfResult(
                file = outFile,
                title = outFile.nameWithoutExtension,
                toolTag = if (filterMode == ScanFilterMode.ORIGINAL) "Photo PDF" else "Scanned",
                pageCount = doc.numberOfPages,
                fileSizeBytes = outFile.length()
            )
        }
    }

    suspend fun pdfToImages(
        uri: Uri,
        zeroBasedPages: List<Int>,
        asPng: Boolean
    ): ExportedImagesResult = withContext(Dispatchers.IO) {
        val temp = FileExportHelper.copyUriToTempFile(context, uri, "toimg")
        val baseName = FileExportHelper.getDisplayName(context, uri).removeSuffix(".pdf")
        val savedUris = mutableListOf<Uri>()
        try {
            ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val pagesToExport = if (zeroBasedPages.isEmpty()) {
                        (0 until renderer.pageCount).toList()
                    } else {
                        zeroBasedPages.filter { it in 0 until renderer.pageCount }
                    }
                    for (idx in pagesToExport) {
                        renderer.openPage(idx).use { page ->
                            val scale = 2.0f
                            val w = (page.width * scale).toInt().coerceIn(600, 2200)
                            val h = (page.height * scale).toInt().coerceIn(600, 3000)
                            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            Canvas(bmp).drawColor(Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                            val saved = FileExportHelper.saveBitmapToGallery(
                                context = context,
                                bitmap = bmp,
                                fileNameNoExt = "${baseName}_page_${idx + 1}",
                                asPng = asPng
                            )
                            if (saved != null) savedUris.add(saved)
                            bmp.recycle()
                        }
                    }
                }
            }
        } finally {
            temp.delete()
        }
        ExportedImagesResult(
            savedCount = savedUris.size,
            formatLabel = if (asPng) "PNG" else "JPG",
            previewUris = savedUris
        )
    }

    private fun decodeScaledBitmap(uri: Uri, maxDimension: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
        if (opts.outWidth <= 0 || opts.outHeight <= 0) return null
        var sampleSize = 1
        while (opts.outWidth / sampleSize > maxDimension || opts.outHeight / sampleSize > maxDimension) {
            sampleSize *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        }
    }

    private fun applyScanFilter(source: Bitmap, mode: ScanFilterMode): Bitmap {
        if (mode == ScanFilterMode.ORIGINAL) return source
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val matrix = when (mode) {
            ScanFilterMode.GRAYSCALE -> ColorMatrix().apply { setSaturation(0f) }
            ScanFilterMode.DOCUMENT_ENHANCE -> ColorMatrix(
                floatArrayOf(
                    1.28f, 0f, 0f, 0f, -20f,
                    0f, 1.28f, 0f, 0f, -20f,
                    0f, 0f, 1.28f, 0f, -20f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            ScanFilterMode.BLACK_AND_WHITE -> {
                val gray = ColorMatrix().apply { setSaturation(0f) }
                val contrast = ColorMatrix(
                    floatArrayOf(
                        2.1f, 0f, 0f, 0f, -135f,
                        0f, 2.1f, 0f, 0f, -135f,
                        0f, 0f, 2.1f, 0f, -135f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                gray.postConcat(contrast)
                gray
            }
            ScanFilterMode.ORIGINAL -> ColorMatrix()
        }
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }
}

fun parsePageRangeInput(input: String, totalPages: Int): List<Int> {
    if (input.isBlank() || totalPages <= 0) return emptyList()
    val result = linkedSetOf<Int>()
    input.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { token ->
        if (token.contains("-")) {
            val parts = token.split("-").mapNotNull { it.trim().toIntOrNull() }
            if (parts.size == 2) {
                val start = parts[0].coerceIn(1, totalPages)
                val end = parts[1].coerceIn(1, totalPages)
                val range = if (start <= end) start..end else start downTo end
                range.forEach { result.add(it - 1) }
            }
        } else {
            token.toIntOrNull()?.let { p ->
                if (p in 1..totalPages) result.add(p - 1)
            }
        }
    }
    return result.toList()
}
