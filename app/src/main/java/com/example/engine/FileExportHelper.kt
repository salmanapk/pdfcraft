package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileExportHelper {

    fun getPdfStorageDir(context: Context): File {
        return File(context.filesDir, "pdfcraft").apply { if (!exists()) mkdirs() }
    }

    fun getSignaturesDir(context: Context): File {
        return File(context.filesDir, "signatures").apply { if (!exists()) mkdirs() }
    }

    fun getScansCacheDir(context: Context): File {
        return File(context.cacheDir, "scans").apply { if (!exists()) mkdirs() }
    }

    fun createOutputPdfFile(context: Context, rawName: String): File {
        val safeBase = rawName.trim()
            .removeSuffix(".pdf")
            .replace(Regex("[^a-zA-Z0-9._ -]"), "_")
            .ifBlank { "PDFCraft_${timestamp()}" }
        var candidate = File(getPdfStorageDir(context), "$safeBase.pdf")
        var counter = 1
        while (candidate.exists()) {
            candidate = File(getPdfStorageDir(context), "${safeBase}_$counter.pdf")
            counter++
        }
        return candidate
    }

    fun createCameraScanFile(context: Context): File {
        return File(getScansCacheDir(context), "scan_${timestamp()}.jpg")
    }

    fun getFileProviderUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun sharePdfFile(context: Context, file: File) {
        val uri = getFileProviderUri(context, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share PDF").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun shareText(context: Context, title: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share Extracted Text").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun copyFileToUri(context: Context, sourceFile: File, targetUri: Uri): Boolean {
        return runCatching {
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            }
            true
        }.getOrDefault(false)
    }

    fun copyUriToTempFile(context: Context, uri: Uri, prefix: String = "tmp_pdf"): File {
        val tempFile = File.createTempFile(prefix, ".pdf", context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Unable to read selected file.")
        return tempFile
    }

    fun importUriToWorkspace(context: Context, uri: Uri): File {
        val displayName = getDisplayName(context, uri)
        val destFile = createOutputPdfFile(context, displayName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Unable to import selected PDF.")
        return destFile
    }

    fun getDisplayName(context: Context, uri: Uri): String {
        if (uri.scheme == "file") {
            return uri.lastPathSegment ?: "document.pdf"
        }
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val name = cursor.getString(idx)
                        if (!name.isNullOrBlank()) return name
                    }
                }
            }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "Document_${timestamp()}.pdf"
    }

    fun getFileSize(context: Context, uri: Uri): Long {
        if (uri.scheme == "file") {
            return uri.path?.let { File(it).length() } ?: 0L
        }
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (idx >= 0) return cursor.getLong(idx)
                }
            }
        return 0L
    }

    fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        fileNameNoExt: String,
        asPng: Boolean
    ): Uri? {
        val mime = if (asPng) "image/png" else "image/jpeg"
        val ext = if (asPng) "png" else "jpg"
        val format = if (asPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        val quality = if (asPng) 100 else 92

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$fileNameNoExt.$ext")
                put(MediaStore.Images.Media.MIME_TYPE, mime)
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/PDFCraft"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(format, quality, out)
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            }
            uri
        } else {
            val dir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
            val file = File(dir, "$fileNameNoExt.$ext")
            FileOutputStream(file).use { bitmap.compress(format, quality, it) }
            getFileProviderUri(context, file)
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, 3)
        return DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble())) +
            " " + units[digitGroups]
    }

    fun calculateWorkspaceBytes(context: Context): Long {
        val pdfBytes = getPdfStorageDir(context).walkTopDown().filter { it.isFile }.sumOf { it.length() }
        val cacheBytes = context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        return pdfBytes + cacheBytes
    }

    fun clearTempCache(context: Context) {
        context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
    }

    fun timestamp(): String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
}
