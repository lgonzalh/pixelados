package com.pixelados.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.pixelados.R
import com.pixelados.data.model.Project
import com.pixelados.data.repository.ProjectRepository
import com.pixelados.data.repository.SavedColorsRepository
import com.pixelados.ui.navigation.Screen
import com.pixelados.ui.theme.AppTheme
import com.pixelados.ui.viewmodel.HomeViewModel
import com.pixelados.util.VersionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    repository: ProjectRepository,
    savedColorsRepo: SavedColorsRepository
) {
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(repository, savedColorsRepo))
    val projects = homeViewModel.projects.collectAsState(initial = emptyList())
    val loaded = homeViewModel.loaded.collectAsState(initial = false)
    val context = LocalContext.current

    // Al volver del editor (o de cualquier pantalla) se recarga la lista: si no,
    // el dibujo recién guardado no aparecía hasta reiniciar la app.
    LaunchedEffect(Unit) {
        homeViewModel.loadProjects()
    }

    // PhotoPicker launcher
    val pickVisualMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            navController.navigate(Screen.DetailLevel.createRoute(selectedUri.toString()))
        }
    }

    // Configurar launcher en ViewModel
    DisposableEffect(homeViewModel) {
        homeViewModel.setPickVisualMediaLauncher { request ->
            pickVisualMedia.launch(request)
        }
        onDispose {}
    }

    Scaffold(
        topBar = {
            // Encabezado tipo "glass": barra translúcida con el color de la paleta.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .background(AppTheme.colors.bar)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ajustes de apariencia (paleta de colores y modo oscuro)
                IconButton(
                    onClick = { navController.navigate(Screen.Settings.route) },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(AppTheme.colors.surfaceVariant)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Ajustes de apariencia",
                        tint = AppTheme.colors.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                // Botón de información "i" → pantalla "Acerca de.."
                IconButton(
                    onClick = { navController.navigate(Screen.About.route) },
                    modifier = Modifier
                        .background(AppTheme.colors.primary, shape = CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_info),
                        contentDescription = "Acerca de",
                        tint = AppTheme.colors.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                // Deja espacio al fondo para la barra de gestos del sistema
                .windowInsetsPadding(WindowInsets.navigationBars),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icono principal de la app (assets/pixelados.png)
            Image(
                painter = painterResource(R.drawable.pixelados),
                contentDescription = "Pixelados Logo",
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(28.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Versión destacada bajo el logo (e.g. "Ver 1.0 build alfa")
            Text(
                text = VersionHelper.getGreekVersionString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Dos botones grandes de entrada
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Crear desde cero
                MainActionCard(
                    iconRes = R.drawable.ic_home_crear,
                    title = stringResource(id = R.string.create_new),
                    subtitle = "Elige tamaño y empieza a dibujar",
                    color = AppTheme.colors.primary,
                    contentColor = AppTheme.colors.onPrimary,
                    onClick = { navController.navigate(Screen.SizeSelector.route) }
                )

                // Convertir foto
                MainActionCard(
                    iconRes = R.drawable.ic_home_convertir_foto,
                    title = stringResource(id = R.string.convert_photo),
                    subtitle = "De galería a pixel art",
                    color = AppTheme.colors.accent,
                    contentColor = AppTheme.colors.onAccent,
                    onClick = {
                        pickVisualMedia.launch(
                            PickVisualMediaRequest(
                                mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    }
                )
            }

            Spacer(Modifier.height(24.dp))

            // Sección "Mis lienzos"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.my_canvases),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary
                )
                if (projects.value.isNotEmpty()) {
                    Text(
                        text = "${projects.value.size} lienzos",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.textSecondary
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            if (!loaded.value) {
                // Todavía leyendo el disco: no se muestra el estado vacío.
                Spacer(Modifier.height(40.dp))
            } else if (projects.value.isEmpty()) {
                EmptyStateCard()
            } else {
                ProjectGrid(
                    projects = projects.value,
                    onProjectClick = { project ->
                        navController.navigate(
                            Screen.Editor.createRoute(
                                project.canvas.width,
                                project.canvas.height,
                                fromPhoto = false,
                                photoUri = null,
                                projectId = project.id
                            )
                        )
                    },
                    onProjectDelete = { project ->
                        homeViewModel.deleteProject(project)
                    },
                    onProjectRename = { project, newName ->
                        homeViewModel.renameProject(project, newName)
                    }
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun RowScope.MainActionCard(
    iconRes: Int,
    title: String,
    subtitle: String,
    color: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .weight(1f)
            .height(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(56.dp).padding(bottom = 12.dp)
            )
            Text(
                text = title,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 12.5.sp,
                color = contentColor.copy(0.9f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EmptyStateCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ImageSearch,
                    contentDescription = null,
                    tint = AppTheme.colors.textSecondary.copy(0.6f),
                    modifier = Modifier.size(64.dp).padding(bottom = 16.dp)
                )
                Text(
                    text = "No hay lienzos guardados aún",
                    fontSize = 16.sp,
                    color = AppTheme.colors.textSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "¡Crea tu primer dibujo o convierte una foto!",
                    fontSize = 12.5.sp,
                    color = AppTheme.colors.textSecondary.copy(0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ProjectGrid(
    projects: List<Project>,
    onProjectClick: (Project) -> Unit,
    onProjectDelete: (Project) -> Unit,
    onProjectRename: (Project, String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val chunkedProjects = projects.chunked(2)
        chunkedProjects.forEach { rowProjects ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowProjects.forEach { project ->
                    Box(modifier = Modifier.weight(1f)) {
                        ProjectThumbnail(
                            project = project,
                            onClick = { onProjectClick(project) },
                            onDelete = { onProjectDelete(project) },
                            onRename = { newName -> onProjectRename(project, newName) }
                        )
                    }
                }
                // Add an empty box for odd numbers of projects to keep size consistent
                if (rowProjects.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ProjectThumbnail(
    project: Project,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(project.canvas.name) }

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Miniatura guardada (actualizada en cada guardado por el editor)
            if (project.thumbnailBase64.isNotBlank()) {
                val decoded = runCatching {
                    android.util.Base64.decode(project.thumbnailBase64, android.util.Base64.DEFAULT)
                }.getOrNull()
                if (decoded != null) {
                    val bmp = runCatching {
                        android.graphics.BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                    }.getOrNull()
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            } else if (project.canvas.pixels.any { it != 0 }) {
                // Sin miniatura guardada: se dibuja el lienzo directamente
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val size = size.width.coerceAtMost(size.height)
                    val cellSize = size / project.canvas.width
                    project.canvas.pixels.forEachIndexed { index, argb ->
                        val color = Color(argb)
                        if (color.alpha > 0) {
                            val row = index / project.canvas.width
                            val col = index % project.canvas.width
                            drawRect(
                                color = color,
                                topLeft = androidx.compose.ui.geometry.Offset(col * cellSize, row * cellSize),
                                size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                            )
                        }
                    }
                    // Grid sutil
                    for (i in 0..project.canvas.width) {
                        drawLine(
                            color = Color.White.copy(0.1f),
                            start = androidx.compose.ui.geometry.Offset(i * cellSize, 0f),
                            end = androidx.compose.ui.geometry.Offset(i * cellSize, size),
                            strokeWidth = 0.5f
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppTheme.colors.surfaceVariant)
                )
            }

            // Nombre del proyecto
            Text(
                text = project.canvas.name,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(0.5f), RoundedCornerShape(0.dp, 0.dp, 12.dp, 12.dp))
            )

            // Botones de acción (rename/delete)
            Row(
                modifier = Modifier
                    .padding(4.dp)
                    .align(Alignment.TopEnd),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = {
                        newName = project.canvas.name
                        showRenameDialog = true
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = AppTheme.colors.surface.copy(0.92f),
                        contentColor = AppTheme.colors.textPrimary
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Rounded.DriveFileRenameOutline, contentDescription = "Renombrar", modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = onDelete,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = AppTheme.colors.surface.copy(0.92f),
                        contentColor = AppTheme.colors.danger
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Eliminar", modifier = Modifier.size(20.dp))
                }
            }
        }
    }

    if (showRenameDialog) {
        RenameDialog(
            initialName = project.canvas.name,
            onDismiss = { showRenameDialog = false },
            onConfirm = { name ->
                onRename(name)
                showRenameDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renombrar lienzo", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.padding(16.dp).width(300.dp)) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name) }, enabled = name.isNotBlank()) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
