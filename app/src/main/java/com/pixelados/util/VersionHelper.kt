package com.pixelados.util

import com.pixelados.BuildConfig

/**
 * Versionado secuencial con nomenclatura de letras griegas.
 * Cada generación del APK incrementa versionCode (app/version.properties),
 * y la etiqueta avanza: alfa → beta → gamma → delta → ...
 * Tras "omega" (24), continúa como "omega-2", "omega-3", ... sin límite.
 */
object VersionHelper {
    private val greekLetters = listOf(
        "alfa", "beta", "gamma", "delta", "epsilon", "zeta", "eta", "theta",
        "iota", "kappa", "lambda", "mu", "nu", "xi", "omicron", "pi", "rho",
        "sigma", "tau", "upsilon", "phi", "chi", "psi", "omega"
    )

    /** Etiqueta de build según versionCode: 1=alfa, 2=beta, 3=gamma, ... */
    fun buildLabel(): String {
        val buildIndex = (BuildConfig.VERSION_CODE - 1).coerceAtLeast(0)
        val cycle = buildIndex / greekLetters.size
        val letter = greekLetters[buildIndex % greekLetters.size]
        return if (cycle == 0) letter else "$letter-${cycle + 1}"
    }

    /** Cadena de versión mostrada en la app, ej: "Ver 1.0 build alfa". */
    fun getGreekVersionString(): String =
        "Ver ${BuildConfig.VERSION_NAME} build ${buildLabel()}"
}
