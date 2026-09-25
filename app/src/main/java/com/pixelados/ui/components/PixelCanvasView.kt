package com.pixelados.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.pixelados.data.model.PixelCanvas
import com.pixelados.data.model.Stamp
import com.pixelados.data.model.StampStyle
import com.pixelados.data.model.Tool
import com.pixelados.ui.theme.AppTheme
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Acciones de celda para el manejo de entrada.
 *
 * [Up] es el final DEFINITIVO del gesto; [Cancel] avisa que el dedo salió del
 * lienzo o que el gesto se interrumpió. Separarlos permite que los sellos se
 * coloquen solo al soltar el dedo (y no en cada interrupción).
 */
enum class CellAction {
    Down,
    Drag,
    Up,
    Cancel
}

/** Reglas de interacción del lienzo (una sola fuente de verdad). */
private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 8f
private val ZOOM_STEPS = listOf(1f, 1.5f, 2f, 3f, 4f, 6f, 8f)

/** Ajusta el zoom al paso más cercano para que el trazo quede predecible. */
fun snapZoom(zoom: Float): Float =
    ZOOM_STEPS.minByOrNull { abs(it - zoom) } ?: MIN_ZOOM

/**
 * Geometría vigente del lienzo. Se pasa al detector de gestos a través de
 * `rememberUpdatedState` para que el gesto SIEMPRE use el tamaño, el
 * desplazamiento y la herramienta actuales (si no, el detector se queda con
 * los valores del primer trazado y "pinta con la herramienta anterior").
 */
private data class CanvasMetrics(
    val cellPx: Float,
    val fitCell: Float,
    val canvasW: Int,
    val canvasH: Int,
    val availW: Float,
    val availH: Float,
    val pan: Offset
) {
    val maxPanX: Float get() = max(0f, (canvasW - availW) / 2f)
    val maxPanY: Float get() = max(0f, (canvasH - availH) / 2f)
    val clampedPan: Offset
        get() = Offset(
            pan.x.coerceIn(-maxPanX, maxPanX),
            pan.y.coerceIn(-maxPanY, maxPanY)
        )
    val originX: Float get() = (availW - canvasW) / 2f + clampedPan.x
    val originY: Float get() = (availH - canvasH) / 2f + clampedPan.y

    /** Tamaño de celda (px enteros) para un zoom dado. */
    fun cellPxFor(zoom: Float): Float = floor(fitCell * zoom).coerceAtLeast(1f)

    fun cellAt(position: Offset, canvas: PixelCanvas): Pair<Int, Int>? {
        val col = floor((position.x - originX) / cellPx).toInt()
        val row = floor((position.y - originY) / cellPx).toInt()
        return if (row in 0 until canvas.height && col in 0 until canvas.width) row to col else null
    }
}

/**
 * Lienzo de pixel art.
 *
 * REGLAS DE PRODUCTO (pensadas para un niño de 10 años):
 *  - 1 celda de la cuadrícula = 1 píxel del dibujo, relleno por completo.
 *  - 1 dedo SIEMPRE pinta (nunca se desactiva el dibujo).
 *  - 2 dedos SOLO mueven/hacen zoom cuando la vista está suelta (candado abierto).
 *  - El trazo es continuo: si el dedo va rápido se rellenan las celdas intermedias.
 */
