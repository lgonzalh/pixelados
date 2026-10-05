package com.pixelados.ui.screens

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pixelados.data.model.PixelCanvas
import com.pixelados.data.model.Project
import com.pixelados.data.repository.ProjectRepository
import com.pixelados.data.repository.SavedColorsRepository
import com.pixelados.ui.theme.PixeladosTheme
import com.pixelados.ui.viewmodel.EditorViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regresión del bug encontrado en el moto g23: al abrir un dibujo guardado, un
 * solo deshacer borraba TODO el lienzo (el historial incluía el lienzo vacío con
 * el que se crea el editor). El historial debe empezar en el dibujo cargado.
 */
@RunWith(AndroidJUnit4::class)
class SavedProjectTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val transparent = Color.Transparent.toArgb()

    @Test
    fun abrirUnLienzoGuardadoYDeshacerNoBorraElDibujo() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ProjectRepository(context)

        // Un dibujo de 16x16 con cinco celdas pintadas, guardado como proyecto
        var canvas = PixelCanvas.empty(16, 16, name = "Prueba deshacer")
        for (i in 0 until 5) canvas = canvas.setPixel(i, i, Color.Red.toArgb())
        val proyecto = Project(canvas = canvas)
        runBlocking { repository.saveProject(proyecto) }

        lateinit var vm: EditorViewModel
        composeRule.setContent {
            val ctx = LocalContext.current
            val navController = rememberNavController()
            vm = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = EditorViewModel.Factory(
                    repository = ProjectRepository(ctx),
                    savedColorsRepo = SavedColorsRepository(ctx),
                    width = 16,
                    height = 16,
                    projectId = proyecto.id
                )
            )
            PixeladosTheme { EditorScreen(viewModel = vm, navController = navController) }
        }

        // esperar a que cargue el proyecto desde disco
        composeRule.waitUntil(timeoutMillis = 8_000) {
            vm.canvas.value.pixels.count { it != transparent } == 5
        }

        composeRule.onNodeWithContentDescription("Deshacer").performClick()
        composeRule.waitForIdle()

        assertEquals(
            "deshacer al abrir un dibujo guardado no debe borrarlo",
            5,
            vm.canvas.value.pixels.count { it != transparent }
        )
        composeRule.onNodeWithTag("canvas").assertExists()
    }
}
