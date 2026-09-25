package com.pixelados.data.model

import kotlinx.serialization.Serializable

/**
 * Un proyecto guardado por el usuario (lienzo + metadatos para la galería).
 */
@Serializable
data class Project(
    val id: String = java.util.UUID.randomUUID().toString(),
    val canvas: PixelCanvas,
    val thumbnailBase64: String = "" // Miniatura en base64 para mostrar rápido
) {
    val createdAt: Long = canvas.createdAt
    val updatedAt: Long = canvas.updatedAt
    val name: String get() = canvas.name
}
