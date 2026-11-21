package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.PdfRecordEntity
import com.example.engine.ExportedImagesResult
import com.example.engine.FileExportHelper
import com.example.engine.ProcessedPdfResult
import com.example.ui.theme.ToolAccentEmerald
import com.example.ui.viewmodel.ToolExecutionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioToolTopBar(
    title: String,
    subtitle: String,
    accent: Color,
    onBack: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        navigationIcon = {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("tool_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Navigate back"
                )
            }
        },
        actions = {
            Box(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
fun DashedPickerZone(
    title: String,
    subtitle: String,
    icon: ImageVector = Icons.Rounded.FolderOpen,
    accent: Color = MaterialTheme.colorScheme.primary,
    tag: String = "picker_zone",
    onClick: () -> Unit
) {
    val strokeColor = accent.copy(alpha = 0.55f)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .drawBehind {
                drawRoundRect(
                    color = strokeColor,
                    cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                    style = Stroke(
                        width = 1.6.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                    )
                )
            },
        color = accent.copy(alpha = 0.06f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accent,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SelectedPdfBanner(
    fileName: String,
    pageCount: Int,
    sizeBytes: Long,
    accent: Color,
    isProtected: Boolean = false,
    onChangeClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isProtected) Icons.Rounded.Lock else Icons.Rounded.PictureAsPdf,
                    contentDescription = "Selected PDF",
                    tint = accent
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                val meta = buildString {
                    if (pageCount > 0) append("$pageCount pages • ")
                    append(FileExportHelper.formatFileSize(sizeBytes))
                    if (isProtected) append(" • LOCKED")
                }
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(
                onClick = onChangeClick,
                modifier = Modifier.testTag("change_pdf_button")
            ) {
                Text("Change")
            }
        }
    }
}

@Composable
fun DocumentRecordCard(
    record: PdfRecordEntity,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onSaveCopy: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val dateLabel = remember(record.createdAt) {
        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(record.createdAt))
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen)
            .testTag("doc_record_${record.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (record.isProtected) Icons.Rounded.Lock else Icons.Rounded.Description,
                    contentDescription = "PDF document",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = record.toolTag.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "${record.pageCount}p • ${FileExportHelper.formatFileSize(record.fileSizeBytes)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.testTag("fav_btn_${record.id}")
            ) {
                Icon(
                    imageVector = if (record.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                    contentDescription = "Toggle favorite",
                    tint = if (record.isFavorite) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.testTag("more_btn_${record.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Document actions"
                    )
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Open in Viewer") },
                        leadingIcon = { Icon(Icons.Rounded.Visibility, contentDescription = null) },
                        onClick = { menuExpanded = false; onOpen() }
                    )
                    DropdownMenuItem(
                        text = { Text("Share PDF") },
                        leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                        onClick = { menuExpanded = false; onShare() }
                    )
                    DropdownMenuItem(
                        text = { Text("Save to Device") },
                        leadingIcon = { Icon(Icons.Rounded.Download, contentDescription = null) },
                        onClick = { menuExpanded = false; onSaveCopy() }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                        onClick = { menuExpanded = false; onRename() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = { menuExpanded = false; onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
fun ExecutionStateHost(
    state: ToolExecutionState,
    onDismiss: () -> Unit,
    onOpenPdf: (ProcessedPdfResult) -> Unit,
    onSharePdf: (ProcessedPdfResult) -> Unit,
    onSavePdfCopy: (ProcessedPdfResult) -> Unit
) {
    when (state) {
        ToolExecutionState.Idle -> Unit
        is ToolExecutionState.Working -> {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.5.dp
                        )
                        Spacer(Modifier.height(18.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Running 100% offline on device",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        is ToolExecutionState.PdfSuccess -> {
            val res = state.result
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Success",
                        tint = ToolAccentEmerald,
                        modifier = Modifier.size(40.dp)
                    )
                },
                title = {
                    Text(
                        text = "${res.toolTag} PDF Ready",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "${res.title}.pdf",
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${res.pageCount} pages • ${FileExportHelper.formatFileSize(res.fileSizeBytes)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (res.originalSizeBytes != null && res.originalSizeBytes > 0) {
                            val saved = res.originalSizeBytes - res.fileSizeBytes
                            val pct = ((saved.toDouble() / res.originalSizeBytes) * 100).toInt()
                            val label = if (saved > 0) {
                                "Saved ${FileExportHelper.formatFileSize(saved)} ($pct% smaller)"
                            } else {
                                "Optimized (${FileExportHelper.formatFileSize(res.originalSizeBytes)} → ${FileExportHelper.formatFileSize(res.fileSizeBytes)})"
                            }
                            Surface(
                                color = ToolAccentEmerald.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ToolAccentEmerald,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { onSharePdf(res) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("result_share_button")
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Share")
                            }
                            OutlinedButton(
                                onClick = { onSavePdfCopy(res) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("result_save_button")
                            ) {
                                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Save")
                            }
                        }
                    }
                },
                confirmButton = {
                    if (!res.isProtected) {
                        Button(
                            onClick = { onOpenPdf(res) },
                            modifier = Modifier.testTag("result_open_pdf_button")
                        ) {
                            Text("Open PDF")
                        }
                    } else {
                        Button(onClick = onDismiss) { Text("Done") }
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            )
        }
        is ToolExecutionState.ImagesSuccess -> {
            val res: ExportedImagesResult = state.result
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Exported",
                        tint = ToolAccentEmerald,
                        modifier = Modifier.size(40.dp)
                    )
                },
                title = { Text("Exported ${res.savedCount} ${res.formatLabel} Images") },
                text = {
                    Text(
                        "Saved to Pictures/PDFCraft in your device gallery at high resolution.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(onClick = onDismiss, modifier = Modifier.testTag("images_done_button")) {
                        Text("Done")
                    }
                }
            )
        }
        is ToolExecutionState.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = { Text("Couldn't Complete Task") },
                text = { Text(state.message, style = MaterialTheme.typography.bodyMedium) },
                confirmButton = {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

data class SignatureStroke(
    val points: List<Offset>,
    val color: Color,
    val widthPx: Float
)

@Composable
fun SignaturePadCanvas(
    strokes: List<SignatureStroke>,
    currentColor: Color,
    strokeWidthPx: Float,
    onStrokeFinished: (SignatureStroke) -> Unit,
    modifier: Modifier = Modifier
) {
    var activePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .pointerInput(currentColor, strokeWidthPx) {
                detectDragGestures(
                    onDragStart = { offset ->
                        activePoints = listOf(offset)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        activePoints = activePoints + change.position
                    },
                    onDragEnd = {
                        if (activePoints.size >= 2) {
                            onStrokeFinished(
                                SignatureStroke(activePoints, currentColor, strokeWidthPx)
                            )
                        }
                        activePoints = emptyList()
                    },
                    onDragCancel = { activePoints = emptyList() }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Subtle baseline guide
            val baseY = size.height * 0.78f
            drawLine(
                color = Color(0xFFE5E7EB),
                start = Offset(24f, baseY),
                end = Offset(size.width - 24f, baseY),
                strokeWidth = 2f
            )
            fun drawStroke(pts: List<Offset>, color: Color, width: Float) {
                if (pts.size < 2) return
                val path = Path().apply {
                    moveTo(pts.first().x, pts.first().y)
                    for (i in 1 until pts.size) {
                        lineTo(pts[i].x, pts[i].y)
                    }
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
            strokes.forEach { drawStroke(it.points, it.color, it.widthPx) }
            if (activePoints.size >= 2) {
                drawStroke(activePoints, currentColor, strokeWidthPx)
            }
        }
        if (strokes.isEmpty() && activePoints.isEmpty()) {
            Text(
                text = "Sign here with your finger",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF9CA3AF),
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

fun renderSignatureStrokesToBitmap(
    strokes: List<SignatureStroke>,
    canvasWidth: Int = 900,
    canvasHeight: Int = 420
): Bitmap? {
    val allPoints = strokes.flatMap { it.points }
    if (allPoints.isEmpty()) return null
    val minX = allPoints.minOf { it.x }
    val maxX = allPoints.maxOf { it.x }
    val minY = allPoints.minOf { it.y }
    val maxY = allPoints.maxOf { it.y }

    val pad = 28f
    val srcW = (maxX - minX).coerceAtLeast(40f)
    val srcH = (maxY - minY).coerceAtLeast(40f)
    val scale = minOf((canvasWidth - pad * 2) / srcW, (canvasHeight - pad * 2) / srcH)
    val outW = (srcW * scale + pad * 2).toInt().coerceIn(200, canvasWidth)
    val outH = (srcH * scale + pad * 2).toInt().coerceIn(100, canvasHeight)

    val bitmap = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    strokes.forEach { stroke ->
        if (stroke.points.size >= 2) {
            val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                color = stroke.color.toArgb()
                style = AndroidPaint.Style.STROKE
                strokeWidth = (stroke.widthPx * scale).coerceIn(4f, 18f)
                strokeCap = AndroidPaint.Cap.ROUND
                strokeJoin = AndroidPaint.Join.ROUND
            }
            val path = AndroidPath()
            stroke.points.forEachIndexed { idx, pt ->
                val tx = (pt.x - minX) * scale + pad
                val ty = (pt.y - minY) * scale + pad
                if (idx == 0) path.moveTo(tx, ty) else path.lineTo(tx, ty)
            }
            canvas.drawPath(path, paint)
        }
    }
    return bitmap
}

@Composable
fun SectionHeader(
    title: String,
    trailingAction: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        trailingAction?.invoke()
    }
}
