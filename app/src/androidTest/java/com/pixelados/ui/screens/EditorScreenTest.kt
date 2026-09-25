package com.pixelados.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pixelados.data.model.Tool
import com.pixelados.data.repository.ProjectRepository
import com.pixelados.data.repository.SavedColorsRepository
import com.pixelados.ui.theme.PixeladosTheme
import com.pixelados.ui.viewmodel.EditorViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pruebas de las funcionalidades REALES del editor (se ejecutan en dispositivo).
 * Verifican las promesas que el usuario ve en pantalla.
 */
@RunWith(AndroidJUnit4::class)
class EditorScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val transparent = Color.Transparent.toArgb()

    private fun launchEditor(
        size: Int = 16,
        onReady: (EditorViewModel) -> Unit = {}
    ) {
        lateinit var viewModel: EditorViewModel
        composeRule.setContent {
            val context = LocalContext.current
            val navController = rememberNavController()
            viewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = EditorViewModel.Factory(
                    repository = ProjectRepository(context),
                    savedColorsRepo = SavedColorsRepository(context),
                    width = size,
                    height = size
                )
            )
            PixeladosTheme {
                EditorScreen(viewModel = viewModel, navController = navController)
            }
        }
        composeRule.waitForIdle()
        onReady(viewModel)
    }

    @Test
    fun alEntrarLaPaletaNoTapaElLienzo() {
        launchEditor()

        composeRule.onNodeWithTag("color_palette").assertDoesNotExist()
        composeRule.onNodeWithTag("canvas").assertExists()
    }

    @Test
    fun tocarElLienzoPintaExactamenteUnPixel() {
        lateinit var vm: EditorViewModel
        launchEditor(size = 16) { vm = it }

        composeRule.onNodeWithTag("canvas").performTouchInput { click(center) }
        composeRule.waitForIdle()

        val pintados = vm.canvas.value.pixels.count { it != transparent }
        assertEquals("un toque debe pintar 1 y solo 1 celda", 1, pintados)
    }

    @Test
    fun laHerramientaRellenarPintaTodoElLienzo() {
        lateinit var vm: EditorViewModel
        launchEditor(size = 8) { vm = it }

        composeRule.onNodeWithTag("tool_FILL").performClick()
        composeRule.onNodeWithTag("canvas").performTouchInput { click(center) }
        composeRule.waitForIdle()

        assertEquals(Tool.FILL, vm.currentTool)
        assertTrue(
            "rellenar un lienzo vacío debe dejar las 64 celdas pintadas",
            vm.canvas.value.pixels.all { it == vm.activeColor }
        )
    }

    @Test
    fun deshacerDejaElLienzoComoEstaba() {
        lateinit var vm: EditorViewModel
        launchEditor(size = 8) { vm = it }

        composeRule.onNodeWithTag("canvas").performTouchInput { click(center) }
        composeRule.waitForIdle()
        assertEquals(1, vm.canvas.value.pixels.count { it != transparent })

        composeRule.onNodeWithContentDescription("Deshacer").performClick()
        composeRule.waitForIdle()

        assertEquals(0, vm.canvas.value.pixels.count { it != transparent })
    }

    @Test
    fun lasFilasDeOpcionesNoSeMuevenAlCambiarDeHerramienta() {
        launchEditor()

        val herramientasAntes = composeRule.onNodeWithTag("tool_row")
            .fetchSemanticsNode().boundsInRoot
        val franjaAntes = composeRule.onNodeWithTag("options_strip")
            .fetchSemanticsNode().boundsInRoot

        // Símbolos muestra la lista de sellos: antes esto empujaba toda la fila
        composeRule.onNodeWithTag("tool_STAMPS").performClick()
        composeRule.waitForIdle()

        val herramientasDespues = composeRule.onNodeWithTag("tool_row")
            .fetchSemanticsNode().boundsInRoot
        val franjaDespues = composeRule.onNodeWithTag("options_strip")
            .fetchSemanticsNode().boundsInRoot

        assertEquals(herramientasAntes, herramientasDespues)
        assertEquals(franjaAntes, franjaDespues)
    }

    @Test
    fun dosDedosHacenZoomSinBloquearElDibujo() {
        launchEditor(size = 16)
        val node = composeRule.onNodeWithTag("canvas").fetchSemanticsNode()
        val centroX = node.size.width / 2f
        val centroY = node.size.height / 2f

        composeRule.onNodeWithTag("canvas").performTouchInput {
            pinch(
                start0 = Offset(centroX - 40f, centroY),
                end0 = Offset(centroX - 300f, centroY),
                start1 = Offset(centroX + 40f, centroY),
                end1 = Offset(centroX + 300f, centroY),
                durationMillis = 400
            )
        }
        composeRule.waitForIdle()

        // El botón de ajuste muestra el zoom; con 1× significaría que no amplió.
        composeRule.onNodeWithContentDescription("Ajustar a la pantalla").assertExists()
        composeRule.onNodeWithText("1×").assertDoesNotExist()
    }

    @Test
    fun unTrazoLargoSeDeshaceConUnSoloToque() {
        lateinit var vm: EditorViewModel
        launchEditor(size = 16) { vm = it }

        val node = composeRule.onNodeWithTag("canvas").fetchSemanticsNode()
        val ancho = node.size.width.toFloat()
        val alto = node.size.height.toFloat()
        composeRule.onNodeWithTag("canvas").performTouchInput {
            swipe(
                start = Offset(ancho * 0.25f, alto * 0.25f),
                end = Offset(ancho * 0.75f, alto * 0.75f),
                durationMillis = 300
            )
        }
        composeRule.waitForIdle()

        val pintados = vm.canvas.value.pixels.count { it != transparent }
        assertTrue("el trazo debe pintar varias celdas seguidas (pintó $pintados)", pintados > 3)
        assertTrue("después de un trazo debe poder deshacerse", vm.canUndo)

        composeRule.onNodeWithContentDescription("Deshacer").performClick()
        composeRule.waitForIdle()

        assertEquals(
            "un solo deshacer revierte el trazo completo",
            0,
            vm.canvas.value.pixels.count { it != transparent }
        )
    }

    @Test
    fun rellenarYDeshacerVuelveAlDibujoAnterior() {
        lateinit var vm: EditorViewModel
        launchEditor(size = 8) { vm = it }

        // 1) Un trazo
        composeRule.onNodeWithTag("canvas").performTouchInput { click(centerLeft) }
        composeRule.waitForIdle()
        val trasElTrazo = vm.canvas.value.pixels.count { it != transparent }
        assertEquals(1, trasElTrazo)

        // 2) Rellenar desde una celda vacía (el centro del lienzo).
        //    Fuera del lienzo no se pinta nada, así que se usa el centro.
        composeRule.onNodeWithTag("tool_FILL").performClick()
        composeRule.onNodeWithTag("canvas").performTouchInput { click(center) }
        composeRule.waitForIdle()
        // 1 celda pintada con el pincel + 63 transparentes rellenadas
        assertEquals(64, vm.canvas.value.pixels.count { it != transparent })

        // 3) Deshacer debe dejar SOLO el trazo original, no el lienzo vacío
        composeRule.onNodeWithContentDescription("Deshacer").performClick()
        composeRule.waitForIdle()

        assertEquals(
            "deshacer el relleno debe volver al trazo, no borrar todo",
            1,
            vm.canvas.value.pixels.count { it != transparent }
        )
    }

    @Test
    fun alSalirDeLaPantallaElDibujoSeGuarda() {
        lateinit var vm: EditorViewModel
        launchEditor(size = 8) { vm = it }

        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val carpetaProyectos = java.io.File(context.filesDir, "projects")
        val antes = carpetaProyectos.listFiles()?.count { it.extension == "json" } ?: 0

        composeRule.onNodeWithTag("canvas").performTouchInput { click(center) }
        composeRule.waitForIdle()

        // Equivale a salir de la pantalla (DisposableEffect / onCleared)
        vm.saveOnExit()
        composeRule.waitForIdle()

        val despues = carpetaProyectos.listFiles()?.count { it.extension == "json" } ?: 0
        assertTrue(
            "al salir debe quedar un archivo de proyecto guardado (antes=$antes, después=$despues)",
            despues > antes
        )
    }

    @Test
    fun elSelloSeColocaAlSoltarYNoSeArrastra() {
        lateinit var vm: EditorViewModel
        launchEditor(size = 32) { vm = it }

        // Un sello chico para poder comparar cuántas celdas pinta
        val sello = com.pixelados.data.model.Stamp.allStamps.first { it.name == "Círculo" }
        vm.setStamp(sello)
        composeRule.waitForIdle()
        val celdasDelSello = sello.mask(true).sumOf { fila -> fila.count { it != 0 } }

        // Arrastrar de una esquina a la otra: debe quedar UN solo sello, en la
        // posición final (antes se iba "arrastrando" y dejaba varios).
        val node = composeRule.onNodeWithTag("canvas").fetchSemanticsNode()
        val ancho = node.size.width.toFloat()
        val alto = node.size.height.toFloat()
        composeRule.onNodeWithTag("canvas").performTouchInput {
            swipe(
                start = Offset(ancho * 0.3f, alto * 0.3f),
                end = Offset(ancho * 0.7f, alto * 0.7f),
                durationMillis = 400
            )
        }
        composeRule.waitForIdle()

        assertEquals(
            "un arrastre debe dejar un solo sello (celdas pintadas)",
            celdasDelSello,
            vm.canvas.value.pixels.count { it != transparent }
        )
    }
}
