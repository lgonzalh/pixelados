package com.pixelados.data.repository

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

/**
 * Persistencia de "Mis colores" - colores guardados por el usuario para reutilizar entre dibujos.
 * Máximo 24 colores guardados.
 */
class SavedColorsRepository(private val context: Context) {

    private val file = File(context.filesDir, "saved_colors.json")
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Obtiene colores guardados (máx 24) */
    suspend fun getSavedColors(): List<Int> = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext emptyList()
        try {
            val jsonString = file.readText()
            val list = json.decodeFromString(ListSerializer(Int.serializer()), jsonString)
            list.take(24)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Agrega un color al inicio de la lista (elimina duplicados, limita a 24) */
    suspend fun addColor(color: Int) = withContext(Dispatchers.IO) {
        var colors = getSavedColors()
        colors = colors.filter { it != color }
        colors = listOf(color) + colors
        if (colors.size > 24) colors = colors.take(24)
        saveColors(colors)
    }

    /** Elimina un color guardado */
    suspend fun removeColor(color: Int) = withContext(Dispatchers.IO) {
        var colors = getSavedColors()
        colors = colors.filter { it != color }
        saveColors(colors)
    }

    /** Limpia todos los colores guardados */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        file.delete()
    }

    private fun saveColors(colors: List<Int>) {
        file.writeText(json.encodeToString(ListSerializer(Int.serializer()), colors))
    }

    companion object {
        /** Color transparente para representar "borrador" en la paleta */
        const val TRANSPARENT_COLOR = 0x00000000
    }
}