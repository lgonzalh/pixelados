package com.pixelados.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pixelados.ui.theme.AppTheme
import com.pixelados.ui.theme.PaletteId
import com.pixelados.ui.theme.PixelThemes

/**
 * Ajustes de apariencia: paleta de colores de la app y modo claro/oscuro.
 * Los cambios se aplican al instante en todas las pantallas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    paletteId: String,
    darkTheme: Boolean,
    onPaletteSelected: (String) -> Unit,
    onDarkThemeChange: (Boolean) -> Unit
) {
    val colors = AppTheme.colors

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ajustes",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.bar)
            )
        },
        containerColor = colors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Colores de la app",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Cambia el fondo, las barras, los bordes y los iconos de una vez.",
                fontSize = 12.sp,
                color = colors.textSecondary
            )
            Spacer(Modifier.height(18.dp))

            // Dos paletas por fila, con vista previa circular
            val palettes = PaletteId.entries
            palettes.chunked(2).forEach { fila ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    fila.forEach { palette ->
                        PaletteCard(
                            palette = palette,
                            selected = palette.id == paletteId,
                            dark = darkTheme,
                            onClick = { onPaletteSelected(palette.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (fila.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
            }

            Spacer(Modifier.height(6.dp))

            // Modo oscuro
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (darkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                    contentDescription = null,
                    tint = colors.primary
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tema oscuro",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = if (darkTheme) "Activado: menos brillo, menos cansancio" else "Desactivado",
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                }
                Switch(
                    checked = darkTheme,
                    onCheckedChange = onDarkThemeChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.onPrimary,
                        checkedTrackColor = colors.primary,
                        uncheckedThumbColor = colors.textDisabled,
                        uncheckedTrackColor = colors.surfaceVariant
                    )
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PaletteCard(
    palette: PaletteId,
    selected: Boolean,
    dark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val preview = PixelThemes.colors(palette, dark)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) colors.surfaceVariant else colors.surface)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colors.primary else colors.border,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(64.dp)) {
                val half = size.width / 2f
                // Cuatro cuadrantes: fondo, color principal, acento y superficie
                drawRect(preview.background, size = Size(half, half))
                drawRect(preview.surface, topLeft = Offset(half, 0f), size = Size(half, half))
                drawRect(preview.primary, topLeft = Offset(0f, half), size = Size(half, half))
                drawRect(preview.accent, topLeft = Offset(half, half), size = Size(half, half))
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Paleta seleccionada",
                        tint = preview.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = palette.label,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) colors.textPrimary else colors.textSecondary
        )
    }
}
