package com.pixelados.ui.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.provider.MediaStore
import android.widget.Toast
import java.io.File
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material.icons.rounded.FormatColorFill
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Redo
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.ZoomOutMap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pixelados.R
import com.pixelados.data.model.Stamp
import com.pixelados.data.model.StampStyle
import com.pixelados.data.model.Tool
import com.pixelados.ui.components.CellAction
import com.pixelados.ui.components.ColorPalettePanel
import com.pixelados.ui.components.PixelCanvasView
import com.pixelados.ui.components.SimpleColorPickerDialog
import com.pixelados.ui.theme.AppTheme
import com.pixelados.ui.viewmodel.EditorViewModel

/** Pasos de zoom de los botones (los mismos a los que se imanta el gesto). */
private val ZOOM_STEPS = listOf(1f, 1.5f, 2f, 3f, 4f, 6f, 8f)

/**
 * Editor de pixel art.
 *
 * SIEMPRE igual de predecible:
 *  - 1 dedo pinta (nunca se bloquea).
 *  - 2 dedos mueven y amplían, en cualquier momento (con o sin candado).
 *  - La franja inferior cambia de contenido pero NUNCA de tamaño: las filas de
 *    botones no se desplazan al elegir una herramienta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    navController: NavController,
    width: Int = 32,
    height: Int = 32,
    fromPhoto: Boolean = false
) {
    val theme = AppTheme.colors
    val context = LocalContext.current
    val canvas by viewModel.canvas.collectAsState()

    val currentTool = viewModel.currentTool
    val activeColorInt = viewModel.activeColor
    val canUndo = viewModel.canUndo
    val canRedo = viewModel.canRedo
    val showGrid = viewModel.showGrid
    val showReference = viewModel.showReference
    val referenceOpacity = viewModel.referenceOpacity
    val mirrorHorizontal = viewModel.mirrorHorizontal
    val mirrorVertical = viewModel.mirrorVertical
    val selectionStart = viewModel.selectionStart
    val selectionEnd = viewModel.selectionEnd
    val activeStamp = viewModel.activeStamp
    val referenceBitmap = viewModel.referenceBitmap
    val savedColors = viewModel.savedColors.collectAsState(initial = emptyList())
    val autoSaveStatus = viewModel.autoSaveStatus.collectAsState(initial = "")
    val autoSaveTick = viewModel.autoSaveTick.collectAsState(initial = 0)

    LaunchedEffect(autoSaveTick.value) {
        if (autoSaveTick.value > 0) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearAutoSaveStatus()
        }
    }

    var zoom by rememberSaveable { mutableStateOf(1f) }
    var fitSignal by remember { mutableIntStateOf(0) }
    var showColorPalette by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showReplaceDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var showStamps by remember { mutableStateOf(false) }
    var showCustomStampColor by remember { mutableStateOf(false) }
    var stampCategory by remember { mutableStateOf<String?>(null) }

    // Elegir una foto para usarla como capa de referencia (calcar).
    val pickReference = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.loadReferenceFromUri(it.toString(), context) }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.saveOnExit() }
    }

    Scaffold(
        topBar = {
            EditorTopBar(
                canvasName = canvas.name,
                autoSaveStatus = autoSaveStatus.value,
                canUndo = canUndo,
                canRedo = canRedo,
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onSaveClick = {
                    viewModel.saveProject()
                    Toast.makeText(context, "Dibujo guardado", Toast.LENGTH_SHORT).show()
                },
                onExportClick = { showExportDialog = true },
                onHelpClick = { showHelp = true },
                onBackClick = { navController.popBackStack() }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(theme.bar)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                ActionRow(
                    activeColor = activeColorInt,
                    showGrid = showGrid,
                    showReference = showReference,
                    referenceAvailable = referenceBitmap != null,
                    mirrorHorizontal = mirrorHorizontal,
                    mirrorVertical = mirrorVertical,
                    onOpenPalette = { showColorPalette = true },
                    onToggleGrid = { viewModel.toggleGrid() },
                    onToggleReference = { if (referenceBitmap != null) viewModel.toggleReference() },
                    onPickReference = {
                        pickReference.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onToggleMirrorH = { viewModel.toggleMirrorHorizontal() },
                    onToggleMirrorV = { viewModel.toggleMirrorVertical() },
                    onReplaceColor = { showReplaceDialog = true },
                    onClearAll = { showClearConfirm = true }
                )

                ToolRow(
                    currentTool = currentTool,
                    onToolSelected = { tool ->
                        viewModel.setTool(tool)
                        if (tool == Tool.STAMPS) showStamps = true
                    }
                )

                OptionsStrip(
                    currentTool = currentTool,
                    activeStamp = activeStamp,
                    activeColor = activeColorInt,
                    stampStyle = viewModel.stampStyle,
                    onOpenStamps = { showStamps = true },
                    hasSelection = selectionStart != null && selectionEnd != null,
                    onEraseSelection = { viewModel.eraseSelection() },
                    onClearSelection = { viewModel.clearSelection() },
                    mirrorHorizontal = mirrorHorizontal,
                    mirrorVertical = mirrorVertical,
                    onToggleMirrorH = { viewModel.toggleMirrorHorizontal() },
                    onToggleMirrorV = { viewModel.toggleMirrorVertical() },
                    showReference = showReference,
                    referenceOpacity = referenceOpacity,
                    onReferenceOpacityChange = { viewModel.updateReferenceOpacity(it) },
                    onClearReference = { viewModel.clearReference() },
                    zoom = zoom,
                    onZoomIn = { zoom = nextZoom(zoom, up = true) },
                    onZoomOut = { zoom = nextZoom(zoom, up = false) },
                    onFit = {
                        zoom = 1f
                        fitSignal++
                    }
                )
            }
        },
        containerColor = theme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PixelCanvasView(
                canvas = canvas,
                showGrid = showGrid,
                currentTool = currentTool,
                showReference = showReference,
                referenceBitmap = referenceBitmap,
                referenceOpacity = referenceOpacity,
                mirrorHorizontal = mirrorHorizontal,
                mirrorVertical = mirrorVertical,
                selectionStart = selectionStart,
                selectionEnd = selectionEnd,
                activeStamp = activeStamp,
                stampStyle = viewModel.stampStyle,
                zoom = zoom,
                fitSignal = fitSignal,
                onZoomChange = { zoom = it },
                onCellAction = { row, col, action ->
                    when (currentTool) {
                        Tool.BRUSH -> when (action) {
                            CellAction.Down -> {
                                viewModel.beginStroke()
                                viewModel.paintCell(row, col)
                            }
                            CellAction.Drag -> viewModel.paintCell(row, col)
                            CellAction.Up -> viewModel.endStroke()
                            CellAction.Cancel -> viewModel.endStroke()
                        }
                        Tool.ERASER -> when (action) {
                            CellAction.Down -> {
                                viewModel.beginStroke()
                                viewModel.eraseCell(row, col)
                            }
                            CellAction.Drag -> viewModel.eraseCell(row, col)
                            CellAction.Up -> viewModel.endStroke()
                            CellAction.Cancel -> viewModel.endStroke()
                        }
                        Tool.FILL -> if (action == CellAction.Down) viewModel.fillFrom(row, col)
                        Tool.EYEDROPPER -> if (action == CellAction.Down) {
                            viewModel.updateActiveColor(viewModel.pickColor(row, col))
                        }
                        Tool.SELECT -> when (action) {
                            CellAction.Down -> viewModel.startSelection(row, col)
                            CellAction.Drag -> viewModel.updateSelection(row, col)
                            CellAction.Up -> viewModel.endSelection()
                            CellAction.Cancel -> viewModel.clearSelection()
                        }
                        // El sello se coloca AL SOLTAR el dedo (mirando la vista previa):
                        // así no queda "arrastrado" por el lienzo.
                        Tool.STAMPS -> if (action == CellAction.Up) viewModel.applyStamp(row, col)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("canvas")
            )
        }
    }

    if (showColorPalette) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showColorPalette = false },
            sheetState = sheetState,
            containerColor = theme.surface
        ) {
            ColorPalettePanel(
                activeColor = activeColorInt,
                onColorSelected = {
                    viewModel.updateActiveColor(it)
                    showColorPalette = false
                },
                savedColors = savedColors.value,
                onSavedColorRemoved = { viewModel.removeSavedColor(it) },
                onChangeAllColors = {
                    showColorPalette = false
                    showReplaceDialog = true
                },
                modifier = Modifier.testTag("color_palette")
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            viewModel = viewModel,
            context = context
        )
    }
    if (showHelp) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showHelp = false },
            sheetState = sheetState,
            containerColor = theme.surface
        ) {
            HowToUseSheet()
        }
    }
    if (showStamps) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showStamps = false },
            sheetState = sheetState,
            containerColor = theme.surface
        ) {
            StampPickerSheet(
                activeStamp = activeStamp,
                stampStyle = viewModel.stampStyle,
                activeColor = activeColorInt,
                savedColors = savedColors.value,
                selectedCategory = stampCategory,
                onCategoryChange = { stampCategory = it },
                onStampSelected = { stamp ->
                    viewModel.setStamp(stamp)
                    showStamps = false
                },
                onStyleChange = { viewModel.updateStampStyle(it) },
                onColorSelected = { viewModel.updateActiveColor(it) },
                onCustomColor = { showCustomStampColor = true }
            )
        }
    }
    if (showCustomStampColor) {
        SimpleColorPickerDialog(
            initialColor = activeColorInt,
            title = "Color del símbolo",
            onDismiss = { showCustomStampColor = false },
            onColorSelected = {
                viewModel.updateActiveColor(it)
                showCustomStampColor = false
            }
        )
    }
    if (showReplaceDialog) {
        ReplaceColorDialog(
            onDismiss = { showReplaceDialog = false },
            viewModel = viewModel,
            currentColor = activeColorInt
        )
    }
    if (showClearConfirm) {
        ConfirmDialog(
            title = "¿Borrar todo el dibujo?",
            message = "Se borra todo el lienzo. Podrás deshacerlo con la flecha de deshacer.",
            confirmText = "Sí, borrar",
            onConfirm = {
                viewModel.clearCanvas()
                showClearConfirm = false
            },
            onDismiss = { showClearConfirm = false }
        )
    }
}

private fun nextZoom(current: Float, up: Boolean): Float {
    val index = ZOOM_STEPS.indexOfFirst { it >= current - 0.01f }.coerceAtLeast(0)
    val next = if (up) index + 1 else index - 1
    return ZOOM_STEPS[next.coerceIn(0, ZOOM_STEPS.lastIndex)]
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTopBar(
    canvasName: String,
    autoSaveStatus: String,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSaveClick: () -> Unit,
    onExportClick: () -> Unit,
    onHelpClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val theme = AppTheme.colors
    TopAppBar(
        title = {
            Column {
                Text(
                    text = canvasName,
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Solo el aviso de guardado; las instrucciones viven en el botón "?"
                if (autoSaveStatus.isNotBlank()) {
                    Text(
                        text = autoSaveStatus,
                        fontSize = 9.5.sp,
                        color = theme.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = theme.textPrimary)
            }
        },
        actions = {
            IconButton(onClick = onHelpClick) {
                Icon(Icons.Rounded.HelpOutline, contentDescription = "Cómo se usa", tint = theme.textPrimary)
            }
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(
                    Icons.Rounded.Undo,
                    contentDescription = "Deshacer",
                    tint = if (canUndo) theme.textPrimary else theme.textDisabled
                )
            }
            IconButton(onClick = onRedo, enabled = canRedo) {
                Icon(
                    Icons.Rounded.Redo,
                    contentDescription = "Rehacer",
                    tint = if (canRedo) theme.textPrimary else theme.textDisabled
                )
            }
            IconButton(onClick = onSaveClick) {
                // Los iconos propios son mapas de bits: hay que darles tamaño
                // explícito (los de vector ya traen 24 dp por defecto). 26 dp con
                // figura al 76% deja la misma tinta que los de Material.
                Icon(
                    painter = painterResource(R.drawable.ic_action_guardado),
                    contentDescription = "Guardar",
                    tint = theme.textPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
            IconButton(onClick = onExportClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_action_descargar),
                    contentDescription = "Exportar PNG",
                    tint = theme.textPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.bar)
    )
}

@Composable
private fun ActionRow(
    activeColor: Int,
    showGrid: Boolean,
    showReference: Boolean,
    referenceAvailable: Boolean,
    onPickReference: () -> Unit,
    mirrorHorizontal: Boolean,
    mirrorVertical: Boolean,
    onOpenPalette: () -> Unit,
    onToggleGrid: () -> Unit,
    onToggleReference: () -> Unit,
    onToggleMirrorH: () -> Unit,
    onToggleMirrorV: () -> Unit,
    onReplaceColor: () -> Unit,
    onClearAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("action_row")
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onOpenPalette)
                .padding(vertical = 4.dp)
                .testTag("color_button")
                .semantics { contentDescription = "Elegir color" }
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(activeColor))
                    .border(2.dp, AppTheme.colors.textPrimary, RoundedCornerShape(8.dp))
            )
            Text("Color", fontSize = 10.sp, color = AppTheme.colors.textPrimary, fontWeight = FontWeight.SemiBold)
        }

        ArtistChip(
            label = "Cuadrícula",
            iconRes = R.drawable.ic_action_cuadricula,
            selected = showGrid,
            onClick = onToggleGrid,
            modifier = Modifier.weight(1f)
        )
        ArtistChip(
            label = "Simetría",
            iconRes = R.drawable.ic_action_simetria,
            selected = mirrorHorizontal || mirrorVertical,
            onClick = {
                if (mirrorHorizontal || mirrorVertical) {
                    onToggleMirrorH()
                    onToggleMirrorV()
                } else {
                    onToggleMirrorH()
                    onToggleMirrorV()
                }
            },
            modifier = Modifier.weight(1f)
        )
        ArtistChip(
            label = "Referencia",
            iconRes = R.drawable.ic_action_referencia,
            selected = showReference,
            // Siempre disponible: si no hay foto, se elige una para calcar.
            enabled = true,
            onClick = { if (referenceAvailable) onToggleReference() else onPickReference() },
            modifier = Modifier.weight(1f)
        )
        ArtistChip(
            label = "Cambiar",
            iconRes = R.drawable.ic_action_cambiar,
            selected = false,
            onClick = onReplaceColor,
            modifier = Modifier.weight(1f)
        )
        ArtistChip(
            label = "Borrar",
            iconRes = R.drawable.ic_action_eliminar,
            selected = false,
            danger = true,
            onClick = onClearAll,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ToolRow(
    currentTool: Tool,
    onToolSelected: (Tool) -> Unit
) {
    val tools = listOf(Tool.BRUSH, Tool.ERASER, Tool.FILL, Tool.EYEDROPPER, Tool.SELECT, Tool.STAMPS)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tool_row")
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tools.forEach { tool ->
            ToolButton(
                tool = tool,
                selected = currentTool == tool,
                onClick = { onToolSelected(tool) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ToolButton(
    tool: Tool,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = AppTheme.colors
    // Transición suave de color: da sensación de app moderna (no un salto seco).
    val background by animateColorAsState(
        targetValue = if (selected) theme.primary else theme.surface,
        label = "toolBackground"
    )
    val content by animateColorAsState(
        targetValue = if (selected) theme.onPrimary else theme.textPrimary,
        label = "toolContent"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = if (selected) Color.Transparent else theme.border,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .testTag("tool_${tool.name}")
            .semantics { contentDescription = tool.label }
    ) {
        Icon(
            painter = painterResource(toolIconRes(tool)),
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(26.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = tool.shortLabel,
            fontSize = 9.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = content,
            maxLines = 1
        )
    }
}

@Composable
private fun ArtistChip(
    label: String,
    iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    danger: Boolean = false
) {
    val theme = AppTheme.colors
    val accent = if (danger) theme.danger else theme.primary
    val background by animateColorAsState(
        targetValue = if (selected) accent else theme.surface,
        label = "chipBackground"
    )
    val content = when {
        !enabled -> theme.textDisabled
        selected -> Color.White
        danger -> accent
        else -> theme.textPrimary
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = if (selected) Color.Transparent else theme.border,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 3.dp)
            .semantics { contentDescription = label + if (selected) " activado" else "" }
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            fontSize = 8.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Franja de contexto de la herramienta activa. Está SIEMPRE visible (aunque no
 * haya nada que mostrar) para que las filas de botones nunca se muevan.
 */
