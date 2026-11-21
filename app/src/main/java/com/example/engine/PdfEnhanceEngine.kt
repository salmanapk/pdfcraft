package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import com.tom_roush.pdfbox.util.Matrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class PageNumberStyle(val label: String) {
    NUMBER_ONLY("1, 2, 3"),
    PAGE_OF_TOTAL("1 / 10"),
    PAGE_PREFIX("Page 1 of 10")
}

enum class PageNumberPosition(val label: String) {
    BOTTOM_CENTER("Bottom Center"),
    BOTTOM_RIGHT("Bottom Right"),
    TOP_RIGHT("Top Right")
}

class PdfEnhanceEngine(private val context: Context) {

    suspend fun addWatermark(
        uri: Uri,
        watermarkText: String,
        opacity: Float,
        fontSize: Float,
        angleDegrees: Float,
        colorInt: Int,
        outputName: String
    ): ProcessedPdfResult = withContext(Dispatchers.IO) {
        val cleanText = watermarkText.trim()
        require(cleanText.isNotEmpty()) { "Enter watermark text." }
        val temp = FileExportHelper.copyUriToTempFile(context, uri, "wm")
        val outFile = FileExportHelper.createOutputPdfFile(context, outputName)
        val useBitmapFallback = !isWinAnsiSafe(cleanText)

        try {
            PDDocument.load(temp).use { doc ->
                val watermarkBitmap = if (useBitmapFallback) {
                    renderWatermarkTextToBitmap(cleanText, colorInt, opacity)
                } else null

                val pdWatermarkImg = watermarkBitmap?.let { LosslessFactory.createFromImage(doc, it) }

                for (page in doc.pages) {
                    val box = page.mediaBox
                    PDPageContentStream(
                        doc,
                        page,
                        PDPageContentStream.AppendMode.APPEND,
                        true,
                        true
                    ).use { cs ->
                        if (pdWatermarkImg != null && watermarkBitmap != null) {
                            val targetW = box.width * 0.65f
                            val targetH = targetW * (watermarkBitmap.height.toFloat() / watermarkBitmap.width)
                            cs.saveGraphicsState()
                            cs.transform(
                                Matrix.getRotateInstance(
                                    Math.toRadians(angleDegrees.toDouble()),
                                    box.width / 2f,
                                    box.height / 2f
                                )
                            )
                            cs.drawImage(pdWatermarkImg, -targetW / 2f, -targetH / 2f, targetW, targetH)
                            cs.restoreGraphicsState()
                        } else {
                            val gs = PDExtendedGraphicsState().apply {
                                nonStrokingAlphaConstant = opacity.coerceIn(0.08f, 0.9f)
                            }
                            cs.saveGraphicsState()
                            cs.setGraphicsStateParameters(gs)
                            val r = Color.red(colorInt) / 255f
                            val g = Color.green(colorInt) / 255f
                            val b = Color.blue(colorInt) / 255f
                            cs.setNonStrokingColor(r, g, b)

                            val font = PDType1Font.HELVETICA_BOLD
                            val textWidth = font.getStringWidth(cleanText) / 1000f * fontSize
                            cs.beginText()
                            cs.setFont(font, fontSize)
                            cs.setTextMatrix(
                                Matrix.getRotateInstance(
                                    Math.toRadians(angleDegrees.toDouble()),
                                    box.width / 2f,
                                    box.height / 2f
                                )
                            )
                            cs.newLineAtOffset(-textWidth / 2f, -fontSize / 3f)
                            cs.showText(cleanText)
                            cs.endText()
                            cs.restoreGraphicsState()
                        }
                    }
                }
                watermarkBitmap?.recycle()
                doc.save(outFile)
                ProcessedPdfResult(
                    file = outFile,
                    title = outFile.nameWithoutExtension,
                    toolTag = "Watermarked",
                    pageCount = doc.numberOfPages,
                    fileSizeBytes = outFile.length()
                )
            }
        } finally {
            temp.delete()
        }
    }

