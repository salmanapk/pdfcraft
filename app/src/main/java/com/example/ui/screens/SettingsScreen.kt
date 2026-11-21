package com.example.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.CompressionLevel
import com.example.data.local.SignatureEntity
import com.example.engine.FileExportHelper
import com.example.ui.components.SignaturePadCanvas
import com.example.ui.components.SignatureStroke
import com.example.ui.components.renderSignatureStrokesToBitmap
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.ToolAccentEmerald

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    defaultCompression: CompressionLevel,
    storageUsedBytes: Long,
    savedSignatures: List<SignatureEntity>,
    onThemeChange: (ThemeMode) -> Unit,
    onCompressionChange: (CompressionLevel) -> Unit,
    onSaveNewSignature: (android.graphics.Bitmap, String) -> Unit,
    onDeleteSignature: (SignatureEntity) -> Unit,
    onClearCache: () -> Unit
) {
    var showNewSigDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text("Studio Settings", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Customize appearance, signatures, and offline storage",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            SettingsCard(title = "Appearance") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = themeMode == mode,
                            onClick = { onThemeChange(mode) },
                            label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            modifier = Modifier.testTag("theme_${mode.name.lowercase()}")
                        )
                    }
                }
            }
        }

        item {
            SettingsCard(title = "Default Compression Preset") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CompressionLevel.entries.forEach { level ->
                            FilterChip(
                                selected = defaultCompression == level,
                                onClick = { onCompressionChange(level) },
                                label = { Text(level.label) },
                                modifier = Modifier.testTag("preset_${level.name.lowercase()}")
                            )
                        }
                    }
                    Text(
                        text = defaultCompression.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            SettingsCard(
                title = "Saved Signatures (${savedSignatures.size})",
                trailing = {
                    FilledTonalButton(
                        onClick = { showNewSigDialog = true },
                        modifier = Modifier.testTag("settings_add_signature_btn")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New")
                    }
                }
            ) {
                if (savedSignatures.isEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Draw,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Save a handwritten signature once and reuse it on any PDF.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        savedSignatures.forEach { sig ->
                            val bmp = remember(sig.filePath) {
                                runCatching { BitmapFactory.decodeFile(sig.filePath) }.getOrNull()
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (bmp != null) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.White,
                                            modifier = Modifier.size(width = 92.dp, height = 46.dp)
                                        ) {
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = sig.label,
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.padding(4.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = sig.label,
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { onDeleteSignature(sig) }) {
                                        Icon(
                                            Icons.Rounded.Delete,
                                            contentDescription = "Delete signature",
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

        item {
            SettingsCard(title = "Device Storage") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = FileExportHelper.formatFileSize(storageUsedBytes),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Local PDFs & temporary scan cache",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = onClearCache,
                        modifier = Modifier.testTag("clear_cache_btn")
                    ) {
                        Icon(Icons.Rounded.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Clear Cache")
                    }
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ToolAccentEmerald.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, ToolAccentEmerald.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = "Privacy guarantee",
                        tint = ToolAccentEmerald
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "100% Offline Privacy Guarantee",
                            style = MaterialTheme.typography.titleMedium,
                            color = ToolAccentEmerald
                        )
                        Text(
                            text = "PDFCraft processes every PDF, OCR scan, password lock, and signature directly on your phone. Your documents never leave your device—no cloud uploads, no file limits, no ads.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    if (showNewSigDialog) {
        var label by remember { mutableStateOf("Primary Signature") }
        var strokes by remember { mutableStateOf<List<SignatureStroke>>(emptyList()) }
        var inkColor by remember { mutableStateOf(Color(0xFF111827)) }

        AlertDialog(
            onDismissRequest = { showNewSigDialog = false },
            title = { Text("Create Reusable Signature") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    SignaturePadCanvas(
                        strokes = strokes,
                        currentColor = inkColor,
                        strokeWidthPx = 7f,
                        onStrokeFinished = { strokes = strokes + it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(Color(0xFF111827), Color(0xFF1D4ED8), Color(0xFFC83226)).forEach { c ->
                                FilterChip(
                                    selected = inkColor == c,
                                    onClick = { inkColor = c },
                                    label = {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .background(c, RoundedCornerShape(50))
                                        )
                                    }
                                )
                            }
                        }
                        TextButton(onClick = { strokes = emptyList() }) { Text("Clear") }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = strokes.isNotEmpty(),
                    onClick = {
                        renderSignatureStrokesToBitmap(strokes)?.let { bmp ->
                            onSaveNewSignature(bmp, label)
                        }
                        showNewSigDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showNewSigDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                trailing?.invoke()
            }
            content()
        }
    }
}
