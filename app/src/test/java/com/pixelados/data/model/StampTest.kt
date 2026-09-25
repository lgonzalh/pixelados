package com.pixelados.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Los símbolos se pueden estampar con relleno o solo con el contorno, y con el
 * color que elija el usuario. Estas pruebas protegen esas dos reglas.
 */
class StampTest {

    private val cuadrado = Stamp(
        name = "Cuadrado",
        category = "Formas",
        pattern = listOf(
            listOf(0, 0, 0, 0, 0),
            listOf(0, 1, 1, 1, 0),
            listOf(0, 1, 1, 1, 0),
            listOf(0, 1, 1, 1, 0),
            listOf(0, 0, 0, 0, 0)
        ).map { fila -> fila.map { if (it == 1) Color.Black.toArgb() else Color.Transparent.toArgb() } }
    )

    @Test
    fun `el contorno deja solo el borde de la figura`() {
        val contorno = cuadrado.mask(filled = false)
        var pintadas = 0
        for (fila in contorno) for (celda in fila) if (celda != Color.Transparent.toArgb()) pintadas++

        // Un cuadrado de 3x3 tiene 8 celdas de borde y 1 de centro
        assertEquals(8, pintadas)
        assertTrue("el borde se pinta", cuadrado.isOn(1, 1, filled = false))
        assertFalse("el centro queda vacío", cuadrado.isOn(2, 2, filled = false))
        assertTrue("relleno sí pinta el centro", cuadrado.isOn(2, 2, filled = true))
    }

    @Test
    fun `estampar usa el color elegido y respeta el estilo`() {
        val lienzo = PixelCanvas.empty(9, 9)
        val rojo = Color.Red.toArgb()

        val relleno = cuadrado.applyTo(lienzo, 4, 4, rojo, StampStyle.FILLED)
        val contorno = cuadrado.applyTo(lienzo, 4, 4, rojo, StampStyle.OUTLINE)

        assertEquals(9, relleno.size)
        assertEquals(8, contorno.size)
        assertTrue(relleno.values.all { it == rojo })
        assertTrue(contorno.values.all { it == rojo })
    }

    @Test
    fun `el estilo original conserva los colores de la figura`() {
        val amarillo = Color.Yellow.toArgb()
        val conColor = cuadrado.copy(
            pattern = cuadrado.pattern.map { fila -> fila.map { if (it != 0) amarillo else 0 } },
            hasOwnColors = true
        )

        val pintado = conColor.applyTo(PixelCanvas.empty(9, 9), 4, 4, Color.Blue.toArgb(), StampStyle.ORIGINAL)

        assertTrue("con estilo original se usa el color de la figura", pintado.values.all { it == amarillo })
        // y con relleno normal manda el color activo
        val normal = conColor.applyTo(PixelCanvas.empty(9, 9), 4, 4, Color.Blue.toArgb(), StampStyle.FILLED)
        assertTrue(normal.values.all { it == Color.Blue.toArgb() })
    }

    @Test
    fun `el catalogo no tiene figuras repetidas`() {
        val huellas = Stamp.allStamps.map { stamp -> stamp.pattern.map { it.joinToString("") }.joinToString("|") }
        assertEquals(huellas.size, huellas.toSet().size)
        assertTrue("hay un catálogo amplio", Stamp.allStamps.size >= 50)
    }

    /**
     * Los nombres se usan como clave en la galería: si se repiten, Compose lanza
     * "Key ... was already used" y la app se cierra. Esta prueba habría cazado
     * ese crash.
     */
    @Test
    fun `los nombres de las figuras son unicos`() {
        val nombres = Stamp.allStamps.map { it.name }
        val repetidos = nombres.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
        assertTrue("nombres repetidos en el catálogo: $repetidos", repetidos.isEmpty())
    }

    @Test
    fun `todas las figuras tienen una mascara valida`() {
        Stamp.allStamps.forEach { stamp ->
            assertTrue("${stamp.name} sin filas", stamp.pattern.isNotEmpty())
            val ancho = stamp.width
            assertTrue("${stamp.name} demasiado angosta", ancho > 1)
            assertTrue("${stamp.name} demasiado baja", stamp.height > 1)
            stamp.pattern.forEachIndexed { fila, celdas ->
                assertEquals("${stamp.name} fila $fila de ancho distinto", ancho, celdas.size)
            }
            // el contorno debe poder calcularse siempre
            assertEquals(stamp.height, stamp.outline.size)
        }
    }

    /**
     * Recorre TODO el catálogo en los tres estilos: es la prueba que detecta
     * crashes ocultos al cambiar entre Relleno / Contorno / Original.
     */
    @Test
    fun `estampar el catalogo completo en los tres estilos no falla`() {
        val lienzo = PixelCanvas.empty(32, 32)
        val color = Color.Magenta.toArgb()
        Stamp.allStamps.forEach { stamp ->
            StampStyle.entries.forEach { estilo ->
                val cambios = stamp.applyTo(lienzo, 16, 16, color, estilo)
                cambios.forEach { (pos, valor) ->
                    assertTrue("${stamp.name} $estilo pintó fuera del lienzo", pos.first in 0 until 32 && pos.second in 0 until 32)
                    assertTrue("${stamp.name} $estilo pintó un color inválido", valor != 0)
                }
                if (estilo == StampStyle.ORIGINAL && stamp.hasOwnColors) {
                    assertTrue("${stamp.name} en original no usó sus colores", cambios.isNotEmpty())
                }
            }
        }
    }
}
