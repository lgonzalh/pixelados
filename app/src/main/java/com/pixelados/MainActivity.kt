package com.pixelados

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.pixelados.data.repository.ProjectRepository
import com.pixelados.data.repository.SavedColorsRepository
import com.pixelados.data.repository.ThemePreferences
import com.pixelados.ui.navigation.PixeladosNavHost
import com.pixelados.ui.theme.AppTheme
import com.pixelados.ui.theme.PixeladosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Antes de super.onCreate(): muestra el logo mientras arranca el proceso
        // y luego deja el tema normal (ver Theme.Pixelados.Starting).
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val repository = ProjectRepository(applicationContext)
        val savedColorsRepo = SavedColorsRepository(applicationContext)
        val themePreferences = ThemePreferences(applicationContext)

        setContent {
            PixeladosApp(
                activity = this@MainActivity,
                repository = repository,
                savedColorsRepo = savedColorsRepo,
                themePreferences = themePreferences
            )
        }
    }
}

@Composable
private fun PixeladosApp(
    activity: ComponentActivity,
    repository: ProjectRepository,
    savedColorsRepo: SavedColorsRepository,
    themePreferences: ThemePreferences
) {
    var paletteId by remember { mutableStateOf(themePreferences.paletteId) }
    var darkPreference by remember { mutableStateOf(themePreferences.darkMode) }
    val systemDark = isSystemInDarkTheme()
    val darkTheme = darkPreference ?: systemDark

    // Barras del sistema acordes al tema elegido (edge-to-edge, transparentes).
    LaunchedEffect(darkTheme) {
        activity.enableEdgeToEdge(
            statusBarStyle = if (darkTheme) {
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                )
            },
            navigationBarStyle = if (darkTheme) {
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                )
            }
        )
    }

    PixeladosTheme(paletteId = paletteId, darkTheme = darkTheme) {
        Surface(color = AppTheme.colors.background) {
            PixeladosNavHost(
                repository = repository,
                savedColorsRepo = savedColorsRepo,
                paletteId = paletteId,
                darkTheme = darkTheme,
                onPaletteSelected = { id ->
                    paletteId = id
                    themePreferences.paletteId = id
                },
                onDarkThemeChange = { value ->
                    darkPreference = value
                    themePreferences.darkMode = value
                }
            )
        }
    }
}
