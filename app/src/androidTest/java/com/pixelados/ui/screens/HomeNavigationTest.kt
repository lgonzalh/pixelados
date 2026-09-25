package com.pixelados.ui.screens

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pixelados.data.model.PixelCanvas
import com.pixelados.data.model.Project
import com.pixelados.data.repository.ProjectRepository
import com.pixelados.data.repository.SavedColorsRepository
import com.pixelados.ui.navigation.PixeladosNavHost
import com.pixelados.ui.theme.PixeladosTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regresión del crash al abrir un lienzo guardado: la ruta de navegación se
 * construía sin "?" y Navigation cerraba la app.
 */
@RunWith(AndroidJUnit4::class)
class HomeNavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun abrirUnLienzoGuardadoNoCierraLaApp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ProjectRepository(context)
        val nombre = "Prueba navegación"
        val canvas = PixelCanvas.empty(8, 8, name = nombre)
            .setPixel(0, 0, Color.Red.toArgb())
        runBlocking { repository.saveProject(Project(canvas = canvas)) }

        composeRule.setContent {
            PixeladosTheme {
                val navController = rememberNavController()
                PixeladosNavHost(
                    navController = navController,
                    repository = repository,
                    savedColorsRepo = SavedColorsRepository(context),
                    paletteId = "lavanda",
                    darkTheme = false,
                    onPaletteSelected = {},
                    onDarkThemeChange = {}
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 8_000) {
            composeRule.onAllNodesWithText(nombre).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText(nombre)[0].performClick()
        composeRule.waitForIdle()

        // Si la ruta estuviera mal, la app se habría cerrado y el lienzo no existiría.
        composeRule.onNodeWithTag("canvas").assertExists()
    }
}
