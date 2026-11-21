package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine.FileExportHelper
import com.example.engine.PdfInspectInfo
import com.example.ui.components.DashedPickerZone
import com.example.ui.components.SelectedPdfBanner
import com.example.ui.components.SignaturePadCanvas
import com.example.ui.components.SignatureStroke
import com.example.ui.components.StudioToolTopBar
import com.example.ui.components.renderSignatureStrokesToBitmap
import com.example.ui.theme.ToolAccentOcr
import com.example.ui.theme.ToolAccentSign
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun OfflineOcrScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var extractedText by remember { mutableStateOf("") }
    var methodInfo by remember { mutableStateOf<String?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    var forceOpticalOcr by remember { mutableStateOf(false) }
    var statusBanner by remember { mutableStateOf<String?>(null) }
    var pendingCamUri by remember { mutableStateOf<Uri?>(null) }

    fun runImageOcr(uri: Uri) {
        scope.launch {
            isScanning = true
            statusBanner = null
            viewModel.runOcrOnImage(uri).onSuccess { res ->
                extractedText = res.extractedText
                methodInfo = "${res.methodLabel} • ${res.extractedText.length} chars"
                if (res.extractedText.isBlank()) {
                    statusBanner = "No readable text detected in this image."
                }
            }.onFailure {
                statusBanner = it.message ?: "OCR failed."
            }
            isScanning = false
        }
    }

    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                isScanning = true
                statusBanner = null
                viewModel.runOcrOnPdf(uri, forceOptical = forceOpticalOcr).onSuccess { res ->
                    extractedText = res.extractedText
                    methodInfo = "${res.methodLabel} • ${res.pagesProcessed} page(s)"
                    if (res.extractedText.isBlank()) {
                        statusBanner = "No text found. Try enabling Force Optical OCR for scanned pages."
                    }
                }.onFailure {
                    statusBanner = it.message ?: "OCR failed on this PDF."
                }
                isScanning = false
            }
        }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) runImageOcr(uri)
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) pendingCamUri?.let { runImageOcr(it) }
    }

    fun launchCam() {
        val file = FileExportHelper.createCameraScanFile(context)
        val uri = FileExportHelper.getFileProviderUri(context, file)
        pendingCamUri = uri
        takePicture.launch(uri)
    }

    val camPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) launchCam() else statusBanner = "Camera permission denied. Pick a PDF or photo instead."
    }

    Scaffold(
        topBar = {
            StudioToolTopBar(
                title = "Offline OCR Scanner",
                subtitle = "Extract text from scanned PDFs & photos — free",
                accent = ToolAccentOcr,
                onBack = onBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { pdfPicker.launch(arrayOf("application/pdf")) },
                        modifier = Modifier.weight(1f).testTag("ocr_pick_pdf_btn")
                    ) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("PDF")
                    }
                    FilledTonalButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f).testTag("ocr_pick_photo_btn")
                    ) {
                        Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Photo")
                    }
                    FilledTonalButton(
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) launchCam() else camPerm.launch(Manifest.permission.CAMERA)
                        },
                        modifier = Modifier.weight(1f).testTag("ocr_camera_btn")
                    ) {
                        Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Scan")
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Force Optical ML Kit OCR", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Always render PDF pages as images for scanned documents",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = forceOpticalOcr, onCheckedChange = { forceOpticalOcr = it })
                    }
                }
            }

            if (isScanning) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ToolAccentOcr.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(26.dp), color = ToolAccentOcr)
                            Column {
                                Text("Recognizing text offline…", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "On-device ML Kit engine running locally",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (statusBanner != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = statusBanner!!,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            if (methodInfo != null) {
                item {
                    Text(
                        text = methodInfo!!,
                        style = MaterialTheme.typography.labelMedium,
                        color = ToolAccentOcr
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = extractedText,
                    onValueChange = { extractedText = it },
                    label = { Text("Extracted editable text") },
                    placeholder = { Text("Pick a scanned PDF, photo, or camera capture above to extract text offline…") },
                    minLines = 10,
                    maxLines = 22,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("ocr_result_text")
                )
            }

            if (extractedText.isNotBlank()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("PDFCraft OCR", extractedText))
                                statusBanner = "Copied text to clipboard!"
                            },
                            modifier = Modifier.weight(1f).testTag("ocr_copy_btn")
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Copy")
                        }
                        OutlinedButton(
                            onClick = { FileExportHelper.shareText(context, "OCR Extract", extractedText) },
                            modifier = Modifier.weight(1f).testTag("ocr_share_btn")
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Share")
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            viewModel.runTextToPdf(
                                title = "OCR Document",
                                body = extractedText,
                                fontSizePt = 13f,
                                outputName = "OCR_${FileExportHelper.timestamp().take(8)}"
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("ocr_save_pdf_btn")
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save Extracted Text as New PDF")
                    }
                }
            }
        }
    }
}

