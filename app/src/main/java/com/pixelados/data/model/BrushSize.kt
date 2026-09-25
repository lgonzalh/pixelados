package com.pixelados.data.model

/**
 * Tamaño de pincel del editor.
 *
 * REGLA DEL PRODUCTO: el pincel es EXACTAMENTE del tamaño de un píxel del
 * lienzo (1×1). No admite desviaciones ni tamaños mayores/menores, y no hay
 * suavizado (anti-aliasing): cada trazo coloca un píxel exacto que ocupa un
 * único píxel de la cuadrícula, preservando la estética de pixel art.
 */
enum class BrushSize(val size: Int, val label: String) {
    PIXEL_EXACT(1, "1×1");

    companion object {
        /** Único tamaño permitido: pincel exacto de 1 píxel. */
        val fixed: BrushSize = PIXEL_EXACT
    }
}
