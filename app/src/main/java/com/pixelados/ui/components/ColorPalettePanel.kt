package com.pixelados.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelados.data.model.ColorPalette
import com.pixelados.ui.theme.AppTheme

/**
 * Paleta de colores para niños.
 *
 * Solo tiene DOS cosas que un niño entiende a la primera:
 *  1. Una cuadrícula grande de colores ordenada por familias ("Rojos", "Azules"…).
 *  2. Un botón "Otro color" con un selector de UNA barra de arcoíris.
 */
@Composable
fun ColorPalettePanel(
    activeColor: Int,
    onColorSelected: (Int) -> Unit,
    savedColors: List<Int>,
    onSavedColorRemoved: (Int) -> Unit,
    onChangeAllColors: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = AppTheme.colors
    var showCustomPicker by remember { mutableStateOf(false) }
    var expandedFamily by remember { mutableStateOf<Int?>(0) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Encabezado: color elegido + botón "Otro color"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(activeColor))
                    .border(2.dp, theme.border, RoundedCornerShape(14.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("Elige un color", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = theme.textPrimary)
                Text("Toca un cuadro y píntalo en el lienzo", fontSize = 12.sp, color = theme.textSecondary)
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(theme.primary)
                    .clickable { showCustomPicker = true }
                    .padding(horizontal = 14.dp, vertical = 11.dp)
                    .semantics { contentDescription = "Otro color" },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Palette,
                    contentDescription = null,
                    tint = theme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Otro color", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = theme.onPrimary)
            }
        }

        RecentColorsRow(
            colors = savedColors,
            activeColor = activeColor,
            onColorSelected = onColorSelected,
            onColorRemoved = onSavedColorRemoved
        )
        Divider(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            color = theme.border
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = 360.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Todos los colores (${ColorPalette.allColors.size})",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = theme.textSecondary
            )
            ColorPalette.colorFamilies.forEachIndexed { index, family ->
                ColorFamilySection(
                    family = family,
                    isExpanded = expandedFamily == index,
                    activeColor = activeColor,
                    onToggle = { expandedFamily = if (expandedFamily == index) null else index },
                    onColorSelected = onColorSelected
                )
            }
            TextButton(onClick = onChangeAllColors, modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    "Cambiar un color en todo el dibujo",
                    fontSize = 13.sp,
                    color = theme.textSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    if (showCustomPicker) {
        SimpleColorPickerDialog(
            initialColor = activeColor,
            onDismiss = { showCustomPicker = false },
            onColorSelected = {
                onColorSelected(it)
                showCustomPicker = false
            }
        )
    }
}

@Composable
private fun RecentColorsRow(
    colors: List<Int>,
    activeColor: Int,
    onColorSelected: (Int) -> Unit,
    onColorRemoved: (Int) -> Unit
) {
    val theme = AppTheme.colors
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Colores recientes",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = theme.textSecondary
            )
            if (colors.isNotEmpty()) {
                Text(
                    text = "Mantén pulsado para quitar",
                    fontSize = 10.sp,
                    color = theme.textDisabled
                )
            }
        }
        if (colors.isEmpty()) {
            Text(
                text = "Aquí verás los colores que vayas usando.",
                fontSize = 11.5.sp,
                color = theme.textSecondary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
            )
        } else {
            // Fila compacta: son accesos rápidos, no la paleta completa.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                colors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color(color))
                            .border(
                                width = if (color == activeColor) 3.dp else 1.dp,
                                color = if (color == activeColor) theme.primary else theme.border,
                                shape = RoundedCornerShape(9.dp)
                            )
                            .pointerInput(color) {
                                detectTapGestures(
                                    onTap = { onColorSelected(color) },
                                    onLongPress = { onColorRemoved(color) }
                                )
                            }
                            .semantics { contentDescription = "Color reciente" }
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorFamilySection(
    family: ColorPalette.ColorFamily,
    isExpanded: Boolean,
    activeColor: Int,
    onToggle: () -> Unit,
    onColorSelected: (Int) -> Unit
) {
    val theme = AppTheme.colors
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onToggle() }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = family.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = theme.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Cerrar ${family.name}" else "Abrir ${family.name}",
                tint = theme.textSecondary
            )
        }

        if (isExpanded) {
            family.colors.chunked(8).forEach { rowColors ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowColors.forEach { color ->
                        ColorSwatch(
                            color = color,
                            isActive = color == activeColor,
                            onClick = { onColorSelected(color) },
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        )
                    }
                    repeat(8 - rowColors.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
fun ColorSwatch(
    color: Int,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = AppTheme.colors
    val composeColor = Color(color)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(composeColor)
            .border(
                width = if (isActive) 3.dp else 1.dp,
                color = if (isActive) theme.primary else theme.border,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .semantics { contentDescription = ColorPalette.colorName(color) },
        contentAlignment = Alignment.Center
    ) {
        if (isActive) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = if (composeColor.luminance() > 0.55f) Color.Black else Color.White,
                modifier = Modifier.fillMaxSize(0.6f)
            )
        }
    }
}

private fun Color.luminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue

/**
 * Selector de color SIMPLE: una barra de arcoíris y una barra de claridad.
 */
@Composable
fun SimpleColorPickerDialog(
    initialColor: Int,
    onDismiss: () -> Unit,
    onColorSelected: (Int) -> Unit,
    title: String = "Otro color"
) {
    val theme = AppTheme.colors
    val hsv = remember(initialColor) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initialColor, it) }
    }
    var hue by remember { mutableStateOf(hsv[0]) }
    var brightness by remember { mutableStateOf(hsv[2].coerceIn(0.35f, 1f)) }

    val preview = Color.hsv(hue, 1f, brightness)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = theme.surface,
        title = { Text(title, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = theme.textPrimary) },
        text = {
            Column(modifier = Modifier.padding(top = 6.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(preview)
                        .border(2.dp, theme.border, RoundedCornerShape(16.dp))
                )
                Spacer(Modifier.height(16.dp))
                Text("1. Desliza para elegir el color", fontSize = 14.sp, color = theme.textPrimary, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                GradientSlider(
                    value = hue,
                    range = 0f..360f,
                    onValueChange = { hue = it },
                    brush = Brush.horizontalGradient((0..36).map { Color.hsv(it * 10f, 1f, 1f) })
                )
                Spacer(Modifier.height(16.dp))
                Text("2. Desliza para aclarar u oscurecer", fontSize = 14.sp, color = theme.textPrimary, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                GradientSlider(
                    value = brightness,
                    range = 0.35f..1f,
                    onValueChange = { brightness = it },
                    brush = Brush.horizontalGradient(
                        listOf(Color.hsv(hue, 1f, 0.35f), Color.hsv(hue, 1f, 0.75f), Color.hsv(hue, 1f, 1f))
                    )
                )
            }
        },
        confirmButton = {
            Button(onClick = { onColorSelected(preview.toArgb()) }) {
                Text("Usar este color")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = theme.textSecondary) }
        }
    )
}

@Composable
private fun GradientSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    brush: Brush
) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(RoundedCornerShape(10.dp))
        ) {
            drawRect(brush = brush, topLeft = Offset.Zero, size = size)
        }
        Slider(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            )
        )
    }
}
