package com.pixelados.ui.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixelados.data.repository.ProjectRepository
import com.pixelados.data.repository.SavedColorsRepository
import com.pixelados.ui.screens.AboutScreen
import com.pixelados.ui.screens.DetailLevelScreen
import com.pixelados.ui.screens.EditorScreen
import com.pixelados.ui.screens.HomeScreen
import com.pixelados.ui.screens.SizeSelectorScreen
import com.pixelados.ui.screens.SettingsScreen
import android.net.Uri
import com.pixelados.ui.viewmodel.EditorViewModel
import com.pixelados.ui.viewmodel.HomeViewModel

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Settings : Screen("settings")
    object SizeSelector : Screen("sizeSelector")
    object About : Screen("about")
    object DetailLevel : Screen("detailLevel/{photoUri}") {
        fun createRoute(photoUri: String): String {
            return "detailLevel/${Uri.encode(photoUri)}"
        }
    }
    object Editor : Screen("editor/{width}/{height}/{fromPhoto}?photoUri={photoUri}&projectId={projectId}") {
        fun createRoute(
            width: Int,
            height: Int,
            fromPhoto: Boolean = false,
            photoUri: String? = null,
            projectId: String? = null
        ): String {
            val base = "editor/$width/$height/$fromPhoto"
            // Los parámetros opcionales SIEMPRE van tras "?" y separados por "&".
            // Antes se concatenaba "&projectId=..." sin "?" cuando no había foto,
            // y Navigation no encontraba la ruta → la app se cerraba al abrir un
            // lienzo guardado.
            val query = buildList {
                if (!photoUri.isNullOrBlank()) add("photoUri=${Uri.encode(photoUri)}")
                if (!projectId.isNullOrBlank()) add("projectId=${Uri.encode(projectId)}")
            }
            return if (query.isEmpty()) base else "$base?" + query.joinToString("&")
        }
    }
}

@Composable
fun PixeladosNavHost(
    navController: NavHostController = rememberNavController(),
    repository: ProjectRepository,
    savedColorsRepo: SavedColorsRepository,
    paletteId: String,
    darkTheme: Boolean,
    onPaletteSelected: (String) -> Unit,
    onDarkThemeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    NavHost(navController = navController, startDestination = Screen.Home.route, modifier = modifier) {
        composable(Screen.Home.route) {
            HomeScreen(
                navController = navController,
                repository = repository,
                savedColorsRepo = savedColorsRepo
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                navController = navController,
                paletteId = paletteId,
                darkTheme = darkTheme,
                onPaletteSelected = onPaletteSelected,
                onDarkThemeChange = onDarkThemeChange
            )
        }
        composable(Screen.SizeSelector.route) {
            SizeSelectorScreen(navController = navController)
        }
        composable(Screen.About.route) {
            AboutScreen(navController = navController)
        }
        composable(
            route = Screen.DetailLevel.route,
            arguments = listOf(navArgument("photoUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val photoUri = backStackEntry.arguments?.getString("photoUri").orEmpty().let { Uri.decode(it) }
            DetailLevelScreen(
                navController = navController,
                photoUri = photoUri,
                context = context
            )
        }
        composable(
            route = Screen.Editor.route,
            arguments = listOf(
                navArgument("width") { type = NavType.IntType },
                navArgument("height") { type = NavType.IntType },
                navArgument("fromPhoto") { type = NavType.BoolType },
                navArgument("photoUri") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("projectId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val width = backStackEntry.arguments?.getInt("width") ?: 32
            val height = backStackEntry.arguments?.getInt("height") ?: 32
            val fromPhoto = backStackEntry.arguments?.getBoolean("fromPhoto") ?: false
            val photoUri = backStackEntry.arguments?.getString("photoUri")?.let { Uri.decode(it) }
            val uriString = photoUri?.takeIf { it.isNotEmpty() }
            val projectId = backStackEntry.arguments?.getString("projectId")?.let { Uri.decode(it) }
                ?.takeIf { it.isNotEmpty() }

            val factory = EditorViewModel.Factory(
                repository = repository,
                savedColorsRepo = savedColorsRepo,
                width = width,
                height = height,
                photoUri = uriString,
                projectId = projectId,
                context = context.applicationContext
            )
            val editorViewModel: EditorViewModel = viewModel(factory = factory)

            EditorScreen(
                viewModel = editorViewModel,
                navController = navController,
                width = width,
                height = height,
                fromPhoto = fromPhoto
            )
        }
    }
}
