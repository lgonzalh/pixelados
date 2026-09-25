package com.pixelados.data.model

/**
 * Tamaños de lienzo predefinidos para que el niño elija fácilmente.
 */
enum class CanvasSize(val width: Int, val height: Int, val label: String, val description: String) {
    SMALL(16, 16, "Ícono pequeño", "16×16"),
    MEDIUM(32, 32, "Un personaje", "32×32"),
    LARGE(64, 64, "Un dibujo con detalles", "64×64"),
    XLARGE(100, 100, "Una escena grande", "100×100"),
    CUSTOM(0, 0, "Personalizado", "Elige tu tamaño")
}
