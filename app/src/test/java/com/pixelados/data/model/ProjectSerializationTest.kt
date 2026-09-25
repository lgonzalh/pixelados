package com.pixelados.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Guardar y volver a abrir un dibujo es la función más importante de la app.
 * Esta prueba protege la ida y vuelta completa (serializar → leer el archivo).
 */
class ProjectSerializationTest {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    @Test
    fun `un proyecto con nombre por defecto se puede volver a leer`() {
        val canvas = PixelCanvas.empty(4, 4).setPixel(0, 0, Color.Red.toArgb())
        val original = Project(canvas = canvas)

        val texto = json.encodeToString(original)
        val leido = json.decodeFromString<Project>(texto)

        assertEquals(original.id, leido.id)
        assertEquals(4, leido.canvas.width)
        assertEquals(Color.Red.toArgb(), leido.canvas.getPixel(0, 0))
    }

    @Test
    fun `un proyecto con nombre propio y miniatura se puede volver a leer`() {
        val canvas = PixelCanvas.empty(8, 8, name = "Mi dibujo")
        val original = Project(canvas = canvas, thumbnailBase64 = "QUJD")

        val texto = json.encodeToString(original)
        val leido = json.decodeFromString<Project>(texto)

        assertEquals("Mi dibujo", leido.name)
        assertEquals("QUJD", leido.thumbnailBase64)
        assertNotNull(leido.canvas)
    }

    @Test
    fun `se puede leer un archivo escrito por una version anterior`() {
        // Formato real de los archivos que ya están en el teléfono
        val antiguo = """
            {
                "id": "e29cf9ec-1a70-486a-be59-c4e8c2ea4ea7",
                "canvas": {
                    "width": 2,
                    "height": 2,
                    "pixels": [0, 0, 0, -65536],
                    "createdAt": 1790365179236,
                    "updatedAt": 1790365179236
                },
                "thumbnailBase64": "QUJD"
            }
        """.trimIndent()

        val leido = json.decodeFromString<Project>(antiguo)

        assertEquals(2, leido.canvas.width)
        assertEquals("Sin título", leido.name)
        assertEquals(-65536, leido.canvas.getPixel(1, 1))
    }
}
