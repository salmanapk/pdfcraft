package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MergeType
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.ToolAccentConvert
import com.example.ui.theme.ToolAccentOcr
import com.example.ui.theme.ToolAccentOptimize
import com.example.ui.theme.ToolAccentOrganize
import com.example.ui.theme.ToolAccentSecurity
import com.example.ui.theme.ToolAccentSign
import com.example.ui.theme.VermilionCrimson

object Routes {
    const val STUDIO = "studio"
    const val FILES = "files"
    const val SETTINGS = "settings"
    const val VIEWER = "viewer"

    const val TOOL_MERGE = "tool_merge"
    const val TOOL_SPLIT = "tool_split"
    const val TOOL_ORGANIZE = "tool_organize"
    const val TOOL_COMPRESS = "tool_compress"
    const val TOOL_SCAN_IMAGES = "tool_scan_images"
    const val TOOL_PDF_TO_IMAGES = "tool_pdf_to_images"
    const val TOOL_OCR = "tool_ocr"
    const val TOOL_SIGN = "tool_sign"
    const val TOOL_WATERMARK = "tool_watermark"
    const val TOOL_PAGE_NUMBERS = "tool_page_numbers"
    const val TOOL_SECURITY = "tool_security"
    const val TOOL_TEXT_TO_PDF = "tool_text_to_pdf"
}

enum class ToolCategory(val label: String) {
    ALL("All Tools"),
    ORGANIZE("Organize"),
    CONVERT("Convert & Scan"),
    OPTIMIZE("Compress & OCR"),
    SIGN_SECURITY("Sign & Protect")
}

enum class PdfToolItem(
    val route: String,
    val title: String,
    val subtitle: String,
    val category: ToolCategory,
    val icon: ImageVector,
    val accent: Color,
    val badge: String? = null
) {
    MERGE(
        route = Routes.TOOL_MERGE,
        title = "Merge PDF",
        subtitle = "Combine unlimited PDFs",
        category = ToolCategory.ORGANIZE,
        icon = Icons.Rounded.MergeType,
        accent = VermilionCrimson
    ),
    SPLIT(
        route = Routes.TOOL_SPLIT,
        title = "Split & Extract",
        subtitle = "Pick pages or ranges",
        category = ToolCategory.ORGANIZE,
        icon = Icons.Rounded.CallSplit,
        accent = ToolAccentOrganize
    ),
    ORGANIZE(
        route = Routes.TOOL_ORGANIZE,
        title = "Organize & Rotate",
        subtitle = "Reorder, turn or delete",
        category = ToolCategory.ORGANIZE,
        icon = Icons.Rounded.GridView,
        accent = ToolAccentOrganize
    ),
    COMPRESS(
        route = Routes.TOOL_COMPRESS,
        title = "Compress PDF",
        subtitle = "Reduce MB offline",
        category = ToolCategory.OPTIMIZE,
        icon = Icons.Rounded.Compress,
        accent = ToolAccentOptimize,
        badge = "UNLIMITED"
    ),
    SCAN_IMAGES(
        route = Routes.TOOL_SCAN_IMAGES,
        title = "Scan & Photo PDF",
        subtitle = "Camera + B&W filters",
        category = ToolCategory.CONVERT,
        icon = Icons.Rounded.DocumentScanner,
        accent = ToolAccentConvert
    ),
    PDF_TO_IMAGES(
        route = Routes.TOOL_PDF_TO_IMAGES,
        title = "PDF to Images",
        subtitle = "Export HD JPG / PNG",
        category = ToolCategory.CONVERT,
        icon = Icons.Rounded.Image,
        accent = ToolAccentConvert
    ),
    OCR(
        route = Routes.TOOL_OCR,
        title = "Offline OCR",
        subtitle = "Scan PDF/photo to text",
        category = ToolCategory.OPTIMIZE,
        icon = Icons.Rounded.TextFields,
        accent = ToolAccentOcr,
        badge = "PRO FREE"
    ),
    SIGN(
        route = Routes.TOOL_SIGN,
        title = "Fill & Sign",
        subtitle = "Draw & place signature",
        category = ToolCategory.SIGN_SECURITY,
        icon = Icons.Rounded.Draw,
        accent = ToolAccentSign,
        badge = "PRO FREE"
    ),
    WATERMARK(
        route = Routes.TOOL_WATERMARK,
        title = "Watermark",
        subtitle = "Diagonal custom stamp",
        category = ToolCategory.SIGN_SECURITY,
        icon = Icons.Rounded.AutoFixHigh,
        accent = ToolAccentSign
    ),
    PAGE_NUMBERS(
        route = Routes.TOOL_PAGE_NUMBERS,
        title = "Page Numbers",
        subtitle = "Stamp footer numbering",
        category = ToolCategory.ORGANIZE,
        icon = Icons.Rounded.FormatListNumbered,
        accent = ToolAccentOptimize
    ),
    SECURITY(
        route = Routes.TOOL_SECURITY,
        title = "Lock & Unlock",
        subtitle = "AES password protect",
        category = ToolCategory.SIGN_SECURITY,
        icon = Icons.Rounded.Lock,
        accent = ToolAccentSecurity
    ),
    TEXT_TO_PDF(
        route = Routes.TOOL_TEXT_TO_PDF,
        title = "Text to PDF",
        subtitle = "Notes & Unicode docs",
        category = ToolCategory.CONVERT,
        icon = Icons.Rounded.Notes,
        accent = VermilionCrimson
    )
}
