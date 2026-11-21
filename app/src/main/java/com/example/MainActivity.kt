package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.engine.FileExportHelper
import com.example.ui.components.ExecutionStateHost
import com.example.ui.navigation.Routes
import com.example.ui.screens.CompressPdfScreen
import com.example.ui.screens.FilesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MergePdfScreen
import com.example.ui.screens.OfflineOcrScreen
import com.example.ui.screens.OrganizeRotatePdfScreen
import com.example.ui.screens.PageNumbersPdfScreen
import com.example.ui.screens.PdfToImagesScreen
import com.example.ui.screens.PdfViewerScreen
import com.example.ui.screens.ProtectUnlockPdfScreen
import com.example.ui.screens.ScanImagesToPdfScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SignPdfScreen
import com.example.ui.screens.SplitPdfScreen
import com.example.ui.screens.TextToPdfScreen
import com.example.ui.screens.WatermarkPdfScreen
import com.example.ui.theme.PDFCraftTheme
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            PDFCraftTheme(themeMode = themeMode) {
                PDFCraftApp(viewModel = viewModel)
            }
        }
    }
}

private data class TopLevelDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val topDestinations = listOf(
    TopLevelDestination(
        route = Routes.STUDIO,
        labelRes = R.string.nav_studio,
        selectedIcon = Icons.Rounded.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard
    ),
    TopLevelDestination(
        route = Routes.FILES,
        labelRes = R.string.nav_files,
        selectedIcon = Icons.Rounded.Folder,
        unselectedIcon = Icons.Outlined.Folder
    ),
    TopLevelDestination(
        route = Routes.SETTINGS,
        labelRes = R.string.nav_settings,
        selectedIcon = Icons.Rounded.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
)

@Composable
fun PDFCraftApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val records by viewModel.allRecords.collectAsStateWithLifecycle()
    val savedSignatures by viewModel.savedSignatures.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val defaultCompression by viewModel.defaultCompression.collectAsStateWithLifecycle()
    val executionState by viewModel.executionState.collectAsStateWithLifecycle()
    val viewerTarget by viewModel.viewerTarget.collectAsStateWithLifecycle()
    val storageUsedBytes by viewModel.storageUsedBytes.collectAsStateWithLifecycle()

    var pendingExportFile by remember { mutableStateOf<File?>(null) }

    val saveCopyLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { targetUri ->
        val src = pendingExportFile
        if (targetUri != null && src != null && src.exists()) {
            val ok = FileExportHelper.copyFileToUri(context, src, targetUri)
            scope.launch {
                snackbarHostState.showSnackbar(
                    if (ok) "Saved copy to device storage" else "Could not save file copy"
                )
            }
        }
        pendingExportFile = null
    }

    val importPdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importAndOpenExternalPdf(uri) {
                navController.navigate(Routes.VIEWER)
            }
        }
    }

    fun triggerExportCopy(file: File, suggestedTitle: String) {
        pendingExportFile = file
        val cleanName = suggestedTitle.removeSuffix(".pdf") + ".pdf"
        saveCopyLauncher.launch(cleanName)
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showTopLevelNav = currentRoute in topDestinations.map { it.route }

    fun navigateTopTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp && showTopLevelNav

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showTopLevelNav && !useNavRail) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        topDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val label = stringResource(dest.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.statusBarsPadding()
                    ) {
                        topDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val label = stringResource(dest.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopTab(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = Routes.STUDIO,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(Routes.STUDIO) {
                            Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                                HomeScreen(
                                    recentRecords = records,
                                    onToolClick = { tool -> navController.navigate(tool.route) },
                                    onQuickImportPdf = { importPdfLauncher.launch(arrayOf("application/pdf")) },
                                    onOpenRecord = { rec ->
                                        viewModel.openFileInViewer(File(rec.filePath), rec.title, rec.pageCount) {
                                            navController.navigate(Routes.VIEWER)
                                        }
                                    },
                                    onViewAllFiles = { navigateTopTab(Routes.FILES) }
                                )
                            }
                        }

                        composable(Routes.FILES) {
                            Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                                FilesScreen(
                                    records = records,
                                    onImportExternalPdf = { importPdfLauncher.launch(arrayOf("application/pdf")) },
                                    onOpenRecord = { rec ->
                                        viewModel.openFileInViewer(File(rec.filePath), rec.title, rec.pageCount) {
                                            navController.navigate(Routes.VIEWER)
                                        }
                                    },
                                    onShareRecord = { rec ->
                                        val file = File(rec.filePath)
                                        if (file.exists()) FileExportHelper.sharePdfFile(context, file)
                                    },
                                    onSaveCopyRecord = { rec ->
                                        triggerExportCopy(File(rec.filePath), rec.title)
                                    },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onRenameRecord = { rec, newName -> viewModel.renameRecord(rec, newName) },
                                    onDeleteRecord = { viewModel.deleteRecord(it) }
                                )
                            }
                        }

                        composable(Routes.SETTINGS) {
                            Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                                SettingsScreen(
                                    themeMode = themeMode,
                                    defaultCompression = defaultCompression,
                                    storageUsedBytes = storageUsedBytes,
                                    savedSignatures = savedSignatures,
                                    onThemeChange = { viewModel.setThemeMode(it) },
                                    onCompressionChange = { viewModel.setDefaultCompression(it) },
                                    onSaveNewSignature = { bmp, label -> viewModel.saveSignature(bmp, label) },
                                    onDeleteSignature = { viewModel.deleteSignature(it) },
                                    onClearCache = {
                                        viewModel.clearTempCache()
                                        scope.launch { snackbarHostState.showSnackbar("Temporary cache cleared") }
                                    }
                                )
                            }
                        }

                        composable(Routes.VIEWER) {
                            PdfViewerScreen(
                                target = viewerTarget,
                                toolsEngine = viewModel.toolsEngine,
                                onBack = { navController.popBackStack() },
                                onShare = {
                                    viewerTarget?.let { FileExportHelper.sharePdfFile(context, it.file) }
                                },
                                onSaveCopy = {
                                    viewerTarget?.let { triggerExportCopy(it.file, it.title) }
                                }
                            )
                        }

                        composable(Routes.TOOL_MERGE) {
                            MergePdfScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                        }
                        composable(Routes.TOOL_SPLIT) {
                            SplitPdfScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                        }
                        composable(Routes.TOOL_ORGANIZE) {
                            OrganizeRotatePdfScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                        }
                        composable(Routes.TOOL_COMPRESS) {
                            CompressPdfScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                        }
                        composable(Routes.TOOL_SCAN_IMAGES) {
                            ScanImagesToPdfScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                        }
                        composable(Routes.TOOL_PDF_TO_IMAGES) {
                            PdfToImagesScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                        }
                        composable(Routes.TOOL_OCR) {
                            OfflineOcrScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                        }
                        composable(Routes.TOOL_SIGN) {
                            SignPdfScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                        }
                        composable(Routes.TOOL_WATERMARK) {
                            WatermarkPdfScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                        }
                        composable(Routes.TOOL_PAGE_NUMBERS) {
                            PageNumbersPdfScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                        }
                        composable(Routes.TOOL_SECURITY) {
                            ProtectUnlockPdfScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                        }
                        composable(Routes.TOOL_TEXT_TO_PDF) {
                            TextToPdfScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }

    ExecutionStateHost(
        state = executionState,
        onDismiss = { viewModel.clearExecutionState() },
        onOpenPdf = { res ->
            viewModel.clearExecutionState()
            viewModel.openFileInViewer(res.file, res.title, res.pageCount) {
                navController.navigate(Routes.VIEWER)
            }
        },
        onSharePdf = { res ->
            FileExportHelper.sharePdfFile(context, res.file)
        },
        onSavePdfCopy = { res ->
            triggerExportCopy(res.file, res.title)
        }
    )
}
