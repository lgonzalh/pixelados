package com.pixelados.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Cómo se estampa un símbolo:
 *  - [FILLED]: relleno, con el color activo.
 *  - [OUTLINE]: solo el contorno, con el color activo.
 *  - [ORIGINAL]: con los colores propios de la figura (los Pokémon los traen).
 */
enum class StampStyle(val label: String) {
    FILLED("Relleno"),
    OUTLINE("Contorno"),
    ORIGINAL("Original")
}

/**
 * Símbolo (sello) que se puede estampar en el lienzo.
 *
 * [pattern] es la máscara de la figura: cada celda rellena se pinta con el color
 * activo al estampar, así que un mismo símbolo sirve con cualquier color.
 * El catálogo completo vive en [StampCatalog] (figuras sin repetir).
 */
data class Stamp(
    val name: String,
    val category: String = "",
    val pattern: List<List<Int>>, // Matriz [row][col], Color.Transparent = hueco
    val defaultColor: Int = Color.Black.toArgb(),
    /** true si la figura trae sus propios colores (p. ej. los Pokémon). */
    val hasOwnColors: Boolean = false
) {
    val width: Int get() = pattern.firstOrNull()?.size ?: 0
    val height: Int get() = pattern.size

    /** Máscara según el estilo: relleno (normal) o solo el contorno. */
    val outline: List<List<Int>> by lazy { buildOutline() }

    /** true si la celda debe pintarse con el estilo elegido. */
    fun isOn(row: Int, col: Int, filled: Boolean): Boolean {
        val mask = if (filled) pattern else outline
        return mask.getOrNull(row)?.getOrNull(col)?.let { it != Color.Transparent.toArgb() } ?: false
    }

    /** Máscara lista para dibujar (rellena o de contorno). */
    fun mask(filled: Boolean): List<List<Int>> = if (filled) pattern else outline

    /** Color de una celda según el estilo elegido. */
    fun colorAt(row: Int, col: Int, style: StampStyle, activeColor: Int): Int {
        val own = pattern.getOrNull(row)?.getOrNull(col) ?: Color.Transparent.toArgb()
        return if (style == StampStyle.ORIGINAL && hasOwnColors) own else activeColor
    }

    /**
     * Contorno de 1 píxel: se queda con las celdas rellenas que tienen algún
     * vecino vacío. Así una misma figura se puede estampar rellena o dibujada.
     */
    private fun buildOutline(): List<List<Int>> {
        val filledColor = Color.Black.toArgb()
        val empty = Color.Transparent.toArgb()
        fun isFilled(r: Int, c: Int): Boolean =
            pattern.getOrNull(r)?.getOrNull(c)?.let { it != empty } ?: false

        return List(height) { row ->
            List(width) { col ->
                val on = isFilled(row, col) &&
                    (!isFilled(row - 1, col) || !isFilled(row + 1, col) ||
                        !isFilled(row, col - 1) || !isFilled(row, col + 1))
                if (on) filledColor else empty
            }
        }
    }

    /** Aplica el sello en el lienzo en (centerRow, centerCol) con color dado */
    fun applyTo(
        canvas: PixelCanvas,
        centerRow: Int,
        centerCol: Int,
        color: Int = defaultColor,
        style: StampStyle = StampStyle.FILLED
    ): Map<Pair<Int, Int>, Int> {
        val changes = mutableMapOf<Pair<Int, Int>, Int>()
        val halfW = width / 2
        val halfH = height / 2
        val filled = style != StampStyle.OUTLINE
        for (row in 0 until height) {
            for (col in 0 until width) {
                if (isOn(row, col, filled)) {
                    val targetRow = centerRow - halfH + row
                    val targetCol = centerCol - halfW + col
                    if (targetRow in 0 until canvas.height && targetCol in 0 until canvas.width) {
                        changes[targetRow to targetCol] = colorAt(row, col, style, color)
                    }
                }
            }
        }
        return changes
    }

    companion object {
        /** Catálogo completo de símbolos disponibles. */
        val allStamps: List<Stamp> get() = StampCatalog.all + PokemonCatalog.all

        /** Símbolos de una categoría (null = todos). */
        fun byCategory(category: String?): List<Stamp> =
            if (category == null) allStamps else allStamps.filter { it.category == category }

        val categories: List<String> get() = StampCatalog.categories + PokemonCatalog.category
    }
}
