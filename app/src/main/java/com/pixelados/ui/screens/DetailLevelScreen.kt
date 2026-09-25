package com.pixelados.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.pixelados.data.util.PhotoPixelator
import com.pixelados.ui.navigation.Screen
import com.pixelados.R
import com.pixelados.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

class DetailLevelViewModel(
    private val photoUri: String,
    private val context: android.content.Context
) : ViewModel() {

    var detailIndex by mutableStateOf(3) // Default to 32x32 (index 2)
        set

    init {
        loadRecommendedDetail()
    }

    private fun loadRecommendedDetail() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uri = android.net.Uri.parse(photoUri)
                val bitmap = android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                val idx = PhotoPixelator.recommendedDetailLevel(bitmap)
                bitmap.recycle()
                detailIndex = idx
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailLevelScreen(
    navController: NavController,
    photoUri: String,
    context: android.content.Context
) {
    val theme = AppTheme.colors
    val viewModel: DetailLevelViewModel = viewModel(
        factory = DetailLevelViewModelFactory(photoUri, context)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Nivel de detalle",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = theme.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = theme.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = theme.bar)
            )
        },
        bottomBar = {
            Box(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = {
                        navController.navigate(Screen.Editor.createRoute(
                            PhotoPixelator.detailLevels[viewModel.detailIndex],
                            PhotoPixelator.detailLevels[viewModel.detailIndex],
                            fromPhoto = true,
                            photoUri = photoUri
                        ))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primary, contentColor = theme.onPrimary)
                ) {
                    Text(
                        text = "Convertir y editar",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        containerColor = theme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Arrastra para elegir cuánto detalle",
                fontSize = 14.5.sp,
                color = AppTheme.colors.textPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // Slider visual con cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PhotoPixelator.detailLevels.forEachIndexed { index, size ->
                    val isSelected = index == viewModel.detailIndex
                    DetailLevelCard(
                        size = size,
                        label = when (size) {
                            16 -> "Ícono diminuto"
                            24 -> "Muy pequeño"
                            32 -> "Pequeño (personaje)"
                            48 -> "Mediano"
                            64 -> "Grande (detalles)"
                            80 -> "Muy grande"
                            100 -> "Escena grande"
                            128 -> "Máximo detalle"
                            else -> "${size}×${size}"
                        },
                        isSelected = isSelected,
                        onClick = { viewModel.detailIndex = index }
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
        }
    }
}

class DetailLevelViewModelFactory(
    private val photoUri: String,
    private val context: android.content.Context
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DetailLevelViewModel(photoUri, context) as T
    }
}

@Composable
fun DetailLevelCard(
    size: Int,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val theme = AppTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) theme.primary else theme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Miniatura del grid
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .padding(end = 16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .border(1.dp, theme.border, RoundedCornerShape(10.dp))
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    // Se dibuja sobre el tamaño REAL del recuadro (60dp menos el
                    // padding); antes usaba 60 fijo y la cuadrícula salía recortada.
                    val box = this.size
                    val cellSize = box.minDimension / size
                    for (i in 0..size) {
                        drawLine(
                            color = Color(0xFFBDBDBD),
                            start = androidx.compose.ui.geometry.Offset(i * cellSize, 0f),
                            end = androidx.compose.ui.geometry.Offset(i * cellSize, box.height),
                            strokeWidth = 1f
                        )
                    }
                    for (i in 0..size) {
                        drawLine(
                            color = Color(0xFFBDBDBD),
                            start = androidx.compose.ui.geometry.Offset(0f, i * cellSize),
                            end = androidx.compose.ui.geometry.Offset(box.width, i * cellSize),
                            strokeWidth = 1f
                        )
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${size}×${size}",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) theme.onPrimary else theme.textPrimary
                )
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = if (isSelected) theme.onPrimary.copy(0.9f) else theme.textSecondary
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Seleccionado",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp).padding(end = 8.dp)
                )
            }
        }
    }
}
