package com.pixelados.data.model

import org.junit.Assert.*
import org.junit.Test

class ColorPaletteTest {

    @Test
    fun testColorPaletteInitialization() {
        // Al acceder a allColors se debe inicializar sin lanzar NullPointerException
        val colors = ColorPalette.allColors
        
        // Verificamos que contenga colores
        assertNotNull("allColors no debe ser nulo", colors)
        assertTrue("allColors debe contener elementos", colors.isNotEmpty())
        
        // Verificamos que las familias se hayan inicializado correctamente
        val families = ColorPalette.colorFamilies
        assertNotNull("colorFamilies no debe ser nulo", families)
        assertTrue("colorFamilies debe tener exactamente 9 familias", families.size == 9)
        
        // Verificamos que todos los colores estén en allColors
        val totalExpectedColors = families.sumOf { it.colors.size }
        assertEquals("allColors debe tener la suma de todos los colores de las familias", totalExpectedColors, colors.size)
    }

    @Test
    fun testNearestPaletteColor() {
        // Color negro
        val nearestToBlack = ColorPalette.nearestPaletteColor(0xFF000000.toInt())
        assertEquals("El color más cercano al negro debe ser negro", 0xFF000000.toInt(), nearestToBlack)
        
        // Un color verde puro
        val nearestToGreen = ColorPalette.nearestPaletteColor(0xFF00FF00.toInt())
        assertEquals("El color más cercano al verde debe ser verde", 0xFF00FF00.toInt(), nearestToGreen)
    }
}
