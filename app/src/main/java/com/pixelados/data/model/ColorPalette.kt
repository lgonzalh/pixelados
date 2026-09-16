package com.pixelados.data.model

import androidx.compose.ui.graphics.Color

/**
 * Paleta de 256 colores organizada por familias para niños.
 * Cada familia tiene 32 colores (8 tonos × 4 saturaciones).
 */
object ColorPalette {

    /** Nombres simples para búsqueda por voz/texto (ej. "rojo", "azul cielo") */
    val colorNames: Map<Int, String> = mapOf(
        // Rojos
        0xFFFF0000.toInt() to "rojo",
        0xFFFF4444.toInt() to "rojo claro",
        0xFFFF8888.toInt() to "rosa",
        0xFFFFCCCC.toInt() to "rosa pálido",
        0xFFCC0000.toInt() to "rojo oscuro",
        0xFF880000.toInt() to "granate",
        // Naranjas
        0xFFFFA500.toInt() to "naranja",
        0xFFFF8800.toInt() to "naranja vivo",
        0xFFFFBB00.toInt() to "ámbar",
        0xFFFFDD88.toInt() to "durazno",
        0xFFCC6600.toInt() to "naranja oscuro",
        0xFF884400.toInt() to "marrón claro",
        // Amarillos
        0xFFFFFF00.toInt() to "amarillo",
        0xFFFFFF44.toInt() to "amarillo claro",
        0xFFFFFF88.toInt() to "crema",
        0xFFFFFFCC.toInt() to "vainilla",
        0xFFCCCC00.toInt() to "mostaza",
        0xFF888800.toInt() to "oliva",
        // Verdes
        0xFF00FF00.toInt() to "verde",
        0xFF44FF44.toInt() to "verde claro",
        0xFF88FF88.toInt() to "menta",
        0xFFCCFFCC.toInt() to "verde pálido",
        0xFF00CC00.toInt() to "verde oscuro",
        0xFF008800.toInt() to "bosque",
        // Verdes azulados
        0xFF00FFFF.toInt() to "cian",
        0xFF44FFFF.toInt() to "cian claro",
        0xFF88FFFF.toInt() to "aguamarina",
        0xFFCCFFFF.toInt() to "azul pálido",
        0xFF00CCCC.toInt() to "turquesa",
        0xFF008888.toInt() to "verde azulado",
        // Azules
        0xFF0000FF.toInt() to "azul",
        0xFF4444FF.toInt() to "azul claro",
        0xFF8888FF.toInt() to "lavanda",
        0xFFCCCCFF.toInt() to "azul pálido",
        0xFF0000CC.toInt() to "azul oscuro",
        0xFF000088.toInt() to "marino",
        // Morados
        0xFFFF00FF.toInt() to "magenta",
        0xFFFF44FF.toInt() to "rosa brillante",
        0xFFFF88FF.toInt() to "lila",
        0xFFFFCCFF.toInt() to "lila pálido",
        0xFFCC00CC.toInt() to "púrpura",
        0xFF880088.toInt() to "violeta",
        // Rosas
        0xFFFF0088.toInt() to "rosa fuerte",
        0xFFFF44AA.toInt() to "fucsia",
        0xFFFF88CC.toInt() to "rosa pastel",
        0xFFFFCCEE.toInt() to "rosa muy claro",
        0xFFCC0066.toInt() to "rosa oscuro",
        0xFF880044.toInt() to "burdeos",
        // Marrones
        0xFF8B4513.toInt() to "marrón",
        0xFFA0522D.toInt() to "siena",
        0xFFD2691E.toInt() to "chocolate",
        0xFFCD853F.toInt() to "perú",
        0xFFDEB887.toInt() to "madera",
        0xFFF5DEB3.toInt() to "trigo",
        // Grises
        0xFF000000.toInt() to "negro",
        0xFF333333.toInt() to "gris muy oscuro",
        0xFF666666.toInt() to "gris oscuro",
        0xFF999999.toInt() to "gris",
        0xFFCCCCCC.toInt() to "gris claro",
        0xFFEEEEEE.toInt() to "gris muy claro",
        0xFFFFFFFF.toInt() to "blanco"
    )

    /** Familias de colores para organización visual */
    data class ColorFamily(
        val name: String,
        val icon: String,
        val colors: List<Int>
    )

