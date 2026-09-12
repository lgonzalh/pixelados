package com.pixelados.data.util

import android.graphics.Bitmap
import android.graphics.Color
import com.pixelados.data.model.ColorPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utilidades para conversión de foto a pixel art.
 * Algoritmo: downsampling por promedio de área + cuantización a paleta 256 colores.
 */
object PhotoPixelator {

    /** Tamaños de cuadrícula predefinidos para el slider de detalle */
    val detailLevels = listOf(16, 24, 32, 48, 64, 80, 100, 128)

    /**
     * Convierte un Bitmap a PixelArt.
     * @param bitmap Imagen original
     * @param targetSize Tamaño de la cuadrícula destino (ej. 32 = 32×32)
     * @return Bitmap pixelado (targetSize × targetSize) con colores de la paleta
     */
    suspend fun pixelate(bitmap: Bitmap, targetSize: Int): Bitmap = withContext(Dispatchers.Default) {
        val srcWidth = bitmap.width
        val srcHeight = bitmap.height

        // Calcular región cuadrada centrada (mantener aspecto recortando)
        val minDim = minOf(srcWidth, srcHeight)
        val startX = (srcWidth - minDim) / 2
        val startY = (srcHeight - minDim) / 2

        // Extraer píxeles de la región cuadrada
        val srcPixels = IntArray(minDim * minDim)
        bitmap.getPixels(srcPixels, 0, minDim, startX, startY, minDim, minDim)

        // Downsampling por promedio de área (area averaging)
        val blockSize = minDim / targetSize
        val dstPixels = IntArray(targetSize * targetSize)

        for (dstY in 0 until targetSize) {
            for (dstX in 0 until targetSize) {
                val srcX = dstX * blockSize
                val srcY = dstY * blockSize
                var rSum = 0L
                var gSum = 0L
                var bSum = 0L
                var aSum = 0L
                var count = 0

                // Promedio de todos los píxeles en el bloque
                for (by in 0 until blockSize) {
                    for (bx in 0 until blockSize) {
                        val sx = srcX + bx
                        val sy = srcY + by
                        if (sx < minDim && sy < minDim) {
                            val pixel = srcPixels[sy * minDim + sx]
                            val a = Color.alpha(pixel)
                            if (a > 0) {
                                rSum += Color.red(pixel)
                                gSum += Color.green(pixel)
                                bSum += Color.blue(pixel)
                                aSum += a
                                count++
                            }
                        }
                    }
                }

                val avgColor: Int
                if (count > 0) {
                    val avgR = (rSum / count).toInt()
                    val avgG = (gSum / count).toInt()
                    val avgB = (bSum / count).toInt()
                    val avgA = (aSum / count).toInt()
                    avgColor = Color.argb(avgA, avgR, avgG, avgB)
                } else {
                    avgColor = Color.TRANSPARENT
                }

                // Cuantizar al color más cercano de la paleta
                dstPixels[dstY * targetSize + dstX] = if (avgColor != Color.TRANSPARENT) {
                    ColorPalette.nearestPaletteColor(avgColor)
                } else {
                    Color.TRANSPARENT
                }
            }
        }

        val result = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        result.setPixels(dstPixels, 0, targetSize, 0, 0, targetSize, targetSize)
        result
    }

    /**
     * Calcula el nivel de detalle recomendado según tamaño y proporción de la imagen.
     * Devuelve índice en detailLevels.
     */
    fun recommendedDetailLevel(bitmap: Bitmap): Int {
        val maxDim = maxOf(bitmap.width, bitmap.height)
        return when {
            maxDim >= 2000 -> detailLevels.indexOf(100)
            maxDim >= 1500 -> detailLevels.indexOf(80)
            maxDim >= 1000 -> detailLevels.indexOf(64)
            maxDim >= 600 -> detailLevels.indexOf(48)
            else -> detailLevels.indexOf(32)
        }
    }

    /**
     * Crea bitmap de referencia escalado para superposición semitransparente.
     * Mantiene aspecto original, cabe en targetSize × targetSize.
     */
    fun createReferenceBitmap(bitmap: Bitmap, targetSize: Int): Bitmap {
        val srcWidth = bitmap.width
        val srcHeight = bitmap.height
        val scale = targetSize.toFloat() / maxOf(srcWidth, srcHeight)
        val dstWidth = (srcWidth * scale).toInt()
        val dstHeight = (srcHeight * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, dstWidth, dstHeight, true)
    }
}