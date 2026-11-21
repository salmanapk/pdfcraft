package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppPreferences
import com.example.data.local.CompressionLevel
import com.example.data.local.PdfCraftDatabase
import com.example.data.local.PdfCraftRepository
import com.example.data.local.PdfRecordEntity
import com.example.data.local.SignatureEntity
import com.example.engine.ExportedImagesResult
import com.example.engine.FileExportHelper
import com.example.engine.OcrEngine
import com.example.engine.OcrResult
import com.example.engine.PageEditSpec
import com.example.engine.PageNumberPosition
import com.example.engine.PageNumberStyle
import com.example.engine.PageSizePreset
import com.example.engine.PdfEnhanceEngine
import com.example.engine.PdfInspectInfo
import com.example.engine.PdfToolsEngine
import com.example.engine.ProcessedPdfResult
import com.example.engine.ScanFilterMode
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface ToolExecutionState {
    data object Idle : ToolExecutionState
    data class Working(val message: String) : ToolExecutionState
    data class PdfSuccess(val result: ProcessedPdfResult) : ToolExecutionState
    data class ImagesSuccess(val result: ExportedImagesResult) : ToolExecutionState
    data class Error(val message: String) : ToolExecutionState
}

data class ViewerTarget(
    val file: File,
    val title: String,
    val pageCount: Int
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val database = PdfCraftDatabase.getInstance(appContext)
    private val repository = PdfCraftRepository(database.pdfCraftDao())
    private val preferences = AppPreferences(appContext)

    val toolsEngine = PdfToolsEngine(appContext)
    val enhanceEngine = PdfEnhanceEngine(appContext)
    val ocrEngine = OcrEngine(appContext)

    val allRecords: StateFlow<List<PdfRecordEntity>> = repository.allRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedSignatures: StateFlow<List<SignatureEntity>> = repository.allSignatures
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val themeMode: StateFlow<ThemeMode> = preferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val defaultCompression: StateFlow<CompressionLevel> = preferences.defaultCompression
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CompressionLevel.MEDIUM)

    private val _executionState = MutableStateFlow<ToolExecutionState>(ToolExecutionState.Idle)
    val executionState: StateFlow<ToolExecutionState> = _executionState.asStateFlow()

    private val _viewerTarget = MutableStateFlow<ViewerTarget?>(null)
    val viewerTarget: StateFlow<ViewerTarget?> = _viewerTarget.asStateFlow()

    private val _storageUsedBytes = MutableStateFlow(0L)
    val storageUsedBytes: StateFlow<Long> = _storageUsedBytes.asStateFlow()

    init {
        refreshStorageStats()
    }

    fun clearExecutionState() {
        _executionState.value = ToolExecutionState.Idle
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun setDefaultCompression(level: CompressionLevel) {
        viewModelScope.launch { preferences.setDefaultCompression(level) }
    }

    fun refreshStorageStats() {
        viewModelScope.launch(Dispatchers.IO) {
            _storageUsedBytes.value = FileExportHelper.calculateWorkspaceBytes(appContext)
        }
    }

    fun clearTempCache() {
        viewModelScope.launch(Dispatchers.IO) {
            FileExportHelper.clearTempCache(appContext)
            _storageUsedBytes.value = FileExportHelper.calculateWorkspaceBytes(appContext)
        }
    }

    fun openFileInViewer(file: File, title: String, knownPages: Int = 0, onReady: () -> Unit) {
        viewModelScope.launch {
            if (!file.exists()) {
                _executionState.value = ToolExecutionState.Error("This PDF file is no longer on the device.")
                return@launch
            }
            val info = toolsEngine.inspectLocalFile(file)
            if (info.isPasswordProtected && info.pageCount == 0) {
                _executionState.value = ToolExecutionState.Error(
                    "This PDF is password protected. Use Lock & Unlock tool to unlock it first."
                )
                return@launch
            }
            _viewerTarget.value = ViewerTarget(
                file = file,
                title = title,
                pageCount = if (info.pageCount > 0) info.pageCount else knownPages.coerceAtLeast(1)
            )
            onReady()
        }
    }

    fun importAndOpenExternalPdf(uri: Uri, onReady: () -> Unit) {
        viewModelScope.launch {
            _executionState.value = ToolExecutionState.Working("Importing PDF…")
            runCatching {
                val imported = withContext(Dispatchers.IO) {
                    FileExportHelper.importUriToWorkspace(appContext, uri)
                }
                val info = toolsEngine.inspectLocalFile(imported)
                val record = PdfRecordEntity(
                    title = imported.nameWithoutExtension,
                    filePath = imported.absolutePath,
                    toolTag = "Imported",
                    pageCount = info.pageCount,
                    fileSizeBytes = imported.length(),
                    isProtected = info.isPasswordProtected
                )
                repository.insertRecord(record)
                refreshStorageStats()
                if (info.isPasswordProtected && info.pageCount == 0) {
                    _executionState.value = ToolExecutionState.Error(
                        "Imported to Files, but it is password protected. Unlock it with Lock & Unlock."
                    )
                } else {
                    _executionState.value = ToolExecutionState.Idle
                    _viewerTarget.value = ViewerTarget(imported, record.title, info.pageCount.coerceAtLeast(1))
                    onReady()
                }
            }.onFailure {
                _executionState.value = ToolExecutionState.Error(it.message ?: "Failed to import PDF.")
            }
        }
    }

    suspend fun inspectPdf(uri: Uri): Result<PdfInspectInfo> = runCatching {
        toolsEngine.inspectPdf(uri)
    }

    fun runMerge(uris: List<Uri>, outputName: String) {
        runPdfTask("Merging ${uris.size} PDFs…") {
            toolsEngine.mergePdfs(uris, outputName)
        }
    }

    fun runSplit(uri: Uri, zeroBasedPages: List<Int>, outputName: String) {
        runPdfTask("Extracting ${zeroBasedPages.size} pages…") {
            toolsEngine.splitPdf(uri, zeroBasedPages, outputName)
        }
    }

    fun runOrganize(uri: Uri, specs: List<PageEditSpec>, outputName: String) {
        runPdfTask("Applying page order & rotations…") {
            toolsEngine.organizePdf(uri, specs, outputName)
        }
    }

    fun runCompress(uri: Uri, level: CompressionLevel, outputName: String) {
        runPdfTask("Compressing PDF (${level.label})…") {
            toolsEngine.compressPdf(uri, level, outputName)
        }
    }

    fun runImagesToPdf(
        uris: List<Uri>,
        filterMode: ScanFilterMode,
        pageSize: PageSizePreset,
        addMargin: Boolean,
        outputName: String
    ) {
        runPdfTask("Creating PDF from ${uris.size} images…") {
            toolsEngine.imagesToPdf(uris, filterMode, pageSize, addMargin, outputName)
        }
    }

    fun runPdfToImages(uri: Uri, pages: List<Int>, asPng: Boolean) {
        viewModelScope.launch {
            _executionState.value = ToolExecutionState.Working("Rendering HD ${if (asPng) "PNG" else "JPG"} images…")
            runCatching {
                toolsEngine.pdfToImages(uri, pages, asPng)
            }.onSuccess { result ->
                _executionState.value = ToolExecutionState.ImagesSuccess(result)
            }.onFailure {
                _executionState.value = ToolExecutionState.Error(it.message ?: "Export failed.")
            }
        }
    }

    fun runWatermark(
        uri: Uri,
        text: String,
        opacity: Float,
        fontSize: Float,
        angle: Float,
        colorInt: Int,
        outputName: String
    ) {
        runPdfTask("Stamping watermark…") {
            enhanceEngine.addWatermark(uri, text, opacity, fontSize, angle, colorInt, outputName)
        }
    }

    fun runPageNumbers(
        uri: Uri,
        style: PageNumberStyle,
        position: PageNumberPosition,
        startNumber: Int,
        outputName: String
    ) {
        runPdfTask("Adding page numbers…") {
            enhanceEngine.addPageNumbers(uri, style, position, startNumber, outputName)
        }
    }

    fun runProtect(uri: Uri, password: String, outputName: String) {
        runPdfTask("Encrypting PDF (AES-128)…") {
            enhanceEngine.protectPdf(uri, password, outputName)
        }
    }

    fun runUnlock(uri: Uri, password: String, outputName: String) {
        runPdfTask("Removing PDF password…") {
            enhanceEngine.unlockPdf(uri, password, outputName)
        }
    }

    fun runSignPdf(
        uri: Uri,
        signatureBitmap: Bitmap,
        pageIndex: Int,
        normX: Float,
        normY: Float,
        widthFraction: Float,
        outputName: String
    ) {
        runPdfTask("Applying signature to page ${pageIndex + 1}…") {
            enhanceEngine.signPdf(uri, signatureBitmap, pageIndex, normX, normY, widthFraction, outputName)
        }
    }

    fun runTextToPdf(title: String, body: String, fontSizePt: Float, outputName: String) {
        runPdfTask("Typesetting PDF document…") {
            enhanceEngine.createTextPdf(title, body, fontSizePt, outputName)
        }
    }

    suspend fun runOcrOnImage(uri: Uri): Result<OcrResult> = runCatching {
        ocrEngine.extractTextFromImageUri(uri)
    }

    suspend fun runOcrOnPdf(uri: Uri, forceOptical: Boolean): Result<OcrResult> = runCatching {
        ocrEngine.extractTextFromPdfUri(uri, forceOpticalOcr = forceOptical)
    }

    fun saveSignature(bitmap: Bitmap, label: String) {
        viewModelScope.launch {
            runCatching {
                val file = enhanceEngine.saveSignatureBitmap(bitmap)
                repository.insertSignature(
                    SignatureEntity(
                        label = label.ifBlank { "My Signature" },
                        filePath = file.absolutePath
                    )
                )
            }
        }
    }

    fun deleteSignature(signature: SignatureEntity) {
        viewModelScope.launch { repository.deleteSignature(signature) }
    }

    fun toggleFavorite(record: PdfRecordEntity) {
        viewModelScope.launch { repository.toggleFavorite(record) }
    }

    fun renameRecord(record: PdfRecordEntity, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch { repository.renameRecord(record.id, newTitle.trim()) }
    }

    fun deleteRecord(record: PdfRecordEntity) {
        viewModelScope.launch {
            repository.deleteRecord(record)
            refreshStorageStats()
        }
    }

    private fun runPdfTask(label: String, block: suspend () -> ProcessedPdfResult) {
        viewModelScope.launch {
            _executionState.value = ToolExecutionState.Working(label)
            runCatching {
                val res = block()
                repository.insertRecord(
                    PdfRecordEntity(
                        title = res.title,
                        filePath = res.file.absolutePath,
                        toolTag = res.toolTag,
                        pageCount = res.pageCount,
                        fileSizeBytes = res.fileSizeBytes,
                        isProtected = res.isProtected
                    )
                )
                refreshStorageStats()
                res
            }.onSuccess { result ->
                _executionState.value = ToolExecutionState.PdfSuccess(result)
            }.onFailure { err ->
                _executionState.value = ToolExecutionState.Error(
                    err.message ?: "Operation failed. Please check the selected file."
                )
            }
        }
    }
}
