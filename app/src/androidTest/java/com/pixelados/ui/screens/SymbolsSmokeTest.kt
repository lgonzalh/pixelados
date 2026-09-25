package com.pixelados.ui.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pixelados.data.repository.ProjectRepository
import com.pixelados.data.repository.SavedColorsRepository
import com.pixelados.ui.theme.PixeladosTheme
import com.pixelados.ui.viewmodel.EditorViewModel
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prueba de humo de la galería de símbolos: recorre los tres estilos y todas las
 * categorías, tocando figuras. Cualquier excepción oculta (claves repetidas en
 * la galería, índices fuera de rango, divisiones por cero…) hace fallar la prueba.
 */
@RunWith(AndroidJUnit4::class)
class SymbolsSmokeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val estilos = listOf("Contorno", "Original", "Relleno")

    private fun abrirGaleria(): EditorViewModel {
        lateinit var vm: EditorViewModel
        composeRule.setContent {
            val context = LocalContext.current
            val navController = rememberNavController()
            vm = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = EditorViewModel.Factory(
                    repository = ProjectRepository(context),
                    savedColorsRepo = SavedColorsRepository(context),
                    width = 32,
                    height = 32
                )
            )
            PixeladosTheme { EditorScreen(viewModel = vm, navController = navController) }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("tool_STAMPS").performClick()
        composeRule.waitForIdle()
        return vm
    }

    private fun existe(texto: String): Boolean =
        composeRule.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty()

    private fun tocar(texto: String) {
        if (existe(texto)) {
            composeRule.onAllNodesWithText(texto)[0].performClick()
            composeRule.waitForIdle()
        }
    }

    /** La galería se cierra al elegir una figura: se vuelve a abrir si hace falta. */
    private fun asegurarGaleriaAbierta() {
        val abierta = composeRule.onAllNodesWithTag("stamp_grid").fetchSemanticsNodes().isNotEmpty()
        if (!abierta) {
            composeRule.onNodeWithTag("tool_STAMPS").performClick()
            composeRule.waitForIdle()
        }
    }

    @Test
    fun recorrerEstilosYCategoriasNoFalla() {
        abrirGaleria()

        // 1) Los tres estilos seguidos (aquí se reportó el crash)
        estilos.forEach { tocar(it) }

        // 2) Todas las categorías, alternando estilos dentro de cada una
        listOf("Formas", "Naturaleza", "Animales", "Objetos", "Fantasía", "Pokémon").forEach { categoria ->
            asegurarGaleriaAbierta()
            if (existe(categoria)) {
                // desplazar la fila de categorías hasta el chip (sin gestos que la
                // hoja pueda interpretar como "cerrar")
                runCatching { composeRule.onAllNodesWithText(categoria)[0].performScrollTo() }
                composeRule.waitForIdle()
                tocar(categoria)
                estilos.forEach { tocar(it) }

                // tocar una figura de la galería (en el centro de la rejilla)
                composeRule.onNodeWithTag("stamp_grid").performTouchInput { click(center) }
                composeRule.waitForIdle()
            }
        }
    }

    @Test
    fun elSelloSeVeCentradoYSeColocaAlSoltar() {
        val vm = abrirGaleria()

        // sin tocar nada, el fantasma está centrado; al tocar el centro se coloca
        composeRule.onNodeWithTag("canvas").performTouchInput { click(center) }
        composeRule.waitForIdle()

        val lienzo = vm.canvas.value
        val pintadas = lienzo.pixels.count { it != 0 }
        assertTrue("el sello debe pintar celdas al soltar el dedo", pintadas > 0)
        assertTrue("el sello debe quedar en el centro", lienzo.getPixel(lienzo.height / 2, lienzo.width / 2) != 0)
    }
}