@Composable
private fun OptionsStrip(
    currentTool: Tool,
    activeStamp: Stamp?,
    activeColor: Int,
    stampStyle: StampStyle,
    onOpenStamps: () -> Unit,
    hasSelection: Boolean,
    onEraseSelection: () -> Unit,
    onClearSelection: () -> Unit,
    mirrorHorizontal: Boolean,
    mirrorVertical: Boolean,
    onToggleMirrorH: () -> Unit,
    onToggleMirrorV: () -> Unit,
    showReference: Boolean,
    referenceOpacity: Float,
    onReferenceOpacityChange: (Float) -> Unit,
    onClearReference: () -> Unit,
    zoom: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onFit: () -> Unit
) {
    val theme = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .testTag("options_strip")
            .background(theme.surfaceVariant)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sin textos-guía aquí: solo controles reales. La ayuda vive en el botón
        // "?" de la barra superior (pop-up), así nada queda cortado ni amontonado.
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            when {
                currentTool == Tool.STAMPS -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(1.dp, theme.border, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        activeStamp?.let {
                            // Sobre recuadro blanco (como el lienzo) se ve igual en
                            // tema claro y oscuro, y con el color y estilo elegidos.
                            StampPreview(
                                stamp = it,
                                size = 28.dp,
                                color = Color(activeColor),
                            style = stampStyle
                            )
                        }
                    }
                    Text(
                        text = activeStamp?.name ?: "Elige uno",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = theme.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    SmallActionButton("Cambiar", onClick = onOpenStamps)
                }
                currentTool == Tool.SELECT && hasSelection -> Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SmallActionButton("Borrar zona", danger = true, onClick = onEraseSelection)
                    SmallActionButton("Quitar", onClick = onClearSelection)
                }
                mirrorHorizontal || mirrorVertical -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallActionButton("H", selected = mirrorHorizontal, onClick = onToggleMirrorH)
                    SmallActionButton("V", selected = mirrorVertical, onClick = onToggleMirrorV)
                }
                showReference -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Foto", fontSize = 11.sp, color = theme.textSecondary)
                    Slider(
                        modifier = Modifier.weight(1f),
                        value = referenceOpacity,
                        onValueChange = onReferenceOpacityChange,
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = theme.primary,
                            activeTrackColor = theme.primary,
                            inactiveTrackColor = theme.border
                        )
                    )
                    SmallActionButton("Quitar", onClick = onClearReference)
                }
                else -> Unit
            }
        }

        // Controles de vista: SIEMPRE en el mismo sitio, grandes y evidentes.
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onZoomOut, modifier = Modifier.size(42.dp)) {
                Icon(
                    Icons.Rounded.Remove,
                    contentDescription = "Alejar",
                    tint = if (zoom > ZOOM_STEPS.first()) theme.textPrimary else theme.textDisabled
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp, 38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(theme.surface)
                    .clickable(onClick = onFit)
                    .semantics { contentDescription = "Ajustar a la pantalla" },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (zoom % 1f == 0f) "${zoom.toInt()}×" else "${zoom}×",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.textPrimary,
                    textAlign = TextAlign.Center
                )
            }
            IconButton(onClick = onZoomIn, modifier = Modifier.size(42.dp)) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = "Acercar",
                    tint = if (zoom < ZOOM_STEPS.last()) theme.textPrimary else theme.textDisabled
                )
            }
        }
    }
}

