package com.pixelados.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pixelados.data.model.PixelCanvas
import com.pixelados.data.model.Project
import com.pixelados.data.model.Stamp
import com.pixelados.data.model.StampStyle
import com.pixelados.data.model.Tool
import com.pixelados.data.repository.ProjectRepository
import com.pixelados.data.repository.SavedColorsRepository
import com.pixelados.data.util.PhotoPixelator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditorViewModel(
    private val repository: ProjectRepository,
    private val savedColorsRepo: SavedColorsRepository,
    width: Int,
    height: Int,
    photoUri: String? = null,
    initialProjectId: String? = null,
    appContext: Context? = null
) : ViewModel() {

    // --- Estado del lienzo ---
    private val _canvas = MutableStateFlow(PixelCanvas.empty(width, height))
    val canvas = _canvas.asStateFlow()

    // --- Imagen de referencia (flujo B: convertir foto) ---
    private val _referenceBitmap = mutableStateOf<Bitmap?>(null)
    var referenceBitmap: Bitmap? get() = _referenceBitmap.value
    private set(value) { _referenceBitmap.value = value }

    /**
     * Foto ORIGINAL elegida por el usuario. Se conserva para poder volver a
     * pixelar al cambiar el nivel de detalle: repixelar desde la referencia ya
     * reducida perdía calidad en cada cambio.
     */
    private var originalPhotoBitmap: Bitmap? = null

    private val _showReference = mutableStateOf(false)
    var showReference: Boolean
        get() = _showReference.value
        set(value) { _showReference.value = value }

    private val _referenceOpacity = mutableStateOf(0.3f)
    var referenceOpacity: Float
        get() = _referenceOpacity.value
        set(value) { _referenceOpacity.value = value.coerceIn(0f, 1f) }

    // --- Herramientas y configuración ---
    var currentTool by mutableStateOf<Tool>(Tool.BRUSH)
        private set

    var activeColor by mutableStateOf(Color.Black.toArgb())
        private set


    // --- Historial deshacer/rehacer (máx 50) ---
    private val undoStack = mutableStateListOf<PixelCanvas>()
    private val redoStack = mutableStateListOf<PixelCanvas>()
    private val MAX_HISTORY = 50

    /** Momento del último guardado en el historial (para fundir trazos seguidos). */
    private var lastPushAt = 0L
    private val MERGE_WINDOW_MS = 400L

    val canUndo: Boolean get() = undoStack.size > 1
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    // --- Cuadrícula ---
    var showGrid by mutableStateOf(true)
        private set

    // --- Paleta de colores ---
    var colorPaletteVisible by mutableStateOf(false)
        private set
    var showColorPalette: Boolean
        get() = colorPaletteVisible
        private set(value) { colorPaletteVisible = value }

    // --- Espejo (simetría) ---
    var mirrorHorizontal by mutableStateOf(false)
        private set
    var mirrorVertical by mutableStateOf(false)
        private set

    // --- Selección rectangular ---
    var selectionStart by mutableStateOf<Pair<Int, Int>?>(null)
        private set
    var selectionEnd by mutableStateOf<Pair<Int, Int>?>(null)
        private set
    var selectionContent by mutableStateOf<List<Pair<Pair<Int, Int>, Int>>?>(null)
        private set
    var isSelecting by mutableStateOf(false)
        private set

    // --- Sello activo ---
    var activeStamp by mutableStateOf<Stamp?>(null)
        private set

    /** Estilo del sello: relleno, contorno o colores originales. */
    var stampStyle by mutableStateOf(StampStyle.FILLED)
        private set

    fun updateStampStyle(style: StampStyle) { stampStyle = style }

    // --- Colores guardados ("Mis colores") ---
    private val _savedColors = MutableStateFlow<List<Int>>(emptyList())
    val savedColors = _savedColors.asStateFlow()

    // --- Autoguardado ---
    private var strokeCount = 0
    private val AUTO_SAVE_THRESHOLD = 10
    private val _autoSaveStatus = MutableStateFlow<String>("")
    val autoSaveStatus = _autoSaveStatus.asStateFlow()

    /** Tick que cambia en cada guardado: permite a la UI re-mostrar y auto-ocultar el aviso. */
    private val _autoSaveTick = MutableStateFlow(0)
    val autoSaveTick = _autoSaveTick.asStateFlow()

    // --- Identidad del proyecto: id estable para hacer UPDATE (no INSERT) ---
    private var projectId: String? = null

    /** Marca de cambios pendientes: evita reescrituras innecesarias. */
    private var isDirty = false

    /** Fija el id del proyecto (upsert contra ese registro). */
    fun setProjectId(id: String?) {
        if (projectId == null) projectId = id
    }

    fun getProjectId(): String? = projectId

    // --- Carga inicial ---
    init {
        undoStack.add(_canvas.value)
        loadSavedColors()
        // Id estable desde la navegación: el autoguardado hace UPDATE, no INSERT.
        if (initialProjectId != null) {
            projectId = initialProjectId
            viewModelScope.launch { loadProjectById(initialProjectId) }
        }
        if (photoUri != null && appContext != null) {
            loadPhotoAndPixelate(photoUri, appContext)
        }
    }

    /** Carga un lienzo existente conservando su id (upsert). */
    private suspend fun loadProjectById(id: String) {
        val project = repository.getProject(id) ?: return
        val loaded = project.canvas
        // El historial ARRANCA en el dibujo cargado: el lienzo vacío con el que
        // se crea el editor nunca fue un estado de este proyecto, así que
        // deshacer no puede borrar el trabajo que el usuario abrió.
        undoStack.clear()
        redoStack.clear()
        undoStack.add(loaded)
        _canvas.value = loaded
    }

    class Factory(
        private val repository: ProjectRepository,
        private val savedColorsRepo: SavedColorsRepository,
        private val width: Int,
        private val height: Int,
        private val photoUri: String? = null,
        private val projectId: String? = null,
        private val context: Context? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditorViewModel(repository, savedColorsRepo, width, height, photoUri, projectId, context) as T
        }
    }

    // ============ HERRAMIENTAS Y AJUSTES ============

    fun setTool(tool: Tool) {
        currentTool = tool
        clearSelection()
        activeStamp = null
        if (tool == Tool.STAMPS) activeStamp = Stamp.allStamps.firstOrNull()
    }

    fun setStamp(stamp: Stamp) {
        activeStamp = stamp
        currentTool = Tool.STAMPS
    }

    fun updateActiveColor(color: Int) {
        activeColor = color
        // Guardar en "Colores recientes" y refrescar la lista de inmediato, para
        // que el color recién usado aparezca en la paleta sin recargar la app.
        viewModelScope.launch {
            savedColorsRepo.addColor(color)
            _savedColors.value = savedColorsRepo.getSavedColors()
        }
    }

    fun toggleGrid() { showGrid = !showGrid }

    fun toggleColorPalette() { colorPaletteVisible = !colorPaletteVisible }

    fun toggleReference() { showReference = !showReference }

    /**
     * Carga una foto como CAPA DE REFERENCIA para calcar (no la convierte en
     * pixel art): se dibuja semitransparente debajo del dibujo, con la opacidad
     * que elija el usuario.
     */
    fun loadReferenceFromUri(uriString: String, context: Context) {
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    android.provider.MediaStore.Images.Media.getBitmap(
                        context.contentResolver,
                        Uri.parse(uriString)
                    )
                }.getOrNull()
            } ?: return@launch

            val canvas = _canvas.value
            _referenceBitmap.value = PhotoPixelator.createReferenceBitmap(
                bitmap,
                maxOf(canvas.width, canvas.height)
            )
            _showReference.value = true
        }
    }

    /** Quita la foto de referencia. */
    fun clearReference() {
        _referenceBitmap.value = null
        _showReference.value = false
    }

    fun updateReferenceOpacity(opacity: Float) { referenceOpacity = opacity.coerceIn(0f, 1f) }

    fun toggleMirrorHorizontal() { mirrorHorizontal = !mirrorHorizontal }

    fun toggleMirrorVertical() { mirrorVertical = !mirrorVertical }

    // ============ DIBUJO ============

    /**
     * Trazo en curso. Mientras hay un trazo abierto, [paintCell] previsualiza
     * los cambios sin escribir historial; al cerrarlo (dedo levantado) se
     * guarda UNA sola entrada de deshacer para todo el trazo.
     */
    private var pendingStroke: MutableMap<Pair<Int, Int>, Int>? = null

    val isStrokeOpen: Boolean get() = pendingStroke != null

    fun beginStroke() {
        // Si un trazo anterior quedó abierto (gesto cancelado por el sistema),
        // se cierra antes de empezar el nuevo.
        if (pendingStroke != null) endStroke()
        pendingStroke = LinkedHashMap()
    }

    fun endStroke() {
        val stroke = pendingStroke ?: return
        pendingStroke = null
        if (stroke.isEmpty()) return
        pushToHistory(_canvas.value, mergeWithPrevious = true)
        redoStack.clear()
        isDirty = true
        checkAutoSave()
    }

    /**
     * Pinta EXACTAMENTE un píxel del lienzo (1×1, sin suavizado ni áreas).
     * La celda bajo el dedo/lápiz es la única afectada; el espejo, si está
     * activo, añade su celda simétrica. Respuesta inmediata (sin trazo
     * intermedio), manteniendo la estética de pixel art.
     */
    fun paintCell(row: Int, col: Int, color: Int? = null) {
        val paintColor = color ?: activeColor
        val canvas = _canvas.value
        if (row !in 0 until canvas.height || col !in 0 until canvas.width) return
        val changes = canvas.mirrorChanges(
            mapOf((row to col) to paintColor),
            mirrorHorizontal,
            mirrorVertical
        )
        val stroke = pendingStroke
        if (stroke != null) {
            // Dentro de un trazo: se previsualiza al instante y el historial
            // recibe UNA sola entrada al levantar el dedo (ver endStroke()).
            stroke.putAll(changes)
            _canvas.value = canvas.setPixels(changes)
            isDirty = true
        } else {
            applyChanges(changes)
        }
    }

    /** Borra (pone transparente) */
    fun eraseCell(row: Int, col: Int) {
        paintCell(row, col, Color.Transparent.toArgb())
    }

    /** Relleno por inundación (flood fill) desde una celda */
    fun fillFrom(row: Int, col: Int) {
        val canvas = _canvas.value
        val changes = canvas.floodFillChanges(row, col, activeColor)
        if (changes.isEmpty()) return
        applyChanges(canvas.mirrorChanges(changes, mirrorHorizontal, mirrorVertical))
    }

    /** Cuentagotas: toma color del lienzo */
    fun pickColor(row: Int, col: Int): Int {
        return _canvas.value.getPixel(row, col)
    }

    /** Reemplaza TODAS las celdas de un color por otro en todo el lienzo */
    fun replaceColor(fromColor: Int, toColor: Int) {
        val changes = _canvas.value.replaceColorChanges(fromColor, toColor)
        if (changes.isEmpty()) return
        applyChanges(changes)
    }

    /** Aplica un sello en la posición dada */
    fun applyStamp(centerRow: Int, centerCol: Int) {
        val stamp = activeStamp ?: return
        val changes = stamp.applyTo(_canvas.value, centerRow, centerCol, activeColor, stampStyle)
        applyChanges(changes)
    }

    // ============ SELECCIÓN ============

    fun startSelection(row: Int, col: Int) {
        isSelecting = true
        selectionStart = row to col
        selectionEnd = row to col
        selectionContent = null
    }

    fun updateSelection(row: Int, col: Int) {
        if (!isSelecting) return
        selectionEnd = row to col
    }

    fun endSelection() {
        if (!isSelecting) return
        isSelecting = false
        val start = selectionStart
        val end = selectionEnd
        if (start != null && end != null) captureSelection(start, end)
    }

    fun clearSelection() {
        isSelecting = false
        selectionStart = null
        selectionEnd = null
        selectionContent = null
    }

    /** Borra (deja transparente) toda la zona marcada con la herramienta Seleccionar. */
    fun eraseSelection() {
        val start = selectionStart ?: return
        val end = selectionEnd ?: return
        val canvas = _canvas.value
        val minRow = minOf(start.first, end.first).coerceIn(0, canvas.height - 1)
        val maxRow = maxOf(start.first, end.first).coerceIn(0, canvas.height - 1)
        val minCol = minOf(start.second, end.second).coerceIn(0, canvas.width - 1)
        val maxCol = maxOf(start.second, end.second).coerceIn(0, canvas.width - 1)
        val changes = LinkedHashMap<Pair<Int, Int>, Int>()
        for (row in minRow..maxRow) {
            for (col in minCol..maxCol) {
                changes[row to col] = Color.Transparent.toArgb()
            }
        }
        applyChanges(changes)
        clearSelection()
    }

    private fun captureSelection(start: Pair<Int, Int>, end: Pair<Int, Int>) {
        val canvas = _canvas.value
        val minRow = minOf(start.first, end.first).coerceIn(0, canvas.height - 1)
        val maxRow = maxOf(start.first, end.first).coerceIn(0, canvas.height - 1)
        val minCol = minOf(start.second, end.second).coerceIn(0, canvas.width - 1)
        val maxCol = maxOf(start.second, end.second).coerceIn(0, canvas.width - 1)
        val content = mutableListOf<Pair<Pair<Int, Int>, Int>>()
        for (row in minRow..maxRow) {
            for (col in minCol..maxCol) {
                val color = canvas.getPixel(row, col)
                if (color != Color.Transparent.toArgb()) {
                    content.add((row to col) to color)
                }
            }
        }
        selectionContent = content
    }

    /** Pega la selección en una nueva posición */
    fun pasteSelection(atRow: Int, atCol: Int) {
        val content = selectionContent ?: return
        val canvas = _canvas.value
        val changes = mutableMapOf<Pair<Int, Int>, Int>()
        val start = selectionStart ?: return
        val rowOffset = atRow - start.first
        val colOffset = atCol - start.second
        content.forEach { entry ->
            val (position, color) = entry
            val (r, c) = position
            val newRow = r + rowOffset
            val newCol = c + colOffset
            if (newRow in 0 until canvas.height && newCol in 0 until canvas.width) {
                changes[newRow to newCol] = color
            }
        }
        applyChanges(changes)
    }

    // ============ HISTORIAL ============

    private fun applyChanges(changes: Map<Pair<Int, Int>, Int>) {
        if (changes.isEmpty()) return
        // Si había un trazo abierto (gesto interrumpido), se cierra primero para
        // que el historial quede en orden: deshacer nunca borra de más.
        if (pendingStroke != null) endStroke()
        val newCanvas = _canvas.value.setPixels(changes)
        pushToHistory(newCanvas)
        _canvas.value = newCanvas
        isDirty = true
        checkAutoSave()
    }

    /**
     * Guarda un estado en el historial.
     *
     * Con [mergeWithPrevious] los cambios muy seguidos (por ejemplo un trazo que
     * el sistema dividió en dos gestos) se funden en UNA sola entrada: deshacer
     * siempre devuelve "la línea completa", nunca píxel a píxel.
     */
    private fun pushToHistory(newCanvas: PixelCanvas, mergeWithPrevious: Boolean = false) {
        val now = System.currentTimeMillis()
        val canMerge = mergeWithPrevious &&
            undoStack.isNotEmpty() &&
            (now - lastPushAt) <= MERGE_WINDOW_MS

        if (canMerge) {
            // Se reemplaza la cima: el historial no crece y deshacer retrocede
            // al estado anterior a TODO el grupo de trazos.
            undoStack[undoStack.lastIndex] = newCanvas
        } else {
            undoStack.add(newCanvas)
            if (undoStack.size > MAX_HISTORY) {
                undoStack.removeAt(0)
            }
        }
        redoStack.clear()
        lastPushAt = now
    }

    /** Marca sucio también al deshacer/rehacer para que el autosave persista. */
    private fun markDirtyFromHistory() {
        isDirty = true
        checkAutoSave()
    }

    fun undo() {
        // Un trazo a medio hacer (gesto cancelado) se cierra antes de deshacer,
        // para que el botón de deshacer nunca quede bloqueado.
        if (pendingStroke != null) endStroke()
        if (undoStack.size <= 1) return
        val current = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(current)
        _canvas.value = undoStack.last()
        markDirtyFromHistory()
    }

    fun redo() {
        if (pendingStroke != null) return
        if (redoStack.isEmpty()) return
        val next = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(next)
        _canvas.value = next
        markDirtyFromHistory()
    }

    // ============ GUARDADO Y EXPORTACIÓN ============

    /** Miniatura PNG (128px, conserva alpha) en Base64 para la galería. */
    private fun buildThumbnail(canvas: PixelCanvas): String = runCatching {
        val src = canvas.toBitmap()
        val scaled = Bitmap.createScaledBitmap(src, 128, 128, false)
        val bos = java.io.ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.PNG, 90, bos)
        android.util.Base64.encodeToString(bos.toByteArray(), android.util.Base64.NO_WRAP)
    }.getOrDefault("")

    /**
     * Guarda (upsert) contra el MISMO id siempre. La primera vez crea el
     * registro y memoriza su id; después solo actualiza ese archivo.
     * Con dirty flag: si no hubo cambios, no escribe nada.
     * Actualiza la miniatura del MISMO registro (no crea entradas nuevas).
     */
    fun saveProject(name: String? = null) {
        if (!isDirty && projectId != null) return // nada cambió desde el último guardado
        viewModelScope.launch {
            val canvas = _canvas.value
            val thumb = buildThumbnail(canvas)
            val existingId = projectId
            val project = if (existingId != null) {
                Project(id = existingId, canvas = canvas.copy(name = name ?: canvas.name), thumbnailBase64 = thumb)
            } else {
                val created = Project(canvas = canvas.copy(name = name ?: canvas.name), thumbnailBase64 = thumb)
                projectId = created.id
                created
            }
            repository.saveProject(project)
            isDirty = false
            _autoSaveTick.value++
            _autoSaveStatus.value = "Guardado automáticamente"
        }
    }

    private fun checkAutoSave() {
        strokeCount++
        if (strokeCount >= AUTO_SAVE_THRESHOLD) {
            strokeCount = 0
            saveProject()
        }
    }

    fun forceAutoSave() {
        strokeCount = AUTO_SAVE_THRESHOLD
        checkAutoSave()
    }

    /** Apaga el aviso de autoguardado (lo llama la UI tras mostrarlo unos segundos). */
    fun clearAutoSaveStatus() {
        _autoSaveStatus.value = ""
    }

    /**
     * Guardado síncrono al SALIR de la pantalla: no depende del viewModelScope
     * (que puede cancelarse al destruirse la entrada de navegación).
     * Respeta el dirty flag; upsert contra el id estable.
     */
    fun saveOnExit() {
        if (!isDirty) return
        val canvas = _canvas.value
        val thumb = buildThumbnail(canvas)
        val id = projectId
        val project = if (id != null) {
            Project(id = id, canvas = canvas, thumbnailBase64 = thumb)
        } else {
            val created = Project(canvas = canvas, thumbnailBase64 = thumb)
            projectId = created.id
            created
        }
        runCatching { kotlinx.coroutines.runBlocking { repository.saveProject(project) } }
        isDirty = false
        _autoSaveStatus.value = "Guardado automáticamente"
    }

    /**
     * Red de seguridad: si la pantalla desaparece por cualquier vía (cambio de
     * configuración, cierre del proceso, navegación inesperada), el dibujo se
     * guarda igual. [saveOnExit] es idempotente porque respeta el dirty flag.
     */
    override fun onCleared() {
        saveOnExit()
        super.onCleared()
    }

    /** Exporta a PNG con escalado nearest-neighbor (1x, 2x, 4x) */
    fun exportPng(scale: Int = 1): Bitmap {
        val bitmap = _canvas.value.toBitmap()
        if (scale <= 1) return bitmap
        return Bitmap.createScaledBitmap(bitmap, bitmap.width * scale, bitmap.height * scale, false)
    }

    /** Borra todo el lienzo (con confirmación en UI) */
    fun clearCanvas() {
        val current = _canvas.value
        val emptyCanvas = PixelCanvas.empty(current.width, current.height, current.name)
        pushToHistory(emptyCanvas)
        _canvas.value = emptyCanvas
        isDirty = true
    }

    // ============ COLORES GUARDADOS ============

    private fun loadSavedColors() {
        viewModelScope.launch {
            _savedColors.value = savedColorsRepo.getSavedColors()
        }
    }

    fun removeSavedColor(color: Int) {
        viewModelScope.launch {
            savedColorsRepo.removeColor(color)
            _savedColors.value = savedColorsRepo.getSavedColors()
        }
    }

    // ============ CONVERSIÓN DE FOTO ============

    private fun loadPhotoAndPixelate(uriString: String, context: Context) {
        viewModelScope.launch {
            try {
                val uri = Uri.parse(uriString)
                val bitmap = withContext(Dispatchers.IO) {
                    android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                if (bitmap != null) {
                    originalPhotoBitmap = bitmap
                    // Tamaño recomendado según imagen original
                    val recommendedIdx = PhotoPixelator.recommendedDetailLevel(bitmap)
                    val targetSize = PhotoPixelator.detailLevels[recommendedIdx]

                    // Pixelar en background
                    val pixelated = PhotoPixelator.pixelate(bitmap, targetSize)

                    // Crear bitmap de referencia (escalado manteniendo aspecto)
                    val reference = PhotoPixelator.createReferenceBitmap(bitmap, targetSize)

                    // Actualizar lienzo con resultado pixelado
                    val newCanvas = PixelCanvas.fromBitmap(pixelated, "De foto")
                    pushToHistory(newCanvas)
                    _canvas.value = newCanvas
                    isDirty = true

                    // Guardar referencia para capa semitransparente
                    _referenceBitmap.value = reference
                    _showReference.value = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /** Cambia el nivel de detalle y repixela la foto original */
    fun changeDetailLevel(levelIndex: Int) {
        val bitmap = originalPhotoBitmap ?: referenceBitmap ?: return
        val targetSize = PhotoPixelator.detailLevels[levelIndex]
        viewModelScope.launch {
            val pixelated = PhotoPixelator.pixelate(bitmap, targetSize)
            val reference = PhotoPixelator.createReferenceBitmap(bitmap, targetSize)
            val newCanvas = PixelCanvas.fromBitmap(pixelated, "De foto")
            pushToHistory(newCanvas)
            _canvas.value = newCanvas
            _referenceBitmap.value = reference
            isDirty = true
        }
    }
}
