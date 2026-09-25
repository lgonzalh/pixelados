package com.pixelados.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Paleta de la app: colores con SIGNIFICADO (no colores sueltos por pantalla).
 *
 * Cambiar de paleta recolorea fondo, barras, bordes, relleno de iconos y textos
 * en toda la app, porque ninguna pantalla tiene colores fijos propios.
 */
@Immutable
data class PixelColors(
    val id: String,
    val label: String,
    val dark: Boolean,
    val primary: Color,
    val onPrimary: Color,
    val accent: Color,
    val onAccent: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val bar: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val border: Color,
    val danger: Color,
    val workspace: Color
)

/** Paletas disponibles para elegir en Ajustes. */
enum class PaletteId(val id: String, val label: String, val primary: Color, val accent: Color) {
    LAVANDA("lavanda", "Lavanda", Color(0xFF6C63FF), Color(0xFFFF7AB6)),
    OCEANO("oceano", "Océano", Color(0xFF2F80ED), Color(0xFF35C6E8)),
    BOSQUE("bosque", "Bosque", Color(0xFF2E9E63), Color(0xFFA8D948)),
    FRAMBUESA("frambuesa", "Frambuesa", Color(0xFFD6336C), Color(0xFF845EF7)),
    ATARDECER("atardecer", "Atardecer", Color(0xFFF0653A), Color(0xFFF5B93B)),
    GRAFITO("grafito", "Grafito", Color(0xFF4B5563), Color(0xFF94A3B8));

    companion object {
        val default: PaletteId = LAVANDA
        fun fromId(id: String?): PaletteId = entries.firstOrNull { it.id == id } ?: default
    }
}

private fun mix(base: Color, tint: Color, amount: Float): Color = Color(
    red = base.red + (tint.red - base.red) * amount,
    green = base.green + (tint.green - base.green) * amount,
    blue = base.blue + (tint.blue - base.blue) * amount,
    alpha = base.alpha
)

/**
 * Construye la paleta completa. Los neutros dependen solo del modo (claro/oscuro)
 * y el color de la paleta los tiñe suavemente: así todas las combinaciones se ven
 * coherentes sin tener que definirlas a mano una por una.
 */
object PixelThemes {

    fun colors(palette: PaletteId, dark: Boolean): PixelColors {
        return if (dark) darkColors(palette) else lightColors(palette)
    }

    private fun lightColors(palette: PaletteId): PixelColors {
        val primary = palette.primary
        val accent = palette.accent
        return PixelColors(
            id = palette.id,
            label = palette.label,
            dark = false,
            primary = primary,
            onPrimary = Color.White,
            accent = accent,
            onAccent = Color(0xFF1B1B22),
            background = mix(Color(0xFFF7F8FC), primary, 0.05f),
            surface = Color.White,
            surfaceVariant = mix(Color(0xFFFFFFFF), primary, 0.12f),
            bar = mix(Color(0xFFFFFFFF), primary, 0.12f).copy(alpha = 0.92f),
            textPrimary = Color(0xFF14161C),
            textSecondary = Color(0xFF5B6070),
            textDisabled = Color(0xFFA9AEBB),
            // Bordes algo más marcados: en claro las tarjetas blancas se perdían.
            border = mix(Color(0xFFD6DAE6), primary, 0.12f),
            danger = Color(0xFFE5484D),
            workspace = mix(Color(0xFFDDE1EA), primary, 0.10f)
        )
    }

    private fun darkColors(palette: PaletteId): PixelColors {
        // En oscuro el color principal se aclara para que se lea sobre negro.
        val primary = mix(palette.primary, Color.White, 0.22f)
        val accent = mix(palette.accent, Color.White, 0.18f)
        return PixelColors(
            id = palette.id,
            label = palette.label,
            dark = true,
            primary = primary,
            onPrimary = Color(0xFF10121A),
            accent = accent,
            onAccent = Color(0xFF10121A),
            background = mix(Color(0xFF0E1016), palette.primary, 0.06f),
            surface = mix(Color(0xFF181B23), palette.primary, 0.05f),
            surfaceVariant = mix(Color(0xFF2B3242), palette.primary, 0.12f),
            bar = mix(Color(0xFF1D222C), palette.primary, 0.16f).copy(alpha = 0.94f),
            textPrimary = Color(0xFFF3F4F8),
            textSecondary = Color(0xFFA7ADBC),
            textDisabled = Color(0xFF6B7180),
            // En oscuro los bordes tenues hacían desaparecer las tarjetas.
            border = mix(Color(0xFF414A5E), palette.primary, 0.14f),
            danger = Color(0xFFFF6B6B),
            workspace = mix(Color(0xFF12141B), palette.primary, 0.06f)
        )
    }
}