    suspend fun addPageNumbers(
        uri: Uri,
        style: PageNumberStyle,
        position: PageNumberPosition,
        startNumber: Int,
        outputName: String
    ): ProcessedPdfResult = withContext(Dispatchers.IO) {
        val temp = FileExportHelper.copyUriToTempFile(context, uri, "pnum")
        val outFile = FileExportHelper.createOutputPdfFile(context, outputName)
        try {
            PDDocument.load(temp).use { doc ->
                val total = doc.numberOfPages
                val font = PDType1Font.HELVETICA
                val fontSize = 11f
                doc.pages.forEachIndexed { idx, page ->
                    val currentNum = idx + startNumber
                    val totalNum = total + startNumber - 1
                    val label = when (style) {
                        PageNumberStyle.NUMBER_ONLY -> "$currentNum"
                        PageNumberStyle.PAGE_OF_TOTAL -> "$currentNum / $totalNum"
                        PageNumberStyle.PAGE_PREFIX -> "Page $currentNum of $totalNum"
                    }
                    val box = page.mediaBox
                    val textW = font.getStringWidth(label) / 1000f * fontSize
                    val x = when (position) {
                        PageNumberPosition.BOTTOM_CENTER -> (box.width - textW) / 2f
                        PageNumberPosition.BOTTOM_RIGHT -> box.width - textW - 28f
                        PageNumberPosition.TOP_RIGHT -> box.width - textW - 28f
                    }
                    val y = when (position) {
                        PageNumberPosition.TOP_RIGHT -> box.height - 26f
                        else -> 22f
                    }
                    PDPageContentStream(
                        doc,
                        page,
                        PDPageContentStream.AppendMode.APPEND,
                        true,
                        true
                    ).use { cs ->
                        cs.setNonStrokingColor(0.25f, 0.25f, 0.28f)
                        cs.beginText()
                        cs.setFont(font, fontSize)
                        cs.newLineAtOffset(x, y)
                        cs.showText(label)
                        cs.endText()
                    }
                }
                doc.save(outFile)
                ProcessedPdfResult(
                    file = outFile,
                    title = outFile.nameWithoutExtension,
                    toolTag = "Numbered",
                    pageCount = doc.numberOfPages,
                    fileSizeBytes = outFile.length()
                )
            }
        } finally {
            temp.delete()
        }
    }

    suspend fun protectPdf(
        uri: Uri,
        password: String,
        outputName: String
    ): ProcessedPdfResult = withContext(Dispatchers.IO) {
        require(password.length >= 4) { "Password must be at least 4 characters." }
        val temp = FileExportHelper.copyUriToTempFile(context, uri, "lock")
        val outFile = FileExportHelper.createOutputPdfFile(context, outputName)
        try {
            PDDocument.load(temp).use { doc ->
                val accessPermission = AccessPermission()
                val policy = StandardProtectionPolicy(password, password, accessPermission).apply {
                    encryptionKeyLength = 128
                }
                doc.protect(policy)
                doc.save(outFile)
                ProcessedPdfResult(
                    file = outFile,
                    title = outFile.nameWithoutExtension,
                    toolTag = "Protected",
                    pageCount = doc.numberOfPages,
                    fileSizeBytes = outFile.length(),
                    isProtected = true
                )
            }
        } finally {
            temp.delete()
        }
    }

    suspend fun unlockPdf(
        uri: Uri,
        password: String,
        outputName: String
    ): ProcessedPdfResult = withContext(Dispatchers.IO) {
        require(password.isNotEmpty()) { "Enter the current PDF password." }
        val temp = FileExportHelper.copyUriToTempFile(context, uri, "unlock")
        val outFile = FileExportHelper.createOutputPdfFile(context, outputName)
        try {
            PDDocument.load(temp, password).use { doc ->
                doc.isAllSecurityToBeRemoved = true
                doc.save(outFile)
                ProcessedPdfResult(
                    file = outFile,
                    title = outFile.nameWithoutExtension,
                    toolTag = "Unlocked",
                    pageCount = doc.numberOfPages,
                    fileSizeBytes = outFile.length(),
                    isProtected = false
                )
            }
        } catch (_: InvalidPasswordException) {
            throw IllegalArgumentException("Incorrect password. Please try again.")
        } finally {
            temp.delete()
        }
    }

