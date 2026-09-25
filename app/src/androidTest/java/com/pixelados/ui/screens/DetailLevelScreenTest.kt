package com.pixelados.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DetailLevelScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun buttonIsAlwaysAccessible() {
        // Obtenemos el contexto para el ViewModel
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeTestRule.setContent {
            val navController = rememberNavController()
            // Pasamos un photoUri falso que no lanzará error grave por el bloque try/catch en el ViewModel
            DetailLevelScreen(
                navController = navController,
                photoUri = "content://fake/uri",
                context = context
            )
        }

        // Verificamos que el botón de confirmación esté presente y sea accesible.
        // Al estar en el bottomBar del Scaffold, no necesita ser escroleado para ser encontrado y mostrado.
        composeTestRule.onNodeWithText("Convertir y editar")
            .assertExists("El botón de convertir debe existir en la jerarquía de UI")
            .assertIsDisplayed()
            
        // Verificamos que se puede hacer scroll para ver la última opción (128x128)
        // El nodo con texto "Máximo detalle" está en el fondo de la lista de tamaños
        composeTestRule.onNodeWithText("Máximo detalle")
            .performScrollTo()
            .assertIsDisplayed()
            
        // Después del scroll, el botón debe seguir visible en la parte inferior
        composeTestRule.onNodeWithText("Convertir y editar")
            .assertIsDisplayed()
    }
}
