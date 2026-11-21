package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MergeType
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.engine.FileExportHelper
import com.example.engine.PageEditSpec
import com.example.engine.PdfInspectInfo
import com.example.engine.parsePageRangeInput
import com.example.ui.components.DashedPickerZone
import com.example.ui.components.SelectedPdfBanner
import com.example.ui.components.StudioToolTopBar
import com.example.ui.theme.ToolAccentOrganize
import com.example.ui.theme.VermilionCrimson
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

private data class MergeSourceItem(
    val uri: Uri,
    val info: PdfInspectInfo
)

@Composable
fun MergePdfScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<MergeSourceItem>>(emptyList()) }
    var outputName by remember { mutableStateOf("Merged_${FileExportHelper.timestamp().take(8)}") }
    var isLoadingPicks by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                isLoadingPicks = true
                val loaded = uris.mapNotNull { uri ->
                    viewModel.inspectPdf(uri).getOrNull()?.let { MergeSourceItem(uri, it) }
                }
                items = items + loaded
                isLoadingPicks = false
            }
        }
    }

    Scaffold(
        topBar = {
            StudioToolTopBar(
                title = "Merge PDF",
                subtitle = "Combine unlimited PDFs in exact order",
                accent = VermilionCrimson,
                onBack = onBack
            )
        },
        bottomBar = {
            if (items.isNotEmpty()) {
                Surface(
                    tonalElevation = 3.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
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
                            onClick = { viewModel.runMerge(items.map { it.uri }, outputName) },
                            enabled = items.size >= 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("merge_execute_btn")
                        ) {
                            Icon(Icons.Rounded.MergeType, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (items.size < 2) "Add at least 2 PDFs"
                                else "Merge ${items.size} PDFs (${items.sumOf { it.info.pageCount }} pages)"
                            )
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
                if (items.isEmpty()) {
                    DashedPickerZone(
                        title = "Select PDFs to Merge",
                        subtitle = "Pick 2 or more PDF files • Unlimited file size",
                        accent = VermilionCrimson,
                        tag = "merge_pick_zone",
                        onClick = { picker.launch(arrayOf("application/pdf")) }
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${items.size} files selected",
                            style = MaterialTheme.typography.titleMedium
                        )
                        FilledTonalButton(
                            onClick = { picker.launch(arrayOf("application/pdf")) },
                            modifier = Modifier.testTag("merge_add_more_btn")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add More")
                        }
                    }
                }
            }

            if (isLoadingPicks) {
                item {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            itemsIndexed(items, key = { idx, item -> "${item.uri}_$idx" }) { index, item ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(VermilionCrimson.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = VermilionCrimson
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.info.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${item.info.pageCount} pages • ${FileExportHelper.formatFileSize(item.info.sizeBytes)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            enabled = index > 0,
                            onClick = {
                                val mutable = items.toMutableList()
                                val moved = mutable.removeAt(index)
                                mutable.add(index - 1, moved)
                                items = mutable
                            }
                        ) {
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move up")
                        }
                        IconButton(
                            enabled = index < items.lastIndex,
                            onClick = {
                                val mutable = items.toMutableList()
                                val moved = mutable.removeAt(index)
                                mutable.add(index + 1, moved)
                                items = mutable
                            }
                        ) {
                            Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move down")
                        }
                        IconButton(
                            onClick = { items = items.filterIndexed { i, _ -> i != index } }
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Remove",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SplitPdfScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var inspectInfo by remember { mutableStateOf<PdfInspectInfo?>(null) }
    var thumbnails by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var selectedPages by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var rangeText by remember { mutableStateOf("") }
    var outputName by remember { mutableStateOf("Split_${FileExportHelper.timestamp().take(8)}") }
    var loadingThumbs by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedUri = uri
            scope.launch {
                loadingThumbs = true
                inspectInfo = viewModel.inspectPdf(uri).getOrNull()
                thumbnails = runCatching { viewModel.toolsEngine.renderUriThumbnails(uri, maxThumbnails = 36) }
                    .getOrDefault(emptyList())
                val count = inspectInfo?.pageCount ?: thumbnails.size
                selectedPages = if (count > 0) setOf(0) else emptySet()
                rangeText = if (count > 0) "1" else ""
                loadingThumbs = false
            }
        }
    }

    val effectivePages = remember(selectedPages, rangeText, inspectInfo) {
        val total = inspectInfo?.pageCount ?: thumbnails.size
        val fromRange = parsePageRangeInput(rangeText, total)
        if (fromRange.isNotEmpty()) fromRange else selectedPages.sorted()
    }

    Scaffold(
        topBar = {
            StudioToolTopBar(
                title = "Split & Extract",
                subtitle = "Extract chosen pages into a new PDF",
                accent = ToolAccentOrganize,
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
                                selectedUri?.let { uri ->
                                    viewModel.runSplit(uri, effectivePages, outputName)
                                }
                            },
                            enabled = effectivePages.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("split_execute_btn")
                        ) {
                            Text("Extract ${effectivePages.size} Page(s)")
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            if (selectedUri == null || inspectInfo == null) {
                DashedPickerZone(
                    title = "Select PDF to Split",
                    subtitle = "Choose specific pages or ranges (e.g. 1-3, 5, 8)",
                    accent = ToolAccentOrganize,
                    tag = "split_pick_zone",
                    onClick = { picker.launch(arrayOf("application/pdf")) }
                )
            } else {
                val info = inspectInfo!!
                SelectedPdfBanner(
                    fileName = info.displayName,
                    pageCount = info.pageCount,
                    sizeBytes = info.sizeBytes,
                    accent = ToolAccentOrganize,
                    isProtected = info.isPasswordProtected,
                    onChangeClick = { picker.launch(arrayOf("application/pdf")) }
                )

                OutlinedTextField(
                    value = rangeText,
                    onValueChange = {
                        rangeText = it
                        selectedPages = parsePageRangeInput(it, info.pageCount).toSet()
                    },
                    label = { Text("Page range (e.g. 1-3, 5)") },
                    supportingText = { Text("Or tap page thumbnails below to select") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("split_range_input")
                )

                if (loadingThumbs) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 104.dp),
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        itemsIndexed(thumbnails) { idx, bmp ->
                            val isChosen = selectedPages.contains(idx)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    width = if (isChosen) 2.5.dp else 1.dp,
                                    color = if (isChosen) ToolAccentOrganize else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        val next = if (isChosen) selectedPages - idx else selectedPages + idx
                                        selectedPages = next
                                        rangeText = next.sorted().joinToString(", ") { "${it + 1}" }
                                    }
                            ) {
                                Box(modifier = Modifier.aspectRatio(0.74f)) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Page ${idx + 1}",
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
                                    if (isChosen) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = ToolAccentOrganize,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .size(22.dp)
                                                .background(Color.White, CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class EditablePageCard(
    val uid: Int,
    val originalIndex: Int,
    val bitmap: Bitmap?,
    val rotation: Int = 0
)

@Composable
fun OrganizeRotatePdfScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var inspectInfo by remember { mutableStateOf<PdfInspectInfo?>(null) }
    var pages by remember { mutableStateOf<List<EditablePageCard>>(emptyList()) }
    var outputName by remember { mutableStateOf("Organized_${FileExportHelper.timestamp().take(8)}") }
    var loading by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedUri = uri
            scope.launch {
                loading = true
                val info = viewModel.inspectPdf(uri).getOrNull()
                inspectInfo = info
                val thumbs = runCatching { viewModel.toolsEngine.renderUriThumbnails(uri, maxThumbnails = 40) }
                    .getOrDefault(emptyList())
                val total = info?.pageCount ?: thumbs.size
                pages = (0 until total).map { i ->
                    EditablePageCard(uid = i, originalIndex = i, bitmap = thumbs.getOrNull(i))
                }
                loading = false
            }
        }
    }

    Scaffold(
        topBar = {
            StudioToolTopBar(
                title = "Organize & Rotate",
                subtitle = "Reorder, rotate 90°, or delete pages",
                accent = ToolAccentOrganize,
                onBack = onBack
            )
        },
        bottomBar = {
            if (selectedUri != null && pages.isNotEmpty()) {
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
                                selectedUri?.let { uri ->
                                    val specs = pages.map { PageEditSpec(it.originalIndex, it.rotation) }
                                    viewModel.runOrganize(uri, specs, outputName)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("organize_execute_btn")
                        ) {
                            Text("Save Organized PDF (${pages.size} Pages)")
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            if (selectedUri == null || inspectInfo == null) {
                DashedPickerZone(
                    title = "Select PDF to Organize",
                    subtitle = "Visually rotate, reorder, or remove unwanted pages",
                    accent = ToolAccentOrganize,
                    tag = "organize_pick_zone",
                    onClick = { picker.launch(arrayOf("application/pdf")) }
                )
            } else {
                val info = inspectInfo!!
                SelectedPdfBanner(
                    fileName = info.displayName,
                    pageCount = info.pageCount,
                    sizeBytes = info.sizeBytes,
                    accent = ToolAccentOrganize,
                    onChangeClick = { picker.launch(arrayOf("application/pdf")) }
                )

                if (loading) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 14.dp)
                    ) {
                        itemsIndexed(pages, key = { _, item -> item.uid }) { index, card ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(0.82f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (card.bitmap != null) {
                                            Image(
                                                bitmap = card.bitmap.asImageBitmap(),
                                                contentDescription = "Page ${card.originalIndex + 1}",
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .rotate(card.rotation.toFloat())
                                            )
                                        } else {
                                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null)
                                        }
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.7f),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                                        ) {
                                            Text(
                                                text = "#${index + 1} (${card.rotation}°)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        IconButton(
                                            enabled = index > 0,
                                            onClick = {
                                                val list = pages.toMutableList()
                                                val item = list.removeAt(index)
                                                list.add(index - 1, item)
                                                pages = list
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Move left")
                                        }
                                        IconButton(
                                            onClick = {
                                                pages = pages.mapIndexed { i, p ->
                                                    if (i == index) p.copy(rotation = (p.rotation + 90) % 360) else p
                                                }
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Rounded.RotateRight, contentDescription = "Rotate 90 degrees")
                                        }
                                        IconButton(
                                            enabled = index < pages.lastIndex,
                                            onClick = {
                                                val list = pages.toMutableList()
                                                val item = list.removeAt(index)
                                                list.add(index + 1, item)
                                                pages = list
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Move right")
                                        }
                                        IconButton(
                                            enabled = pages.size > 1,
                                            onClick = { pages = pages.filterIndexed { i, _ -> i != index } },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.Delete,
                                                contentDescription = "Delete page",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