@Composable
private fun SmallActionButton(
    text: String,
    selected: Boolean = false,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val theme = AppTheme.colors
    val accent = if (danger) theme.danger else theme.primary
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) accent else theme.surface)
            .border(1.dp, if (selected) accent else theme.border, RoundedCornerShape(12.dp))
            .padding(horizontal = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else accent,
            maxLines = 1
        )
    }
}

/** Dibuja la máscara de un símbolo con el color indicado. */
@Composable
private fun StampPreview(
    stamp: Stamp,
    size: androidx.compose.ui.unit.Dp,
    color: Color,
    style: StampStyle = StampStyle.FILLED,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(size)) {
        val side = this.size.minDimension
        val cell = side / maxOf(stamp.width, stamp.height)
        val offsetX = (this.size.width - cell * stamp.width) / 2f
        val offsetY = (this.size.height - cell * stamp.height) / 2f
        stamp.mask(style != StampStyle.OUTLINE).forEachIndexed { row, colors ->
            colors.forEachIndexed { col, value ->
                if (value != Color.Transparent.toArgb()) {
                    val cellColor = if (style == StampStyle.ORIGINAL && stamp.hasOwnColors) Color(value) else color
                    drawRect(
                        color = cellColor,
                        topLeft = androidx.compose.ui.geometry.Offset(offsetX + col * cell, offsetY + row * cell),
                        size = androidx.compose.ui.geometry.Size(cell, cell)
                    )
                }
            }
        }
    }
}