    suspend fun signPdf(
        uri: Uri,
        signatureBitmap: Bitmap,
        pageIndex: Int,
        normCenterX: Float,
        normCenterY: Float,
        widthFraction: Float,
        outputName: String
    ): ProcessedPdfResult = withContext(Dispatchers.IO) {
        val temp = FileExportHelper.copyUriToTempFile(context, uri, "sign")
        val outFile = FileExportHelper.createOutputPdfFile(context, outputName)
        try {
            PDDocument.load(temp).use { doc ->
                require(pageIndex in 0 until doc.numberOfPages) { "Invalid page selected." }
                val page = doc.getPage(pageIndex)
                val box = page.mediaBox
                val pdImage = LosslessFactory.createFromImage(doc, signatureBitmap)
                val aspect = signatureBitmap.height.toFloat() / signatureBitmap.width.coerceAtLeast(1)
                val sigW = box.width * widthFraction.coerceIn(0.15f, 0.7f)
                val sigH = sigW * aspect

                // Convert Compose top-down normalized coordinates to PDF bottom-up coordinates
                val pdfX = (box.width * normCenterX - sigW / 2f).coerceIn(0f, (box.width - sigW).coerceAtLeast(0f))
                val pdfY = (box.height * (1f - normCenterY) - sigH / 2f).coerceIn(0f, (box.height - sigH).coerceAtLeast(0f))

                PDPageContentStream(
                    doc,
                    page,
                    PDPageContentStream.AppendMode.APPEND,
                    true,
                    true
                ).use { cs ->
                    cs.drawImage(pdImage, pdfX, pdfY, sigW, sigH)
                }
                doc.save(outFile)
                ProcessedPdfResult(
                    file = outFile,
                    title = outFile.nameWithoutExtension,
                    toolTag = "Signed",
                    pageCount = doc.numberOfPages,
                    fileSizeBytes = outFile.length()
                )
            }
        } finally {
            temp.delete()
        }
    }

    suspend fun createTextPdf(
        docTitle: String,
        bodyText: String,
        fontSizePt: Float,
        outputName: String
    ): ProcessedPdfResult = withContext(Dispatchers.IO) {
        require(bodyText.isNotBlank()) { "Enter some text to create a PDF." }
        val outFile = FileExportHelper.createOutputPdfFile(context, outputName)
        val combined = if (docTitle.isNotBlank()) "$docTitle\n\n$bodyText" else bodyText

        // Render with Android StaticLayout so English, Bengali, Arabic, emojis & all Unicode render flawlessly!
        val pageW = 1240 // Crisp 150 DPI A4
        val pageH = 1754
        val marginPx = 110
        val contentW = pageW - marginPx * 2
        val contentH = pageH - marginPx * 2

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(24, 25, 28)
            textSize = fontSizePt * 2.2f
        }
        val layout = StaticLayout.Builder
            .obtain(combined, 0, combined.length, textPaint, contentW)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(6f, 1.25f)
            .build()

        PDDocument().use { doc ->
            var currentLine = 0
            while (currentLine < layout.lineCount) {
                val pageStartY = layout.getLineTop(currentLine)
                var endLine = currentLine
                while (endLine < layout.lineCount &&
                    layout.getLineBottom(endLine) - pageStartY <= contentH
                ) {
                    endLine++
                }
                if (endLine == currentLine) endLine++

                val pageBitmap = Bitmap.createBitmap(pageW, pageH, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(pageBitmap)
                canvas.drawColor(Color.WHITE)

                val sliceHeight = (layout.getLineBottom(endLine - 1) - pageStartY).coerceAtMost(contentH)
                canvas.save()
                canvas.clipRect(marginPx, marginPx, marginPx + contentW, marginPx + sliceHeight)
                canvas.translate(marginPx.toFloat(), (marginPx - pageStartY).toFloat())
                layout.draw(canvas)
                canvas.restore()

                val page = PDPage(PDRectangle.A4)
                doc.addPage(page)
                val pdImg = JPEGFactory.createFromImage(doc, pageBitmap, 0.90f)
                PDPageContentStream(doc, page).use { cs ->
                    cs.drawImage(pdImg, 0f, 0f, PDRectangle.A4.width, PDRectangle.A4.height)
                }
                pageBitmap.recycle()
                currentLine = endLine
            }
            doc.save(outFile)
            ProcessedPdfResult(
                file = outFile,
                title = outFile.nameWithoutExtension,
                toolTag = "Text PDF",
                pageCount = doc.numberOfPages,
                fileSizeBytes = outFile.length()
            )
        }
    }

    suspend fun saveSignatureBitmap(bitmap: Bitmap): File = withContext(Dispatchers.IO) {
        val file = File(FileExportHelper.getSignaturesDir(context), "sig_${FileExportHelper.timestamp()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        file
    }

    private fun isWinAnsiSafe(text: String): Boolean {
        return text.all { ch -> ch.code in 32..126 }
    }

    private fun renderWatermarkTextToBitmap(text: String, colorInt: Int, opacity: Float): Bitmap {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(
                (opacity.coerceIn(0.1f, 0.9f) * 255).toInt(),
                Color.red(colorInt),
                Color.green(colorInt),
                Color.blue(colorInt)
            )
            textSize = 96f
            isFakeBoldText = true
        }
        val width = (paint.measureText(text) + 60f).toInt().coerceIn(200, 1400)
        val height = 160
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val yPos = height / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(text, 30f, yPos, paint)
        return bmp
    }
}
