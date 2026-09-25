package com.pixelados.data.model

/**
 * Herramientas de dibujo del editor.
 *
 * Solo están aquí las acciones que se aplican al LIENZO (con el dedo).
 * Cuadrícula, espejo y referencia son INTERRUPTORES de vista, no herramientas:
 * mantenerlos fuera evita que "seleccionar una opción" deje el lienzo sin
 * poder pintar.
 */
enum class Tool(val icon: String, val label: String, val shortLabel: String) {
    BRUSH("brush", "Pincel", "Pincel"),
    ERASER("eraser", "Borrador", "Borrador"),
    FILL("format_color_fill", "Rellenar", "Rellenar"),
    EYEDROPPER("colorize", "Cuentagotas", "Gotero"),
    SELECT("select_all", "Seleccionar", "Selección"),
    STAMPS("star", "Símbolos", "Símbolos")
}