    val colorFamilies: List<ColorFamily> = listOf(
        ColorFamily(
            name = "Rojos",
            icon = "heart",
            colors = generateFamily(0xFFFF0000.toInt(), 0xFFCC0000.toInt(), 0xFF880000.toInt(), 0xFF440000.toInt())
        ),
        ColorFamily(
            name = "Naranjas",
            icon = "circle",
            colors = generateFamily(0xFFFFA500.toInt(), 0xFFCC6600.toInt(), 0xFF884400.toInt(), 0xFF442200.toInt())
        ),
        ColorFamily(
            name = "Amarillos",
            icon = "star",
            colors = generateFamily(0xFFFFFF00.toInt(), 0xFFCCCC00.toInt(), 0xFF888800.toInt(), 0xFF444400.toInt())
        ),
        ColorFamily(
            name = "Verdes",
            icon = "leaf",
            colors = generateFamily(0xFF00FF00.toInt(), 0xFF00CC00.toInt(), 0xFF008800.toInt(), 0xFF004400.toInt())
        ),
        ColorFamily(
            name = "Azules",
            icon = "water",
            colors = generateFamily(0xFF0000FF.toInt(), 0xFF0000CC.toInt(), 0xFF000088.toInt(), 0xFF000044.toInt())
        ),
        ColorFamily(
            name = "Morados",
            icon = "magic",
            colors = generateFamily(0xFFFF00FF.toInt(), 0xFFCC00CC.toInt(), 0xFF880088.toInt(), 0xFF440044.toInt())
        ),
        ColorFamily(
            name = "Rosados",
            icon = "favorite",
            colors = generateFamily(0xFFFF0088.toInt(), 0xFFCC0066.toInt(), 0xFF880044.toInt(), 0xFF440022.toInt())
        ),
        ColorFamily(
            name = "Marrones",
            icon = "nature",
            colors = generateFamily(0xFF8B4513.toInt(), 0xFFA0522D.toInt(), 0xFFD2691E.toInt(), 0xFFCD853F.toInt())
        ),
        ColorFamily(
            name = "Grises",
            icon = "circle_outline",
            colors = generateFamily(0xFF000000.toInt(), 0xFF333333.toInt(), 0xFF666666.toInt(), 0xFF999999.toInt())
        )
    )

    /** Genera 32 colores por familia: 8 tonos × 4 saturaciones */
    private fun generateFamily(
        baseBright: Int,
        baseMid: Int,
        baseDark: Int,
        baseVeryDark: Int
    ): List<Int> {
        val colors = mutableListOf<Int>()
        val bases = listOf(baseBright, baseMid, baseDark, baseVeryDark)
        bases.forEach { base ->
            val r = (base shr 16) and 0xFF
            val g = (base shr 8) and 0xFF
            val b = base and 0xFF
            // 8 variaciones de saturación por tono base
            (0..7).forEach { i ->
                val factor = 1.0 - (i * 0.12)
                val nr = (r * factor).toInt().coerceIn(0, 255)
                val ng = (g * factor).toInt().coerceIn(0, 255)
                val nb = (b * factor).toInt().coerceIn(0, 255)
                colors.add((0xFF000000L or ((nr and 0xFF) shl 16).toLong() or ((ng and 0xFF) shl 8).toLong() or (nb and 0xFF).toLong()).toInt())
            }
        }
        return colors
    }

    /** Color más cercano de la paleta (distancia RGB simple) */
    fun nearestPaletteColor(target: Int): Int {
        var bestColor = allColors[0]
        var bestDist = Int.MAX_VALUE
        val tr = (target shr 16) and 0xFF
        val tg = (target shr 8) and 0xFF
        val tb = target and 0xFF
        allColors.forEach { c ->
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            val dr = r - tr
            val dg = g - tg
            val db = b - tb
            val dist = dr * dr + dg * dg + db * db
            if (dist < bestDist) {
                bestDist = dist
                bestColor = c
            }
        }
        return bestColor
    }

    /** Nombre simple del color para búsqueda infantil */
    fun colorName(color: Int): String = colorNames[color] ?: "color"

    /** Todos los 256 colores en orden: familias → tonos → saturaciones */
    val allColors: List<Int> by lazy {
        buildList {
            colorFamilies.forEach { family ->
                family.colors.forEach { add(it) }
            }
        }
    }
}