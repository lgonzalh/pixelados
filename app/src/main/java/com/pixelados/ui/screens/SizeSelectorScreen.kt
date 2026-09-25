package com.pixelados.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pixelados.data.model.CanvasSize
import com.pixelados.ui.navigation.Screen
import com.pixelados.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SizeSelectorScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val theme = AppTheme.colors
    var showCustomDialog by remember { mutableStateOf(false) }
    var customWidth by remember { mutableStateOf(32) }
    var customHeight by remember { mutableStateOf(32) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tamaño del lienzo",
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
        containerColor = theme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "¿Qué vas a dibujar?",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = theme.textPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            Text(
                text = "Elige un tamaño para empezar",
                fontSize = 13.sp,
                color = theme.textSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )

            val sizes = listOf(
                CanvasSize.SMALL,
                CanvasSize.MEDIUM,
                CanvasSize.LARGE,
                CanvasSize.XLARGE
            )

            sizes.forEach { size ->
                SizeCard(
                    size = size,
                    onClick = {
                        navController.navigate(Screen.Editor.createRoute(size.width, size.height))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Divisor
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(modifier = Modifier.weight(1f), color = theme.border)
                Text("  O elige tu tamaño  ", color = theme.textSecondary, fontSize = 12.5.sp)
                Divider(modifier = Modifier.weight(1f), color = theme.border)
            }

            Spacer(Modifier.height(16.dp))

            // Slider personalizado
            CustomSizeSlider(
                width = customWidth,
                height = customHeight,
                onWidthChange = { customWidth = it },
                onHeightChange = { customHeight = it },
                onConfirm = {
                    navController.navigate(Screen.Editor.createRoute(customWidth, customHeight))
                }
            )
        }
    }
}

@Composable
fun SizeCard(
    size: CanvasSize,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = AppTheme.colors
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = theme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Miniatura del grid
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .padding(end = 16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .border(1.dp, theme.border, RoundedCornerShape(10.dp))
            ) {
                // La cuadrícula se dibuja sobre el tamaño REAL del recuadro
                // (antes usaba 80 fijo y quedaba recortada/desalineada).
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // "box" = tamaño real del recuadro en pantalla;
                    // "size" (parámetro) = tamaño del lienzo elegido.
                    val box = this.size
                    val cellSize = box.minDimension / maxOf(size.width, size.height).toFloat()
                    val gridW = cellSize * size.width
                    val gridH = cellSize * size.height
                    val offsetX = (box.width - gridW) / 2f
                    val offsetY = (box.height - gridH) / 2f
                    for (i in 0..size.width) {
                        drawLine(
                            color = Color(0xFFBDBDBD),
                            start = Offset(offsetX + i * cellSize, offsetY),
                            end = Offset(offsetX + i * cellSize, offsetY + gridH),
                            strokeWidth = 1f,
                            cap = StrokeCap.Butt
                        )
                    }
                    for (i in 0..size.height) {
                        drawLine(
                            color = Color(0xFFBDBDBD),
                            start = Offset(offsetX, offsetY + i * cellSize),
                            end = Offset(offsetX + gridW, offsetY + i * cellSize),
                            strokeWidth = 1f,
                            cap = StrokeCap.Butt
                        )
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${size.width}×${size.height}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.textPrimary
                )
                Text(
                    text = size.label,
                    fontSize = 13.5.sp,
                    color = theme.textSecondary
                )
                Text(
                    text = size.description,
                    fontSize = 11.sp,
                    color = theme.textSecondary.copy(0.8f)
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = theme.primary,
                modifier = Modifier.size(28.dp).padding(start = 16.dp)
            )
        }
    }
}

@Composable
fun CustomSizeSlider(
    width: Int,
    height: Int,
    onWidthChange: (Int) -> Unit,
    onHeightChange: (Int) -> Unit,
    onConfirm: () -> Unit
) {
    val theme = AppTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        // Ancho
        VStack(spacing = 8.dp) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Ancho", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = theme.textPrimary)
                Text("${width}", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = theme.primary)
            }
            Slider(
                modifier = Modifier.fillMaxWidth(),
                value = width.toFloat(),
                onValueChange = { onWidthChange(it.toInt()) },
                valueRange = 8f..128f,
                steps = 120,
                colors = SliderDefaults.colors(
                    thumbColor = theme.primary,
                    activeTrackColor = theme.primary,
                    inactiveTrackColor = theme.primary.copy(alpha = 0.24f)
                )
            )
            Text("Mín 8 — Máx 128", fontSize = 11.sp, color = theme.textSecondary)
        }

        Spacer(Modifier.height(16.dp))

        // Alto
        VStack(spacing = 8.dp) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Alto", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = theme.textPrimary)
                Text("${height}", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = theme.primary)
            }
            Slider(
                modifier = Modifier.fillMaxWidth(),
                value = height.toFloat(),
                onValueChange = { onHeightChange(it.toInt()) },
                valueRange = 8f..128f,
                steps = 120,
                colors = SliderDefaults.colors(
                    thumbColor = theme.primary,
                    activeTrackColor = theme.primary,
                    inactiveTrackColor = theme.primary.copy(alpha = 0.24f)
                )
            )
            Text("Mín 8 — Máx 128", fontSize = 11.sp, color = theme.textSecondary)
        }

        Spacer(Modifier.height(24.dp))

        // Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(1.dp, theme.border, RoundedCornerShape(12.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val size = size.width.coerceAtMost(size.height)
                val cellSize = size / width.coerceAtMost(height)
                val displayW = (width * cellSize).toInt()
                val displayH = (height * cellSize).toInt()
                val offsetX = (size - displayW) / 2
                val offsetY = (size - displayH) / 2
                for (i in 0..width) {
                    drawLine(
                        color = Color(0xFFBDBDBD),
                        start = Offset(offsetX + i * cellSize, offsetY),
                        end = Offset(offsetX + i * cellSize, offsetY + displayH),
                        strokeWidth = 1f
                    )
                }
                for (i in 0..height) {
                    drawLine(
                        color = Color(0xFFBDBDBD),
                        start = Offset(offsetX, offsetY + i * cellSize),
                        end = Offset(offsetX + displayW, offsetY + i * cellSize),
                        strokeWidth = 1f
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = theme.primary, contentColor = theme.onPrimary)
        ) {
            Text("Crear lienzo ${width}×${height}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun VStack(
    modifier: Modifier = Modifier,
    spacing: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing), content = content)
}
