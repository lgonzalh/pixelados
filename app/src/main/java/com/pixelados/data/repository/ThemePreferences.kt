package com.pixelados.data.repository

import android.content.Context
import com.pixelados.ui.theme.PaletteId

/**
 * Guarda la apariencia elegida por el usuario: paleta de colores y modo
 * claro/oscuro. Se conserva entre sesiones y reinicios.
 */
class ThemePreferences(context: Context) {

    private val prefs = context.getSharedPreferences("pixelados_theme", Context.MODE_PRIVATE)

    /** Paleta activa (por defecto, la primera de la lista). */
    var paletteId: String
        get() = prefs.getString(KEY_PALETTE, PaletteId.default.id) ?: PaletteId.default.id
        set(value) = prefs.edit().putString(KEY_PALETTE, value).apply()

    /**
     * Modo oscuro: null = seguir al sistema. Se guarda explícito cuando el
     * usuario toca el interruptor de Ajustes.
     */
    var darkMode: Boolean?
        get() = if (prefs.contains(KEY_DARK)) prefs.getBoolean(KEY_DARK, false) else null
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_DARK) else putBoolean(KEY_DARK, value)
            }.apply()
        }

    private companion object {
        const val KEY_PALETTE = "palette_id"
        const val KEY_DARK = "dark_mode"
    }
}