@Composable
fun PixelCanvasView(
    canvas: PixelCanvas,
    showGrid: Boolean,
    currentTool: Tool,
    showReference: Boolean,
    referenceBitmap: Bitmap?,
    referenceOpacity: Float,
    mirrorHorizontal: Boolean,
    mirrorVertical: Boolean,
    selectionStart: Pair<Int, Int>?,
    selectionEnd: Pair<Int, Int>?,
    activeStamp: Stamp?,
    /** Estilo del sello para la vista previa (fantasma) sobre el lienzo. */
    stampStyle: StampStyle = StampStyle.FILLED,
    zoom: Float,
    /** Cambiar este valor recentra el lienzo (botón "ajustar"). */
    fitSignal: Int = 0,
    onZoomChange: (Float) -> Unit,
    onCellAction: (row: Int, col: Int, action: CellAction) -> Unit,
    modifier: Modifier = Modifier
) {
    if (canvas.width <= 0 || canvas.height <= 0) return

    // Bitmap del dibujo: se recalcula solo cuando cambian los píxeles.
    val bitmap = remember(canvas.pixels) { canvas.toBitmap() }

    var panOffset by remember(canvas.width, canvas.height) { mutableStateOf(Offset.Zero) }
    var hoverCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // "Ajustar": vuelve al centro (el zoom lo controla la pantalla).
    androidx.compose.runtime.LaunchedEffect(fitSignal) {
        if (fitSignal > 0) panOffset = Offset.Zero
    }

    // El detector de gestos necesita el zoom, la geometría y los callbacks
    // vigentes sin reiniciarse en cada frame (si no, se queda "pegado" a la
    // herramienta y al tamaño del primer trazado).
    val currentZoom by rememberUpdatedState(zoom)
    val handleCellAction by rememberUpdatedState(onCellAction)
    val handleZoomChange by rememberUpdatedState(onZoomChange)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            // Fondo del área de trabajo: más oscuro que el lienzo para que el
            // niño distinga dónde empieza y termina su dibujo.
            .background(AppTheme.colors.workspace)
    ) {
        val availW = constraints.maxWidth.toFloat()
        val availH = constraints.maxHeight.toFloat()

        // Tamaño de celda en PÍXELES ENTEROS (fit-to-screen × zoom).
        // Entero ⇒ bitmap, cuadrícula y mapeo del dedo coinciden exactamente.
        val fitCell = min(availW / canvas.width, availH / canvas.height)
        val cellPx = floor(fitCell * zoom).coerceAtLeast(1f)
        val canvasW = (cellPx * canvas.width).toInt()
        val canvasH = (cellPx * canvas.height).toInt()

        // Límite de arrastre: nunca se puede sacar el lienzo de la pantalla.
        val maxPanX = max(0f, (canvasW - availW) / 2f)
        val maxPanY = max(0f, (canvasH - availH) / 2f)
        val originX = (availW - canvasW) / 2f + panOffset.x.coerceIn(-maxPanX, maxPanX)
        val originY = (availH - canvasH) / 2f + panOffset.y.coerceIn(-maxPanY, maxPanY)

        val metrics by rememberUpdatedState(
            CanvasMetrics(
                cellPx = cellPx,
                fitCell = fitCell,
                canvasW = canvasW,
                canvasH = canvasH,
                availW = availW,
                availH = availH,
                pan = panOffset
            )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                // OJO: el zoom NO es una clave del detector. Si lo fuera, al
                // ampliar se reiniciaba el gesto y se colocaba otro sello.
                .pointerInput(canvas.width, canvas.height) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var drawing = true
                        var ended = false
                        var lastCell = metrics.cellAt(down.position, canvas)
                        lastCell?.let {
                            hoverCell = it
                            handleCellAction(it.first, it.second, CellAction.Down)
                        }
                        var gestureZoom = currentZoom

                        // El "finally" garantiza que TODO trazo se cierre (Up) aunque
                        // el gesto se interrumpa: si no, el dibujo quedaría fuera del
                        // historial y deshacer borraría de más.
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val pressedChanges = event.changes.filter { it.pressed }
                                if (pressedChanges.isEmpty()) break

                                if (pressedChanges.size >= 2) {
                                    // 2 dedos = mover / acercar. El trazo en curso se cancela.
                                    if (drawing) {
                                        lastCell?.let { handleCellAction(it.first, it.second, CellAction.Cancel) }
                                        drawing = false
                                        ended = true
                                        hoverCell = null
                                    }
                                    val centroid = event.calculateCentroid(useCurrent = true)
                                    val previousCell = metrics.cellPx
                                    val newZoom = (gestureZoom * event.calculateZoom())
                                        .coerceIn(MIN_ZOOM, MAX_ZOOM)
                                    val newCell = metrics.cellPxFor(newZoom)
                                    // El punto del lienzo bajo los dedos se queda quieto:
                                    // se compensa el cambio de escala alrededor del centroide.
                                    val applied = if (previousCell > 0f) newCell / previousCell else 1f
                                    val newOriginX = centroid.x - (centroid.x - metrics.originX) * applied
                                    val newOriginY = centroid.y - (centroid.y - metrics.originY) * applied
                                    val newCanvasW = newCell * canvas.width
                                    val newCanvasH = newCell * canvas.height
                                    panOffset = Offset(
                                        newOriginX - (metrics.availW - newCanvasW) / 2f,
                                        newOriginY - (metrics.availH - newCanvasH) / 2f
                                    ) + event.calculatePan()
                                    gestureZoom = newZoom
                                    handleZoomChange(newZoom)
                                }
                                if (pressedChanges.size >= 2) {
                                    // nada más que hacer en este evento
                                } else if (drawing) {
                                    val change = pressedChanges.first()
                                    val cell = metrics.cellAt(change.position, canvas)
                                    if (cell == null) {
                                        // El dedo salió del lienzo: se cierra el trazo (no se
                                        // pinta al volver a entrar por otro lado) y no se
                                        // coloca ningún sello.
                                        if (lastCell != null) {
                                            handleCellAction(lastCell.first, lastCell.second, CellAction.Cancel)
                                            lastCell = null
                                            ended = true
                                            hoverCell = null
                                        }
                                    } else if (ended) {
                                        // Volvió a entrar: empieza un trazo nuevo en esa celda.
                                        ended = false
                                        lastCell = cell
                                        hoverCell = cell
                                        handleCellAction(cell.first, cell.second, CellAction.Down)
                                    } else if (cell != lastCell) {
                                        // Trazo continuo: pinta cada celda intermedia del recorrido.
                                        for (step in lineBetween(lastCell, cell)) {
                                            handleCellAction(step.first, step.second, CellAction.Drag)
                                        }
                                        lastCell = cell
                                        hoverCell = cell
                                    }
                                    change.consume()
                                }
                        }
                        } finally {
                            if (drawing && !ended && lastCell != null) {
                                handleCellAction(lastCell.first, lastCell.second, CellAction.Up)
                            }
                            hoverCell = null
                            // Se limita el desplazamiento actual (no el del inicio del gesto)
                            panOffset = Offset(
                                panOffset.x.coerceIn(-metrics.maxPanX, metrics.maxPanX),
                                panOffset.y.coerceIn(-metrics.maxPanY, metrics.maxPanY)
                            )
                        }
                    }
                }
                .pointerInput(Unit) {
                    // Al soltar los dedos el zoom queda en un paso legible (1×, 2×, …).
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                        } while (event.changes.any { it.pressed })
                        handleZoomChange(snapZoom(currentZoom))
                    }
                }
        ) {
            val themeColors = AppTheme.colors
            Canvas(modifier = Modifier.fillMaxSize()) {
                translate(originX, originY) {
                    val canvasWf = canvasW.toFloat()
                    val canvasHf = canvasH.toFloat()

                    // 0. Sombra dura (estilo pixel art) que separa el lienzo del fondo
                    drawRect(
                        color = Color(0x33000000),
                        topLeft = Offset(4f, 4f),
                        size = Size(canvasWf, canvasHf)
                    )

                    // 1. Fondo del lienzo: BLANCO liso (sin ajedrezado).
                    //    Al exportar, las celdas vacías siguen siendo transparentes.
                    drawRect(
                        color = Color.White,
                        topLeft = Offset.Zero,
                        size = Size(canvasWf, canvasHf)
                    )

                    // 2. Foto de referencia (semitransparente, debajo del dibujo)
                    if (showReference && referenceBitmap != null) {
                        drawReferenceImage(referenceBitmap, canvasW.toFloat(), canvasH.toFloat(), referenceOpacity)
                    }

                    // 3. Dibujo: 1 píxel del bitmap = 1 celda exacta, sin suavizado
                    drawImage(
                        image = bitmap.asImageBitmap(),
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(bitmap.width, bitmap.height),
                        dstOffset = IntOffset.Zero,
                        dstSize = IntSize(canvasW, canvasH),
                        filterQuality = FilterQuality.None
                    )

                    // 4. Cuadrícula fina
                    if (showGrid) {
                        drawGrid(canvas.width, canvas.height, cellPx, canvasW.toFloat(), canvasH.toFloat())
                    }

                    // 5. Selección
                    if (selectionStart != null && selectionEnd != null) {
                        drawSelectionOverlay(
                            selectionStart, selectionEnd,
                            canvas.width, canvas.height, cellPx,
                            accentColor = themeColors.primary
                        )
                    }

                    // 6. Guías de espejo
                    if (mirrorHorizontal || mirrorVertical) {
                        drawMirrorGuides(
                            mirrorHorizontal, mirrorVertical,
                            canvas.width, canvas.height, cellPx,
                            canvasWf, canvasHf,
                            guideColor = themeColors.accent
                        )
                    }

                    // 7. Borde del lienzo
                    drawRect(
                        color = Color(0x88000000),
                        topLeft = Offset.Zero,
                        size = Size(canvasWf, canvasHf),
                        style = Stroke(width = 2f)
                    )

                    // 8. Celda bajo el dedo (o el centro del lienzo para los
                    //    símbolos): el niño ve exactamente dónde va a quedar.
                    val celdaResaltada = if (currentTool == Tool.STAMPS) {
                        hoverCell ?: (canvas.height / 2 to canvas.width / 2)
                    } else {
                        hoverCell
                    }
                    celdaResaltada?.let { (row, col) ->
                        drawRect(
                            color = themeColors.primary,
                            topLeft = Offset(col * cellPx, row * cellPx),
                            size = Size(cellPx, cellPx),
                            style = Stroke(width = max(2f, cellPx / 6f))
                        )
                        // Vista previa del sello antes de soltar el dedo
                        if (currentTool == Tool.STAMPS && activeStamp != null) {
                            val halfW = activeStamp.width / 2
                            val halfH = activeStamp.height / 2
                            val relleno = stampStyle != StampStyle.OUTLINE
                            activeStamp.mask(relleno).forEachIndexed { r, colors ->
                                colors.forEachIndexed { c, color ->
                                    if (color != Color.Transparent.toArgb()) {
                                        val pintar = if (stampStyle == StampStyle.ORIGINAL && activeStamp.hasOwnColors) {
                                            Color(color).copy(alpha = 0.65f)
                                        } else {
                                            Color(0x99FFFF00)
                                        }
                                        drawRect(
                                            color = pintar,
                                            topLeft = Offset((col - halfW + c) * cellPx, (row - halfH + r) * cellPx),
                                            size = Size(cellPx, cellPx)
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

/** Celdas intermedias entre dos celdas (incluye el destino, no el origen). */
internal fun lineBetween(from: Pair<Int, Int>?, to: Pair<Int, Int>): List<Pair<Int, Int>> {
    if (from == null) return listOf(to)
    val (r0, c0) = from
    val (r1, c1) = to
    val steps = max(abs(r1 - r0), abs(c1 - c0))
    if (steps == 0) return emptyList()
    return (1..steps).map { i ->
        val r = r0 + ((r1 - r0) * i) / steps
        val c = c0 + ((c1 - c0) * i) / steps
        r to c
    }
}

private fun DrawScope.drawReferenceImage(
    refBitmap: Bitmap,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    referenceOpacity: Float
) {
    val scale = min(canvasWidthPx / refBitmap.width, canvasHeightPx / refBitmap.height)
    val drawWidth = refBitmap.width * scale
    val drawHeight = refBitmap.height * scale
    drawImage(
        image = refBitmap.asImageBitmap(),
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(refBitmap.width, refBitmap.height),
        dstOffset = IntOffset(
            ((canvasWidthPx - drawWidth) / 2).toInt(),
            ((canvasHeightPx - drawHeight) / 2).toInt()
        ),
        dstSize = IntSize(drawWidth.toInt(), drawHeight.toInt()),
        alpha = referenceOpacity
    )
}

private fun DrawScope.drawGrid(
    canvasWidth: Int,
    canvasHeight: Int,
    cellPx: Float,
    canvasWidthPx: Float,
    canvasHeightPx: Float
) {
    // Línea fina de 1 píxel de pantalla: separa sin tapar el color pintado.
    val gridColor = Color(0x33000000)
    for (i in 0..canvasWidth) {
        val x = i * cellPx
        drawLine(gridColor, Offset(x, 0f), Offset(x, canvasHeightPx), strokeWidth = 1f)
    }
    for (i in 0..canvasHeight) {
        val y = i * cellPx
        drawLine(gridColor, Offset(0f, y), Offset(canvasWidthPx, y), strokeWidth = 1f)
    }
}

private fun DrawScope.drawSelectionOverlay(
    start: Pair<Int, Int>,
    end: Pair<Int, Int>,
    canvasWidth: Int,
    canvasHeight: Int,
    cellPx: Float,
    accentColor: Color
) {
    val minRow = minOf(start.first, end.first).coerceIn(0, canvasHeight - 1)
    val maxRow = maxOf(start.first, end.first).coerceIn(0, canvasHeight - 1)
    val minCol = minOf(start.second, end.second).coerceIn(0, canvasWidth - 1)
    val maxCol = maxOf(start.second, end.second).coerceIn(0, canvasWidth - 1)

    val left = minCol * cellPx
    val top = minRow * cellPx
    val width = (maxCol - minCol + 1) * cellPx
    val height = (maxRow - minRow + 1) * cellPx

    drawRect(
        color = accentColor.copy(alpha = 0.22f),
        topLeft = Offset(left, top),
        size = Size(width, height)
    )
    drawRect(
        color = accentColor,
        topLeft = Offset(left, top),
        size = Size(width, height),
        style = Stroke(width = 3f)
    )
}

private fun DrawScope.drawMirrorGuides(
    mirrorHorizontal: Boolean,
    mirrorVertical: Boolean,
    canvasWidth: Int,
    canvasHeight: Int,
    cellPx: Float,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    guideColor: Color
) {
    if (mirrorHorizontal) {
        val x = (canvasWidth / 2f) * cellPx
        drawLine(
            color = guideColor,
            start = Offset(x, 0f),
            end = Offset(x, canvasHeightPx),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
        )
    }
    if (mirrorVertical) {
        val y = (canvasHeight / 2f) * cellPx
        drawLine(
            color = guideColor,
            start = Offset(0f, y),
            end = Offset(canvasWidthPx, y),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
        )
    }
}