/**
 * Catálogo de símbolos en pop-up: muchas figuras organizadas por categorías.
 * Antes solo había tres y se mostraban en la franja inferior.
 */
@Composable
private fun StampPickerSheet(
    activeStamp: Stamp?,
    stampStyle: StampStyle,
    activeColor: Int,
    savedColors: List<Int>,
    selectedCategory: String?,
    onCategoryChange: (String?) -> Unit,
    onStampSelected: (Stamp) -> Unit,
    onStyleChange: (StampStyle) -> Unit,
    onColorSelected: (Int) -> Unit,
    onCustomColor: () -> Unit
) {
    val theme = AppTheme.colors
    val stamps = Stamp.byCategory(selectedCategory)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Símbolos",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = theme.textPrimary,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp)
        )
        Text(
            "${stamps.size} dibujos para estampar · elige uno y tócalo en el lienzo",
            fontSize = 11.5.sp,
            color = theme.textSecondary,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 2.dp)
        )
        Spacer(Modifier.height(10.dp))

        // Color y estilo del símbolo: se aplican al estampar.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(activeColor))
                    .border(2.dp, theme.border, RoundedCornerShape(10.dp))
                    .clickable(onClick = onCustomColor)
                    .semantics { contentDescription = "Color del símbolo" }
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                (savedColors.take(10)).forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color(color))
                            .border(
                                width = if (color == activeColor) 3.dp else 1.dp,
                                color = if (color == activeColor) theme.primary else theme.border,
                                shape = RoundedCornerShape(9.dp)
                            )
                            .clickable { onColorSelected(color) }
                    )
                }
                if (savedColors.isEmpty()) {
                    Text(
                        "Color",
                        fontSize = 11.sp,
                        color = theme.textSecondary
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                StampStyle.entries.forEach { style ->
                    SmallActionButton(
                        text = style.label,
                        selected = stampStyle == style,
                        onClick = { onStyleChange(style) }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .testTag("stamp_categories")
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoryChip(
                label = "Todos",
                selected = selectedCategory == null,
                onClick = { onCategoryChange(null) }
            )
            Stamp.categories.forEach { category ->
                CategoryChip(
                    label = category,
                    selected = selectedCategory == category,
                    onClick = { onCategoryChange(category) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 84.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .testTag("stamp_grid")
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Clave única: el nombre por sí solo no basta (en el catálogo puede
            // repetirse) y una clave repetida hace que la galería se cierre.
            itemsIndexed(stamps, key = { index, stamp -> "$index-${stamp.name}" }) { _, stamp ->
                val isActive = activeStamp?.name == stamp.name
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isActive) theme.primary.copy(alpha = 0.18f) else theme.surface)
                        .border(
                            width = if (isActive) 2.dp else 1.dp,
                            color = if (isActive) theme.primary else theme.border,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onStampSelected(stamp) }
                        .padding(vertical = 8.dp)
                        .semantics { contentDescription = "Símbolo ${stamp.name}" }
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        StampPreview(
                            stamp = stamp,
                            size = 42.dp,
                            color = Color(activeColor),
                            style = stampStyle
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stamp.name,
                        fontSize = 10.sp,
                        color = theme.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val theme = AppTheme.colors
    Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        color = if (selected) theme.onPrimary else theme.textPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) theme.primary else theme.surface)
            .border(1.dp, if (selected) theme.primary else theme.border, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

/**
 * Iconos propios de Pixelados (assets/): set dibujado a mano, de trazo continuo,
 * que se tiñe con el color del tema. Es el que corresponde a la identidad de la
 * app, en lugar de los iconos genéricos de la librería.
 */
fun toolIconRes(tool: Tool): Int = when (tool) {
    Tool.BRUSH -> R.drawable.ic_tool_pincel
    Tool.ERASER -> R.drawable.ic_tool_borrador
    Tool.FILL -> R.drawable.ic_tool_rellenar
    Tool.EYEDROPPER -> R.drawable.ic_tool_gotero
    Tool.SELECT -> R.drawable.ic_tool_seleccion
    Tool.STAMPS -> R.drawable.ic_tool_simbolos
}

@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    viewModel: EditorViewModel,
    context: Context
) {
    val theme = AppTheme.colors
    var scale by remember { mutableStateOf(1) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.surface,
        title = { Text("Exportar PNG", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = theme.textPrimary) },
        text = {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text("¿Qué tan grande quieres la imagen?", fontSize = 13.sp, color = theme.textSecondary)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(1, 2, 4).forEach { s ->
                        val selected = scale == s
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { scale = s }
                                .background(if (selected) theme.primary else theme.surfaceVariant)
                                .padding(vertical = 12.dp)
                        ) {
                            Text(
                                "$s×",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) theme.onPrimary else theme.textPrimary
                            )
                            Text(
                                when (s) {
                                    1 -> "Tamaño real"
                                    2 -> "Doble"
                                    else -> "Cuádruple"
                                },
                                fontSize = 11.sp,
                                color = if (selected) theme.onPrimary else theme.textSecondary
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Cada cuadro sale como un píxel, sin suavizado. Lo que está vacío queda transparente.",
                    fontSize = 11.sp,
                    color = theme.textSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bitmap = viewModel.exportPng(scale)
                    val ok = saveBitmapToGallery(context, bitmap)
                    Toast.makeText(
                        context,
                        if (ok) "Guardado en la galería (Pictures/Pixelados)" else "No se pudo guardar",
                        Toast.LENGTH_SHORT
                    ).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = theme.primary, contentColor = theme.onPrimary)
            ) {
                Text("Guardar en galería")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) { Text("Cancelar", color = theme.textSecondary) }
                TextButton(onClick = {
                    shareBitmap(context, viewModel.exportPng(scale))
                    onDismiss()
                }) {
                    Text("Compartir…", color = theme.primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    )
}

@Composable
fun ReplaceColorDialog(
    onDismiss: () -> Unit,
    viewModel: EditorViewModel,
    currentColor: Int
) {
    val theme = AppTheme.colors
    var fromColor by remember { mutableStateOf(currentColor) }
    var toColor by remember { mutableStateOf(viewModel.activeColor) }
    var picking by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.surface,
        title = {
            Text(
                "Cambiar un color en todo el dibujo",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = theme.textPrimary
            )
        },
        text = {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    "Toca un cuadro para elegir el color que quieres cambiar y otro para el color nuevo.",
                    fontSize = 12.sp,
                    color = theme.textSecondary
                )
                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    ColorChoiceBox(
                        caption = "Cambiar el color",
                        color = fromColor,
                        onClick = { picking = 1 },
                        modifier = Modifier.weight(1f)
                    )
                    ColorChoiceBox(
                        caption = "Por el color",
                        color = toColor,
                        onClick = { picking = 2 },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.replaceColor(fromColor, toColor)
                    onDismiss()
                },
                enabled = fromColor != toColor,
                colors = ButtonDefaults.buttonColors(containerColor = theme.primary, contentColor = theme.onPrimary)
            ) {
                Text("Cambiar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = theme.textSecondary) }
        }
    )

    if (picking != 0) {
        SimpleColorPickerDialog(
            initialColor = if (picking == 1) fromColor else toColor,
            title = if (picking == 1) "Color que quieres cambiar" else "Color nuevo",
            onDismiss = { picking = 0 },
            onColorSelected = {
                if (picking == 1) fromColor = it else toColor = it
                picking = 0
            }
        )
    }
}

@Composable
private fun ColorChoiceBox(
    caption: String,
    color: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = AppTheme.colors
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(caption, fontSize = 12.sp, color = theme.textPrimary, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(color))
                .border(2.dp, theme.border, RoundedCornerShape(14.dp))
                .clickable(onClick = onClick)
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = "Sí"
) {
    val theme = AppTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.surface,
        title = { Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = theme.textPrimary) },
        text = { Text(message, fontSize = 13.sp, color = theme.textSecondary) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = theme.danger, contentColor = Color.White)
            ) {
                Text(confirmText, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("No, cancelar", color = theme.textPrimary, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

/**
 * Guarda el PNG en la galería del dispositivo. Devuelve true si se pudo.
 *
 * En Android 10+ se usa MediaStore con carpeta (Pictures/Pixelados). En Android
 * 9 y anteriores se escribe en la carpeta de la app y se avisa al sistema para
 * que la imagen aparezca en la galería (sin pedir permisos de almacenamiento).
 */
fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Boolean {
    val nombre = "pixelados_${System.currentTimeMillis()}.png"
    return runCatching {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, nombre)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Pixelados")
            }
            val uri = context.contentResolver
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return false
            context.contentResolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            } ?: return false
            true
        } else {
            val file = escribirPngEnCarpetaDeApp(context, nombre, bitmap) ?: return false
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf("image/png"),
                null
            )
            true
        }
    }.getOrDefault(false)
}

/** Escribe el PNG en la carpeta pública de la app (no requiere permisos). */
private fun escribirPngEnCarpetaDeApp(context: Context, nombre: String, bitmap: Bitmap): File? =
    runCatching {
        val carpeta = File(
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES),
            "Pixelados"
        ).apply { mkdirs() }
        val file = File(carpeta, nombre)
        java.io.FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        file
    }.getOrNull()

fun shareBitmap(context: Context, bitmap: Bitmap) {
    val nombre = "pixelados_share_${System.currentTimeMillis()}.png"
    val uri: android.net.Uri? = runCatching {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, nombre)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Pixelados")
            }
            val destino = context.contentResolver
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return
            context.contentResolver.openOutputStream(destino)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            destino
        } else {
            val file = escribirPngEnCarpetaDeApp(context, nombre, bitmap) ?: return
            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/png"), null)
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "com.pixelados.fileprovider",
                file
            )
        }
    }.getOrNull()
    uri ?: return
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Compartir dibujo"))
}

