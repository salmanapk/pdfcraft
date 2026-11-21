package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.engine.FileExportHelper
import com.example.engine.PageNumberPosition
import com.example.engine.PageNumberStyle
import com.example.engine.PdfInspectInfo
import com.example.ui.components.DashedPickerZone
import com.example.ui.components.SelectedPdfBanner
import com.example.ui.components.StudioToolTopBar
import com.example.ui.theme.ToolAccentOptimize
import com.example.ui.theme.ToolAccentSecurity
import com.example.ui.theme.ToolAccentSign
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun WatermarkPdfScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var inspectInfo by remember { mutableStateOf<PdfInspectInfo?>(null) }
    var watermarkText by remember { mutableStateOf("CONFIDENTIAL") }
    var opacity by remember { mutableFloatStateOf(0.28f) }
    var fontSize by remember { mutableFloatStateOf(44f) }
    var angle by remember { mutableFloatStateOf(45f) }
    var color by remember { mutableStateOf(Color(0xFFC83226)) }
    var outputName by remember { mutableStateOf("Watermarked_${FileExportHelper.timestamp().take(8)}") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedUri = uri
            scope.launch { inspectInfo = viewModel.inspectPdf(uri).getOrNull() }
        }
    }

    Scaffold(
        topBar = {
            StudioToolTopBar(
                title = "Watermark PDF",
                subtitle = "Stamp custom text across every page",
                accent = ToolAccentSign,
                onBack = onBack
            )
        },
        bottomBar = {
            if (selectedUri != null) {
                Surface(tonalElevation = 3.dp, modifier = Modifier.navigationBarsPadding()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Button(
                            onClick = {
                                selectedUri?.let { uri ->
                                    viewModel.runWatermark(
                                        uri = uri,
                                        text = watermarkText,
                                        opacity = opacity,
                                        fontSize = fontSize,
                                        angle = angle,
                                        colorInt = color.toArgb(),
                                        outputName = outputName
                                    )
                                }
                            },
                            enabled = watermarkText.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("watermark_execute_btn")
                        ) {
                            Icon(Icons.Rounded.AutoFixHigh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Apply Watermark")
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
                if (selectedUri == null || inspectInfo == null) {
                    DashedPickerZone(
                        title = "Select PDF to Watermark",
                        subtitle = "Supports diagonal stamp, opacity, color & Unicode",
                        icon = Icons.Rounded.AutoFixHigh,
                        accent = ToolAccentSign,
                        tag = "watermark_pick_zone",
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

            item {
                OutlinedTextField(
                    value = watermarkText,
                    onValueChange = { watermarkText = it },
                    label = { Text("Watermark text") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("watermark_text_input")
                )
            }

            item {
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Opacity", style = MaterialTheme.typography.titleSmall)
                        Text("${(opacity * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
                    }
                    Slider(value = opacity, onValueChange = { opacity = it }, valueRange = 0.10f..0.80f)
                }
            }

            item {
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Font Size", style = MaterialTheme.typography.titleSmall)
                        Text("${fontSize.roundToInt()} pt", style = MaterialTheme.typography.labelMedium)
                    }
                    Slider(value = fontSize, onValueChange = { fontSize = it }, valueRange = 22f..76f)
                }
            }

            item {
                Text("Angle", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(45f to "Diagonal ↗", 0f to "Horizontal", -45f to "Diagonal ↘").forEach { (deg, label) ->
                        FilterChip(
                            selected = angle == deg,
                            onClick = { angle = deg },
                            label = { Text(label) }
                        )
                    }
                }
            }

            item {
                Text("Stamp Color", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Color(0xFFC83226) to "Red",
                        Color(0xFF4B5563) to "Gray",
                        Color(0xFF2563EB) to "Blue",
                        Color(0xFF111827) to "Black"
                    ).forEach { (c, label) ->
                        FilterChip(
                            selected = color == c,
                            onClick = { color = c },
                            label = { Text(label) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(c, RoundedCornerShape(50))
                                )
                            }
                        )
                    }
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

@Composable
fun PageNumbersPdfScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var inspectInfo by remember { mutableStateOf<PdfInspectInfo?>(null) }
    var style by remember { mutableStateOf(PageNumberStyle.PAGE_OF_TOTAL) }
    var position by remember { mutableStateOf(PageNumberPosition.BOTTOM_CENTER) }
    var startNumText by remember { mutableStateOf("1") }
    var outputName by remember { mutableStateOf("Numbered_${FileExportHelper.timestamp().take(8)}") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedUri = uri
            scope.launch { inspectInfo = viewModel.inspectPdf(uri).getOrNull() }
        }
    }

    Scaffold(
        topBar = {
            StudioToolTopBar(
                title = "Page Numbers",
                subtitle = "Stamp clean footer or header numbers",
                accent = ToolAccentOptimize,
                onBack = onBack
            )
        },
        bottomBar = {
            if (selectedUri != null) {
                Surface(tonalElevation = 3.dp, modifier = Modifier.navigationBarsPadding()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Button(
                            onClick = {
                                selectedUri?.let { uri ->
                                    val startAt = startNumText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                                    viewModel.runPageNumbers(uri, style, position, startAt, outputName)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("page_numbers_execute_btn")
                        ) {
                            Icon(Icons.Rounded.FormatListNumbered, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Stamp Page Numbers")
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
                if (selectedUri == null || inspectInfo == null) {
                    DashedPickerZone(
                        title = "Select PDF to Number",
                        subtitle = "Automatically stamps sequential numbering on all pages",
                        icon = Icons.Rounded.FormatListNumbered,
                        accent = ToolAccentOptimize,
                        tag = "page_num_pick_zone",
                        onClick = { picker.launch(arrayOf("application/pdf")) }
                    )
                } else {
                    SelectedPdfBanner(
                        fileName = inspectInfo!!.displayName,
                        pageCount = inspectInfo!!.pageCount,
                        sizeBytes = inspectInfo!!.sizeBytes,
                        accent = ToolAccentOptimize,
                        onChangeClick = { picker.launch(arrayOf("application/pdf")) }
                    )
                }
            }

            item {
                Text("Number Format", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PageNumberStyle.entries.forEach { s ->
                        FilterChip(
                            selected = style == s,
                            onClick = { style = s },
                            label = { Text(s.label) }
                        )
                    }
                }
            }

            item {
                Text("Placement", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PageNumberPosition.entries.forEach { pos ->
                        FilterChip(
                            selected = position == pos,
                            onClick = { position = pos },
                            label = { Text(pos.label) }
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = startNumText,
                    onValueChange = { if (it.all(Char::isDigit)) startNumText = it },
                    label = { Text("Start numbering from") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
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

@Composable
fun ProtectUnlockPdfScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isLockMode by remember { mutableStateOf(true) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var inspectInfo by remember { mutableStateOf<PdfInspectInfo?>(null) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var outputName by remember { mutableStateOf("Secured_${FileExportHelper.timestamp().take(8)}") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedUri = uri
            scope.launch {
                val info = viewModel.inspectPdf(uri).getOrNull()
                inspectInfo = info
                if (info?.isPasswordProtected == true) {
                    isLockMode = false
                    outputName = "Unlocked_${FileExportHelper.timestamp().take(8)}"
                }
            }
        }
    }

    val canSubmit = selectedUri != null && if (isLockMode) {
        password.length >= 4 && password == confirmPassword
    } else {
        password.isNotEmpty()
    }

    Scaffold(
        topBar = {
            StudioToolTopBar(
                title = "Lock & Unlock PDF",
                subtitle = "128-bit AES encryption offline",
                accent = ToolAccentSecurity,
                onBack = onBack
            )
        },
        bottomBar = {
            if (selectedUri != null) {
                Surface(tonalElevation = 3.dp, modifier = Modifier.navigationBarsPadding()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Button(
                            onClick = {
                                val uri = selectedUri ?: return@Button
                                if (isLockMode) {
                                    viewModel.runProtect(uri, password, outputName)
                                } else {
                                    viewModel.runUnlock(uri, password, outputName)
                                }
                            },
                            enabled = canSubmit,
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("security_execute_btn")
                        ) {
                            Icon(
                                if (isLockMode) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(if (isLockMode) "Encrypt & Lock PDF" else "Remove Password & Unlock")
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
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = isLockMode,
                        onClick = {
                            isLockMode = true
                            outputName = "Locked_${FileExportHelper.timestamp().take(8)}"
                        },
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Protect / Lock") },
                        modifier = Modifier.testTag("mode_lock_chip")
                    )
                    FilterChip(
                        selected = !isLockMode,
                        onClick = {
                            isLockMode = false
                            outputName = "Unlocked_${FileExportHelper.timestamp().take(8)}"
                        },
                        leadingIcon = { Icon(Icons.Rounded.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Unlock PDF") },
                        modifier = Modifier.testTag("mode_unlock_chip")
                    )
                }
            }

            item {
                if (selectedUri == null || inspectInfo == null) {
                    DashedPickerZone(
                        title = if (isLockMode) "Select PDF to Lock" else "Select Protected PDF",
                        subtitle = if (isLockMode) "Encrypt with a password using AES-128" else "Enter the known password to create an unlocked copy",
                        icon = if (isLockMode) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                        accent = ToolAccentSecurity,
                        tag = "security_pick_zone",
                        onClick = { picker.launch(arrayOf("application/pdf")) }
                    )
                } else {
                    SelectedPdfBanner(
                        fileName = inspectInfo!!.displayName,
                        pageCount = inspectInfo!!.pageCount,
                        sizeBytes = inspectInfo!!.sizeBytes,
                        accent = ToolAccentSecurity,
                        isProtected = inspectInfo!!.isPasswordProtected,
                        onChangeClick = { picker.launch(arrayOf("application/pdf")) }
                    )
                }
            }

            if (selectedUri != null) {
                item {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(if (isLockMode) "New password (min 4 chars)" else "Current PDF password") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = "Toggle visibility"
                                )
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("security_password_input")
                    )
                }

                if (isLockMode) {
                    item {
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirm password") },
                            isError = confirmPassword.isNotEmpty() && confirmPassword != password,
                            supportingText = {
                                if (confirmPassword.isNotEmpty() && confirmPassword != password) {
                                    Text("Passwords do not match")
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().testTag("security_confirm_input")
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
