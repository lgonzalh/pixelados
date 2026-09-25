package com.pixelados.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pixelados.R

/** Colores de la app accesibles desde cualquier pantalla. */
val LocalPixelColors = staticCompositionLocalOf { PixelThemes.colors(PaletteId.default, false) }

/** Acceso corto: `AppTheme.colors.primary`. */
object AppTheme {
    val colors: PixelColors
        @Composable get() = LocalPixelColors.current
}

/**
 * Tipografía redondeada: Quicksand (SIL Open Font License), con la "a" redonda
 * de caligrafía y todos los acentos y la ñ. Se usan cuatro pesos reales en vez
 * de negrita sintética.
 */
val RoundedFont = FontFamily(
    Font(R.font.quicksand_regular, FontWeight.Normal),
    Font(R.font.quicksand_medium, FontWeight.Medium),
    Font(R.font.quicksand_semibold, FontWeight.SemiBold),
    Font(R.font.quicksand_bold, FontWeight.Bold)
)

/**
 * Escala tipográfica proporcionada: se reducen los tamaños respecto a Material
 * para que la interfaz respire y las letras no dominen sobre los iconos.
 */
private val AppTypography: Typography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = RoundedFont),
        displayMedium = displayMedium.copy(fontFamily = RoundedFont),
        displaySmall = displaySmall.copy(fontFamily = RoundedFont),
        headlineLarge = headlineLarge.copy(fontFamily = RoundedFont, fontSize = 28.sp, fontWeight = FontWeight.SemiBold),
        headlineMedium = headlineMedium.copy(fontFamily = RoundedFont, fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
        headlineSmall = headlineSmall.copy(fontFamily = RoundedFont, fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontFamily = RoundedFont, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontFamily = RoundedFont, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontFamily = RoundedFont, fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(fontFamily = RoundedFont, fontSize = 14.sp),
        bodyMedium = bodyMedium.copy(fontFamily = RoundedFont, fontSize = 13.sp),
        bodySmall = bodySmall.copy(fontFamily = RoundedFont, fontSize = 11.5.sp),
        labelLarge = labelLarge.copy(fontFamily = RoundedFont, fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
        labelMedium = labelMedium.copy(fontFamily = RoundedFont, fontSize = 11.5.sp),
        labelSmall = labelSmall.copy(fontFamily = RoundedFont, fontSize = 10.5.sp)
    )
}

/** Estilo por defecto de los textos sueltos (los que no declaran `style`). */
val DefaultTextStyle = TextStyle(fontFamily = RoundedFont, fontSize = 13.5.sp)

@Composable
fun PixeladosTheme(
    paletteId: String = PaletteId.default.id,
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = PixelThemes.colors(PaletteId.fromId(paletteId), darkTheme)

    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            primaryContainer = colors.surfaceVariant,
            onPrimaryContainer = colors.textPrimary,
            secondary = colors.accent,
            onSecondary = colors.onAccent,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceVariant,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.border,
            error = colors.danger,
            onError = colors.onPrimary
        )
    } else {
        lightColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            primaryContainer = colors.surfaceVariant,
            onPrimaryContainer = colors.textPrimary,
            secondary = colors.accent,
            onSecondary = colors.onAccent,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceVariant,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.border,
            error = colors.danger,
            onError = colors.onPrimary
        )
    }

    CompositionLocalProvider(LocalPixelColors provides colors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AppTypography,
            content = content
        )
    }
}
