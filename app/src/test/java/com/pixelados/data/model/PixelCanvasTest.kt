package com.pixelados.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reglas del producto verificadas sin dispositivo:
 * 1 celda de la cuadrícula = 1 píxel del dibujo, relleno por completo.
 */
class PixelCanvasTest {

    private val transparent = Color.Transparent.toArgb()
    private val red = Color.Red.toArgb()
    private val blue = Color.Blue.toArgb()

    @Test
    fun `lienzo nuevo empieza vacio y con el tamano exacto`() {
        val canvas = PixelCanvas.empty(16, 3)

        assertEquals(16, canvas.width)
        assertEquals(3, canvas.height)
        assertEquals(48, canvas.pixels.size)
        assertTrue(canvas.pixels.all { it == transparent })
    }

    @Test
    fun `un pixel pintado llena una sola celda y no toca las vecinas`() {
        val canvas = PixelCanvas.empty(4, 4).setPixel(2, 1, red)

        assertEquals(red, canvas.getPixel(2, 1))
        assertEquals(transparent, canvas.getPixel(2, 0))
        assertEquals(transparent, canvas.getPixel(2, 2))
        assertEquals(transparent, canvas.getPixel(1, 1))
        assertEquals(transparent, canvas.getPixel(3, 1))
    }

    @Test
    fun `escribir fuera del lienzo no cambia nada`() {
        val canvas = PixelCanvas.empty(4, 4)

        assertEquals(canvas, canvas.setPixel(4, 0, red))
        assertEquals(canvas, canvas.setPixel(0, -1, red))
        assertEquals(transparent, canvas.getPixel(99, 99))
    }

    @Test
    fun `rellenar pinta solo la zona contigua del mismo color`() {
        // Cuadro 3x3 con una linea vertical roja en la columna 1 que separa zonas
        var canvas = PixelCanvas.empty(3, 3)
        canvas = canvas.setPixel(0, 1, red)
        canvas = canvas.setPixel(1, 1, red)
        canvas = canvas.setPixel(2, 1, red)

        // Rellenar desde la esquina izquierda: solo columna 0
        val changes = canvas.floodFillChanges(0, 0, blue)

        assertEquals(3, changes.size)
        assertTrue(changes.keys.all { it.second == 0 })

        val filled = canvas.setPixels(changes)
        assertEquals(blue, filled.getPixel(0, 0))
        assertEquals(blue, filled.getPixel(2, 0))
        assertEquals(red, filled.getPixel(1, 1))
        assertEquals(transparent, filled.getPixel(1, 2))
    }

    @Test
    fun `rellenar con el mismo color no genera cambios`() {
        val canvas = PixelCanvas.empty(2, 2)

        assertTrue(canvas.floodFillChanges(0, 0, transparent).isEmpty())
    }

    @Test
    fun `reemplazar cambia todas las celdas de un color en todo el lienzo`() {
        var canvas = PixelCanvas.empty(2, 2)
        canvas = canvas.setPixel(0, 0, red)
        canvas = canvas.setPixel(1, 1, red)
        canvas = canvas.setPixel(0, 1, blue)

        val replaced = canvas.setPixels(canvas.replaceColorChanges(red, blue))

        assertFalse(replaced.pixels.contains(red))
        assertEquals(3, replaced.pixels.count { it == blue })
        assertTrue(canvas.replaceColorChanges(blue, blue).isEmpty())
    }

    @Test
    fun `el espejo duplica la celda pintada en horizontal vertical y ambas`() {
        val canvas = PixelCanvas.empty(4, 4)
        val origen = mapOf((0 to 1) to red)

        val soloH = canvas.mirrorChanges(origen, horizontal = true, vertical = false)
        assertEquals(2, soloH.size)
        assertEquals(red, soloH[0 to 2])

        val soloV = canvas.mirrorChanges(origen, horizontal = false, vertical = true)
        assertEquals(2, soloV.size)
        assertEquals(red, soloV[3 to 1])

        val ambas = canvas.mirrorChanges(origen, horizontal = true, vertical = true)
        assertEquals(4, ambas.size)
        assertEquals(red, ambas[3 to 2])
    }

    @Test
    fun `el espejo mantiene dos celdas en lienzos de ancho par`() {
        val canvas = PixelCanvas.empty(4, 4)
        // En un lienzo de 4 columnas no hay columna central: 1 se refleja en 2
        val reflejado = canvas.mirrorChanges(mapOf((1 to 1) to red), horizontal = true, vertical = false)

        assertEquals(2, reflejado.size)
        assertEquals(red, reflejado[1 to 2])
    }

    @Test
    fun `un trazo de varias celdas se aplica de una sola vez`() {
        val canvas = PixelCanvas.empty(3, 1)
        val trazo = mapOf((0 to 0) to red, (0 to 1) to red, (0 to 2) to red)

        val pintado = canvas.setPixels(trazo)

        assertTrue(pintado.pixels.all { it == red })
    }
}
