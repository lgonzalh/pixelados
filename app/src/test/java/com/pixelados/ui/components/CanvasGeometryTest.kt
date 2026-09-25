package com.pixelados.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El dedo de un niño se mueve rápido: el trazo debe rellenar las celdas
 * intermedias para no dejar huecos (el clásico "punteado").
 */
class CanvasGeometryTest {

    @Test
    fun `trazo diagonal incluye todas las celdas intermedias`() {
        val celdas = lineBetween(0 to 0, 3 to 3)

        assertEquals(listOf(1 to 1, 2 to 2, 3 to 3), celdas)
    }

    @Test
    fun `trazo horizontal no deja huecos`() {
        val celdas = lineBetween(2 to 0, 2 to 4)

        assertEquals(listOf(2 to 1, 2 to 2, 2 to 3, 2 to 4), celdas)
    }

    @Test
    fun `trazo desde la primera celda solo devuelve el destino`() {
        assertEquals(listOf(5 to 7), lineBetween(null, 5 to 7))
    }

    @Test
    fun `moverse dentro de la misma celda no genera cambios`() {
        assertTrue(lineBetween(1 to 1, 1 to 1).isEmpty())
    }

    @Test
    fun `el zoom se imanta a los pasos legibles`() {
        assertEquals(1f, snapZoom(0.4f))
        assertEquals(1.5f, snapZoom(1.6f))
        assertEquals(2f, snapZoom(2.1f))
        assertEquals(4f, snapZoom(3.9f))
        assertEquals(8f, snapZoom(12f))
    }
}
