package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class OcrResult(
    val extractedText: String,
    val pagesProcessed: Int,
    val methodLabel: String
)

class OcrEngine(private val context: Context) {

    suspend fun extractTextFromImageUri(uri: Uri): OcrResult = withContext(Dispatchers.IO) {
        val image = InputImage.fromFilePath(context, uri)
        val text = recognizeInputImage(image)
        OcrResult(
            extractedText = text.trim(),
            pagesProcessed = 1,
            methodLabel = "On-Device ML Kit OCR"
        )
    }

    suspend fun extractTextFromPdfUri(
        uri: Uri,
        forceOpticalOcr: Boolean = false,
        maxPages: Int = 20
    ): OcrResult = withContext(Dispatchers.IO) {
        val temp = FileExportHelper.copyUriToTempFile(context, uri, "ocr_pdf")
        try {
            if (!forceOpticalOcr) {
                val embedded = runCatching {
                    PDDocument.load(temp).use { doc ->
                        val stripper = PDFTextStripper().apply {
                            startPage = 1
                            endPage = doc.numberOfPages.coerceAtMost(maxPages)
                        }
                        val text = stripper.getText(doc).trim()
                        if (text.length >= 20) {
                            OcrResult(
                                extractedText = text,
                                pagesProcessed = doc.numberOfPages.coerceAtMost(maxPages),
                                methodLabel = "Embedded PDF Text"
                            )
                        } else null
                    }
                }.getOrNull()
                if (embedded != null) return@withContext embedded
            }

            // Optical OCR via PdfRenderer -> ML Kit Text Recognition (100% offline!)
            val builder = StringBuilder()
            var count = 0
            ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    count = renderer.pageCount.coerceAtMost(maxPages)
                    for (i in 0 until count) {
                        renderer.openPage(i).use { page ->
                            val scale = 2.0f
                            val w = (page.width * scale).toInt().coerceIn(700, 1800)
                            val h = (page.height * scale).toInt().coerceIn(700, 2400)
                            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            Canvas(bmp).drawColor(Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            val pageText = recognizeBitmap(bmp).trim()
                            bmp.recycle()
                            if (pageText.isNotEmpty()) {
                                if (count > 1) builder.append("--- Page ${i + 1} ---\n")
                                builder.append(pageText).append("\n\n")
                            }
                        }
                    }
                }
            }
            OcrResult(
                extractedText = builder.toString().trim(),
                pagesProcessed = count,
                methodLabel = "On-Device ML Kit OCR"
            )
        } finally {
            temp.delete()
        }
    }

    private suspend fun recognizeBitmap(bitmap: Bitmap): String {
        val image = InputImage.fromBitmap(bitmap, 0)
        return recognizeInputImage(image)
    }

    private suspend fun recognizeInputImage(image: InputImage): String =
        suspendCancellableCoroutine { cont ->
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    recognizer.close()
                    if (cont.isActive) cont.resume(visionText.text)
                }
                .addOnFailureListener { e ->
                    recognizer.close()
                    if (cont.isActive) cont.resumeWithException(e)
                }
        }
}