@Composable
fun SignPdfScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val savedSignatures by viewModel.savedSignatures.collectAsStateWithLifecycle()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var inspectInfo by remember { mutableStateOf<PdfInspectInfo?>(null) }
    var pageIndex by remember { mutableIntStateOf(0) }
    var pagePreview by remember { mutableStateOf<Bitmap?>(null) }
    var loadingPage by remember { mutableStateOf(false) }

    var strokes by remember { mutableStateOf<List<SignatureStroke>>(emptyList()) }
    var inkColor by remember { mutableStateOf(Color(0xFF111827)) }
    var activeSigBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isDrawingMode by remember { mutableStateOf(true) }

    var normX by remember { mutableFloatStateOf(0.68f) }
    var normY by remember { mutableFloatStateOf(0.80f) }
    var sigWidthFraction by remember { mutableFloatStateOf(0.32f) }
    var outputName by remember { mutableStateOf("Signed_${FileExportHelper.timestamp().take(8)}") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedUri = uri
            pageIndex = 0
            scope.launch {
                inspectInfo = viewModel.inspectPdf(uri).getOrNull()
            }
        }
    }

    LaunchedEffect(selectedUri, pageIndex) {
        val uri = selectedUri ?: return@LaunchedEffect
        loadingPage = true
        val thumbs = runCatching {
            val temp = FileExportHelper.copyUriToTempFile(viewModel.getApplication(), uri, "sign_prev")
            try {
                viewModel.toolsEngine.renderFilePageBitmap(temp, pageIndex, targetWidthPx = 900)
            } finally {
                temp.delete()
            }
        }.getOrNull()
        pagePreview = thumbs
        loadingPage = false
    }

    Scaffold(
        topBar = {
            StudioToolTopBar(
                title = "Fill & Sign PDF",
                subtitle = "Draw signature & place directly on page",
                accent = ToolAccentSign,
                onBack = onBack
            )
        },
        bottomBar = {
            if (selectedUri != null && activeSigBitmap != null) {
                Surface(tonalElevation = 3.dp, modifier = Modifier.navigationBarsPadding()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val uri = selectedUri ?: return@Button
                                val bmp = activeSigBitmap ?: return@Button
                                viewModel.runSignPdf(
                                    uri = uri,
                                    signatureBitmap = bmp,
                                    pageIndex = pageIndex,
                                    normX = normX,
                                    normY = normY,
                                    widthFraction = sigWidthFraction,
                                    outputName = outputName
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("sign_execute_btn")
                        ) {
                            Icon(Icons.Rounded.Draw, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Stamp Signature on Page ${pageIndex + 1}")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                if (selectedUri == null || inspectInfo == null) {
                    DashedPickerZone(
                        title = "Select PDF to Sign",
                        subtitle = "Draw a handwritten signature or reuse a saved one",
                        icon = Icons.Rounded.Draw,
                        accent = ToolAccentSign,
                        tag = "sign_pick_zone",
                        onClick = { picker.launch(arrayOf("application/pdf")) }
                    )
                } else {
                    SelectedPdfBanner(
                        fileName = inspectInfo!!.displayName,
                        pageCount = inspectInfo!!.pageCount,
                        sizeBytes = inspectInfo!!.sizeBytes,
                        accent = ToolAccentSign,
                        onChangeClick = { picker.launch(arrayOf("application/pdf")) }
                    )
                }
            }

            if (selectedUri != null && inspectInfo != null) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = isDrawingMode,
                            onClick = { isDrawingMode = true },
                            label = { Text("1. Draw / Pick Signature") }
                        )
                        FilterChip(
                            selected = !isDrawingMode,
                            onClick = { if (activeSigBitmap != null) isDrawingMode = false },
                            enabled = activeSigBitmap != null,
                            label = { Text("2. Place on Page") }
                        )
                    }
                }

                if (isDrawingMode) {
                    if (savedSignatures.isNotEmpty()) {
                        item {
                            Text("Saved Signatures", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(savedSignatures, key = { it.id }) { sig ->
                                    val bmp = remember(sig.filePath) {
                                        runCatching { BitmapFactory.decodeFile(sig.filePath) }.getOrNull()
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.5.dp, ToolAccentSign.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .size(width = 124.dp, height = 64.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                if (bmp != null) {
                                                    activeSigBitmap = bmp
                                                    isDrawingMode = false
                                                }
                                            }
                                    ) {
                                        if (bmp != null) {
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = sig.label,
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.padding(6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text("Draw Signature", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(6.dp))
                        SignaturePadCanvas(
                            strokes = strokes,
                            currentColor = inkColor,
                            strokeWidthPx = 7f,
                            onStrokeFinished = { strokes = strokes + it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .testTag("sign_pad_canvas")
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    Color(0xFF111827) to "Ink",
                                    Color(0xFF1D4ED8) to "Blue",
                                    Color(0xFFC83226) to "Red"
                                ).forEach { (c, label) ->
                                    FilterChip(
                                        selected = inkColor == c,
                                        onClick = { inkColor = c },
                                        label = { Text(label) }
                                    )
                                }
                            }
                            TextButton(onClick = { strokes = emptyList() }) { Text("Clear") }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                enabled = strokes.isNotEmpty(),
                                onClick = {
                                    renderSignatureStrokesToBitmap(strokes)?.let { bmp ->
                                        viewModel.saveSignature(bmp, "Signature ${savedSignatures.size + 1}")
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save Reusable")
                            }
                            Button(
                                enabled = strokes.isNotEmpty(),
                                onClick = {
                                    renderSignatureStrokesToBitmap(strokes)?.let { bmp ->
                                        activeSigBitmap = bmp
                                        isDrawingMode = false
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("sign_use_drawn_btn")
                            ) {
                                Text("Place on PDF")
                            }
                        }
                    }
                } else {
                    item {
                        val total = inspectInfo!!.pageCount.coerceAtLeast(1)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tap or drag signature on page", style = MaterialTheme.typography.titleSmall)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(enabled = pageIndex > 0, onClick = { pageIndex-- }) {
                                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Prev")
                                }
                                Text("${pageIndex + 1}/$total", style = MaterialTheme.typography.labelMedium)
                                IconButton(enabled = pageIndex < total - 1, onClick = { pageIndex++ }) {
                                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next")
                                }
                            }
                        }
                    }

                    item {
                        val preview = pagePreview
                        val sigBmp = activeSigBitmap
                        if (loadingPage || preview == null) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(340.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            val pageAspect = preview.width.toFloat() / preview.height.coerceAtLeast(1)
                            val density = LocalDensity.current
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(pageAspect.coerceIn(0.55f, 1.4f))
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                    .pointerInput(Unit) {
                                        detectTapGestures { pos ->
                                            normX = (pos.x / size.width).coerceIn(0.1f, 0.9f)
                                            normY = (pos.y / size.height).coerceIn(0.08f, 0.92f)
                                        }
                                    }
                                    .pointerInput(Unit) {
                                        detectDragGestures { change, drag ->
                                            change.consume()
                                            normX = (normX + drag.x / size.width).coerceIn(0.1f, 0.9f)
                                            normY = (normY + drag.y / size.height).coerceIn(0.08f, 0.92f)
                                        }
                                    }
                            ) {
                                Image(
                                    bitmap = preview.asImageBitmap(),
                                    contentDescription = "PDF page preview",
                                    contentScale = ContentScale.FillBounds,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (sigBmp != null) {
                                    val boxW = with(density) { maxWidth.toPx() }
                                    val boxH = with(density) { maxHeight.toPx() }
                                    val sigAspect = sigBmp.height.toFloat() / sigBmp.width.coerceAtLeast(1)
                                    val sigW = boxW * sigWidthFraction
                                    val sigH = sigW * sigAspect
                                    val offsetX = (normX * boxW - sigW / 2f).roundToInt()
                                    val offsetY = (normY * boxH - sigH / 2f).roundToInt()

                                    Box(
                                        modifier = Modifier
                                            .offset { IntOffset(offsetX, offsetY) }
                                            .size(
                                                width = with(density) { sigW.toDp() },
                                                height = with(density) { sigH.toDp() }
                                            )
                                            .border(1.5.dp, ToolAccentSign, RoundedCornerShape(4.dp))
                                            .background(ToolAccentSign.copy(alpha = 0.08f))
                                    ) {
                                        Image(
                                            bitmap = sigBmp.asImageBitmap(),
                                            contentDescription = "Placed signature",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Signature Size", style = MaterialTheme.typography.titleSmall)
                                Text("${(sigWidthFraction * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
                            }
                            Slider(
                                value = sigWidthFraction,
                                onValueChange = { sigWidthFraction = it },
                                valueRange = 0.16f..0.60f
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = outputName,
                            onValueChange = { outputName = it },
                            label = { Text("Output file name") },
                            suffix = { Text(".pdf") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
