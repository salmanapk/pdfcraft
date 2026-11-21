package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.CompressionLevel
import com.example.engine.FileExportHelper
import com.example.engine.PageSizePreset
import com.example.engine.PdfInspectInfo
import com.example.engine.ScanFilterMode
import com.example.engine.parsePageRangeInput
import com.example.ui.components.DashedPickerZone
import com.example.ui.components.SelectedPdfBanner
import com.example.ui.components.StudioToolTopBar
import com.example.ui.theme.ToolAccentConvert
import com.example.ui.theme.ToolAccentOptimize
import com.example.ui.theme.VermilionCrimson
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun CompressPdfScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val defaultLevel by viewModel.defaultCompression.collectAsStateWithLifecycle()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var inspectInfo by remember { mutableStateOf<PdfInspectInfo?>(null) }
    var chosenLevel by remember(defaultLevel) { mutableStateOf(defaultLevel) }
    var outputName by remember { mutableStateOf("Compressed_${FileExportHelper.timestamp().take(8)}") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedUri = uri
            scope.launch { inspectInfo = viewModel.inspectPdf(uri).getOrNull() }
        }
    }

    Scaffold(
        topBar = {
            StudioToolTopBar(
                title = "Compress PDF",
                subtitle = "Shrink PDF size offline with no limits",
                accent = ToolAccentOptimize,
                onBack = onBack
            )
        },
        bottomBar = {
            if (selectedUri != null && inspectInfo != null) {
                Surface(tonalElevation = 3.dp, modifier = Modifier.navigationBarsPadding()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = outputName,
                            onValueChange = { outputName = it },
                            label = { Text("Output file name") },
                            suffix = { Text(".pdf") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                selectedUri?.let { viewModel.runCompress(it, chosenLevel, outputName) }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("compress_execute_btn")
                        ) {
                            Icon(Icons.Rounded.Compress, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Compress PDF (${chosenLevel.label})")
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
                        title = "Select PDF to Compress",
                        subtitle = "No file size cap • Processed locally on your phone",
                        accent = ToolAccentOptimize,
                        tag = "compress_pick_zone",
                        onClick = { picker.launch(arrayOf("application/pdf")) }
                    )
                } else {
                    val info = inspectInfo!!
                    SelectedPdfBanner(
                        fileName = info.displayName,
                        pageCount = info.pageCount,
                        sizeBytes = info.sizeBytes,
                        accent = ToolAccentOptimize,
                        isProtected = info.isPasswordProtected,
                        onChangeClick = { picker.launch(arrayOf("application/pdf")) }
                    )
                }
            }

            item {
                Text("Compression Level", style = MaterialTheme.typography.titleMedium)
            }

            items(CompressionLevel.entries) { level ->
                val isSelected = chosenLevel == level
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) ToolAccentOptimize.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) ToolAccentOptimize else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { chosenLevel = level }
                        .testTag("compress_level_${level.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) ToolAccentOptimize else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(level.label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                level.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Q${level.jpegQuality}",
                            style = MaterialTheme.typography.labelMedium,
                            color = ToolAccentOptimize
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScanImagesToPdfScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var filterMode by remember { mutableStateOf(ScanFilterMode.DOCUMENT_ENHANCE) }
    var pageSize by remember { mutableStateOf(PageSizePreset.A4) }
    var addMargin by remember { mutableStateOf(true) }
    var outputName by remember { mutableStateOf("Scan_${FileExportHelper.timestamp().take(8)}") }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var cameraNotice by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            imageUris = imageUris + uris
            cameraNotice = null
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingCameraUri?.let { imageUris = imageUris + it }
        }
    }

    fun launchCameraScan() {
        val file = FileExportHelper.createCameraScanFile(context)
        val uri = FileExportHelper.getFileProviderUri(context, file)
        pendingCameraUri = uri
        takePictureLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraNotice = null
            launchCameraScan()
        } else {
            cameraNotice = "Camera access denied. You can still pick photos from your gallery."
        }
    }

    fun requestOrLaunchCamera() {
        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (hasPerm) launchCameraScan() else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        topBar = {
            StudioToolTopBar(
                title = "Scan & Photo to PDF",
                subtitle = "Camera scanner + Photo Picker + B&W filters",
                accent = ToolAccentConvert,
                onBack = onBack
            )
        },
        bottomBar = {
            if (imageUris.isNotEmpty()) {
                Surface(tonalElevation = 3.dp, modifier = Modifier.navigationBarsPadding()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = outputName,
                            onValueChange = { outputName = it },
                            label = { Text("Output PDF name") },
                            suffix = { Text(".pdf") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                viewModel.runImagesToPdf(
                                    uris = imageUris,
                                    filterMode = filterMode,
                                    pageSize = pageSize,
                                    addMargin = addMargin,
                                    outputName = outputName
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("images_to_pdf_execute_btn")
                        ) {
                            Text("Create PDF (${imageUris.size} ${if (imageUris.size == 1) "Page" else "Pages"})")
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = { requestOrLaunchCamera() },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("scan_camera_btn")
                    ) {
                        Icon(Icons.Rounded.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Camera Scan")
                    }
                    Button(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("scan_gallery_btn")
                    ) {
                        Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Pick Photos")
                    }
                }
            }

            if (cameraNotice != null) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = cameraNotice!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            if (imageUris.isEmpty()) {
                item {
                    DashedPickerZone(
                        title = "Scan Documents or Select Photos",
                        subtitle = "Zero storage permissions needed • Combine unlimited pages",
                        icon = Icons.Rounded.AddPhotoAlternate,
                        accent = ToolAccentConvert,
                        tag = "scan_drop_zone",
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                }
            } else {
                item {
                    Text("Pages (${imageUris.size})", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(imageUris) { idx, uri ->
                            Box(
                                modifier = Modifier
                                    .width(120.dp)
                                    .aspectRatio(0.75f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Selected page ${idx + 1}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Surface(
                                    color = Color.Black.copy(alpha = 0.65f),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
                                ) {
                                    Text(
                                        text = "P${idx + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { imageUris = imageUris.filterIndexed { i, _ -> i != idx } },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(26.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Remove image",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Document Filter", style = MaterialTheme.typography.titleMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ScanFilterMode.entries) { mode ->
                            FilterChip(
                                selected = filterMode == mode,
                                onClick = { filterMode = mode },
                                label = { Text(mode.label) }
                            )
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Page Size", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PageSizePreset.entries.forEach { preset ->
                            FilterChip(
                                selected = pageSize == preset,
                                onClick = { pageSize = preset },
                                label = { Text(preset.label) }
                            )
                        }
                    }
                }
            }

            if (pageSize != PageSizePreset.FIT) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Add Clean Margins", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "White border around each scanned page",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = addMargin, onCheckedChange = { addMargin = it })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PdfToImagesScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var inspectInfo by remember { mutableStateOf<PdfInspectInfo?>(null) }
    var asPng by remember { mutableStateOf(false) }
    var rangeText by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedUri = uri
            scope.launch {
                val info = viewModel.inspectPdf(uri).getOrNull()
                inspectInfo = info
                rangeText = if (info != null && info.pageCount > 0) "1-${info.pageCount}" else ""
            }
        }
    }

    Scaffold(
        topBar = {
            StudioToolTopBar(
                title = "PDF to Images",
                subtitle = "Export crisp JPG or PNG pages to gallery",
                accent = ToolAccentConvert,
                onBack = onBack
            )
        },
        bottomBar = {
            if (selectedUri != null && inspectInfo != null) {
                Surface(tonalElevation = 3.dp, modifier = Modifier.navigationBarsPadding()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Button(
                            onClick = {
                                val total = inspectInfo!!.pageCount
                                val pages = parsePageRangeInput(rangeText, total)
                                selectedUri?.let { viewModel.runPdfToImages(it, pages, asPng) }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("pdf_to_img_execute_btn")
                        ) {
                            Icon(Icons.Rounded.Image, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Export as ${if (asPng) "PNG" else "JPG"}")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (selectedUri == null || inspectInfo == null) {
                DashedPickerZone(
                    title = "Select PDF to Convert",
                    subtitle = "Save every page as high-resolution JPG or PNG",
                    accent = ToolAccentConvert,
                    tag = "pdf_to_img_pick_zone",
                    onClick = { picker.launch(arrayOf("application/pdf")) }
                )
            } else {
                val info = inspectInfo!!
                SelectedPdfBanner(
                    fileName = info.displayName,
                    pageCount = info.pageCount,
                    sizeBytes = info.sizeBytes,
                    accent = ToolAccentConvert,
                    onChangeClick = { picker.launch(arrayOf("application/pdf")) }
                )

                Text("Image Format", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = !asPng,
                        onClick = { asPng = false },
                        label = { Text("JPG (Compact)") }
                    )
                    FilterChip(
                        selected = asPng,
                        onClick = { asPng = true },
                        label = { Text("PNG (Lossless)") }
                    )
                }

                OutlinedTextField(
                    value = rangeText,
                    onValueChange = { rangeText = it },
                    label = { Text("Pages to export (blank = all pages)") },
                    placeholder = { Text("e.g. 1-5, 7") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun TextToPdfScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var fontSize by remember { mutableFloatStateOf(14f) }
    var outputName by remember { mutableStateOf("Note_${FileExportHelper.timestamp().take(8)}") }

    Scaffold(
        topBar = {
            StudioToolTopBar(
                title = "Text to PDF",
                subtitle = "Supports English, Bangla & full Unicode",
                accent = VermilionCrimson,
                onBack = onBack
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, modifier = Modifier.navigationBarsPadding()) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Button(
                        onClick = { viewModel.runTextToPdf(title, body, fontSize, outputName) },
                        enabled = body.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("text_to_pdf_execute_btn")
                    ) {
                        Icon(Icons.Rounded.Notes, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Generate A4 PDF")
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
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Document heading (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("text_pdf_title_input")
                )
            }
            item {
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Write or paste your text…") },
                    minLines = 8,
                    maxLines = 16,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("text_pdf_body_input")
                )
            }
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Font Size", style = MaterialTheme.typography.titleSmall)
                        Text("${fontSize.roundToInt()} pt", style = MaterialTheme.typography.labelMedium)
                    }
                    Slider(
                        value = fontSize,
                        onValueChange = { fontSize = it },
                        valueRange = 10f..24f
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
