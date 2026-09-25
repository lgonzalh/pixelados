package com.pixelados.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.serialization.Serializable

/**
 * Representa un lienzo de pixel art: matriz de colores (Int ARGB) + metadatos.
 * Las celdas transparentes se guardan como 0 (Color.Transparent).
 */
@Serializable
data class PixelCanvas(
    val width: Int,
    val height: Int,
    val pixels: List<Int>, // Lista plana: row * width + col
    val name: String = "Sin título",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Crea un lienzo vacío (todo transparente).
     */
    companion object {
        fun empty(width: Int, height: Int, name: String = "Sin título"): PixelCanvas {
            return PixelCanvas(
                width = width,
                height = height,
                pixels = List(width * height) { Color.Transparent.toArgb() },
                name = name
            )
        }

        /** Crea un PixelCanvas desde un Bitmap (para importar). */
        fun fromBitmap(bitmap: android.graphics.Bitmap, name: String = "Importado"): PixelCanvas {
            val width = bitmap.width
            val height = bitmap.height
            val pixelsArray = IntArray(width * height)
            bitmap.getPixels(pixelsArray, 0, width, 0, 0, width, height)
            return PixelCanvas(width, height, pixelsArray.toList(), name)
        }
    }

    /** Obtiene el color de una celda (row, col). */
    fun getPixel(row: Int, col: Int): Int {
        if (row !in 0 until height || col !in 0 until width) return Color.Transparent.toArgb()
        return pixels[row * width + col]
    }

    /** Devuelve una NUEVA instancia con el píxel modificado (inmutabilidad). */
    fun setPixel(row: Int, col: Int, color: Int): PixelCanvas {
        if (row !in 0 until height || col !in 0 until width) return this
        val newPixels = pixels.toMutableList()
        newPixels[row * width + col] = color
        return copy(pixels = newPixels, updatedAt = System.currentTimeMillis())
    }

    /** Devuelve una NUEVA instancia con múltiples píxeles modificados. */
    fun setPixels(changes: Map<Pair<Int, Int>, Int>): PixelCanvas {
        if (changes.isEmpty()) return this
        val newPixels = pixels.toMutableList()
        changes.forEach { (pos, color) ->
            val (row, col) = pos
            if (row in 0 until height && col in 0 until width) {
                newPixels[row * width + col] = color
            }
        }
        return copy(pixels = newPixels, updatedAt = System.currentTimeMillis())
    }

    /**
     * Región contigua (4 direcciones) del MISMO color que contiene (row, col).
     * Devuelve las posiciones afectadas. Pura y testeable: no toca Android.
     */
    fun regionOf(row: Int, col: Int): Set<Pair<Int, Int>> {
        if (row !in 0 until height || col !in 0 until width) return emptySet()
        val target = getPixel(row, col)
        val visited = HashSet<Pair<Int, Int>>()
        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.addLast(row to col)
        while (queue.isNotEmpty()) {
            val (r, c) = queue.removeFirst()
            if (r !in 0 until height || c !in 0 until width) continue
            if (r to c in visited) continue
            if (getPixel(r, c) != target) continue
            visited.add(r to c)
            queue.addLast(r - 1 to c)
            queue.addLast(r + 1 to c)
            queue.addLast(r to c - 1)
            queue.addLast(r to c + 1)
        }
        return visited
    }

    /** Cambios necesarios para rellenar con [newColor] la región que contiene (row, col). */
    fun floodFillChanges(row: Int, col: Int, newColor: Int): Map<Pair<Int, Int>, Int> {
        if (row !in 0 until height || col !in 0 until width) return emptyMap()
        if (getPixel(row, col) == newColor) return emptyMap()
        return regionOf(row, col).associateWith { newColor }
    }

    /** Cambios necesarios para convertir TODAS las celdas de [fromColor] en [toColor]. */
    fun replaceColorChanges(fromColor: Int, toColor: Int): Map<Pair<Int, Int>, Int> {
        if (fromColor == toColor) return emptyMap()
        val changes = LinkedHashMap<Pair<Int, Int>, Int>()
        pixels.forEachIndexed { index, color ->
            if (color == fromColor) {
                changes[(index / width) to (index % width)] = toColor
            }
        }
        return changes
    }

    /**
     * Espeja (simetría) un conjunto de cambios. Devuelve un mapa NUEVO que
     * incluye los cambios originales más sus reflejos horizontal/vertical.
     * Pura y testeable.
     */
    fun mirrorChanges(
        changes: Map<Pair<Int, Int>, Int>,
        horizontal: Boolean,
        vertical: Boolean
    ): Map<Pair<Int, Int>, Int> {
        if (changes.isEmpty() || (!horizontal && !vertical)) return changes
        val result = LinkedHashMap(changes)
        changes.entries.forEach { (pos, color) ->
            val (row, col) = pos
            val mirroredCol = width - 1 - col
            val mirroredRow = height - 1 - row
            if (horizontal) result[row to mirroredCol] = color
            if (vertical) result[mirroredRow to col] = color
            if (horizontal && vertical) result[mirroredRow to mirroredCol] = color
        }
        return result
    }

    /** Convierte a Bitmap para exportar/renderizar. */
    fun toBitmap(): android.graphics.Bitmap {
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val pixelsArray = pixels.toIntArray()
        bitmap.setPixels(pixelsArray, 0, width, 0, 0, width, height)
        return bitmap
    }
}