/**
 * Ayuda en pop-up: aquí viven las guías de uso. Antes estaban como textos fijos
 * en la franja inferior, donde se cortaban y amontonaban.
 */
@Composable
private fun HowToUseSheet() {
    val theme = AppTheme.colors
    val items = listOf(
        Icons.Rounded.TouchApp to ("1 dedo: pinta un cuadro" to "Cada cuadro de la cuadrícula es un píxel del dibujo."),
        Icons.Rounded.ZoomOutMap to ("2 dedos: mover y ampliar" to "Pellizca con dos dedos; el botón 1× vuelve a ajustar."),
        Icons.Rounded.Undo to ("Deshacer: borra la última línea" to "Un toque quita el trazo completo, no píxel a píxel."),
        Icons.Rounded.CropFree to ("Selección: marca una zona" to "Arrastra para marcarla y luego toca Borrar zona."),
        Icons.Rounded.FlipCameraAndroid to ("Simetría: copia al lado opuesto" to "Lo que pintes aparece reflejado en espejo."),
        Icons.Rounded.Star to ("Símbolos: pega un dibujo" to "Abre el catálogo, elige uno y tócalo en el lienzo."),
        Icons.Rounded.Image to ("Referencia: calca una foto" to "Elige una foto, ajústale la transparencia y cópiala píxel a píxel."),
        Icons.Rounded.Palette to ("Color: elige o crea uno" to "Toca Color para la paleta y Otro color para inventarlo.")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(
            "Cómo se usa",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = theme.textPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Todo se hace con el dedo, sin menús escondidos.",
            fontSize = 12.sp,
            color = theme.textSecondary
        )
        Spacer(Modifier.height(14.dp))

        items.forEach { (icon, texts) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(theme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = theme.primary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(texts.first, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = theme.textPrimary)
                    Text(texts.second, fontSize = 11.5.sp, color = theme.textSecondary)
                }
            }
        }
    }
}
