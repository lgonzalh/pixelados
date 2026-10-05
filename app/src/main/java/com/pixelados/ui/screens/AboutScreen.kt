package com.pixelados.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.pixelados.R
import com.pixelados.util.VersionHelper
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pixelados.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AboutScreen(navController: NavController) {
    val context = LocalContext.current
    val theme = AppTheme.colors
    fun openUrl(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.about_screen)) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = theme.bar,
                        titleContentColor = theme.textPrimary,
                        navigationIconContentColor = theme.textPrimary
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.luis),
                    contentDescription = "Luis Gonzalez",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(24.dp))
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Luis Gonzalez",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = theme.textPrimary
                )
                Text(
                    text = "AI Engineer | Programmer | Entrepreneur",
                    style = MaterialTheme.typography.titleSmall,
                    color = theme.textSecondary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = VersionHelper.getGreekVersionString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.primary
                )
                Spacer(Modifier.height(18.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = theme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Soy Luis Gonzalez, AI Engineer especializado en diseñar herramientas y plataformas que combinan inteligencia humana e inteligencia artificial para ampliar las capacidades de los equipos de desarrollo.",
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Construyo plataformas B2B, Micro-SaaS y herramientas de ingeniería de software principalmente sobre .NET/C#, arquitecturas web y serverless, con especial interés en sistemas local-first y aplicaciones donde la IA puede trabajar directamente sobre el entorno y los datos del usuario.",
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "La IA no debería limitarse a generar código: debe comprender el proyecto, conservar su contexto y participar en su evolución de forma trazable. Ese principio está en acción en herramientas como Condor y en las soluciones empresariales de Lantonium para automatizar cargas masivas de datos.",
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = theme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Pila tecnológica",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        FlowRow(
                            modifier = Modifier.padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                ".NET C#", "ASP.NET Core", "PostgreSQL", "Supabase", "Kotlin",
                                "Android SDK", "Python", "Ollama", "SignalR", "Git", "Firebase",
                                "Google Cloud Run"
                            ).forEach { tag ->
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(tag) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = theme.surfaceVariant,
                                        labelColor = theme.textPrimary
                                    ),
                                    border = SuggestionChipDefaults.suggestionChipBorder(
                                        enabled = true,
                                        borderColor = theme.border
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = theme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Contacto",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ContactIcon(R.drawable.lantonium_logo_black, "Sitio web", Modifier.weight(1f)) {
                                openUrl("https://lantonium.com/acerca")
                            }
                            ContactIcon(R.drawable.github, "GitHub", Modifier.weight(1f)) {
                                openUrl("https://github.com/lgonzalh")
                            }
                            ContactIcon(R.drawable.linkedin, "LinkedIn", Modifier.weight(1f)) {
                                openUrl("https://www.linkedin.com/in/lantonium/")
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ContactIcon(R.drawable.whatsapp, "WhatsApp", Modifier.weight(1f)) {
                                openUrl("https://wa.me/573246864991")
                            }
                            ContactIcon(R.drawable.outlook, "Email", Modifier.weight(1f)) {
                                openUrl("mailto:lgonzalh@outlook.com")
                            }
                            Spacer(Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Si quieres conversar sobre un proyecto que tengas en mente, por ejemplo esta app la desarrollé especialmente para mi hijo Juanes, o simplemente intercambiar ideas sobre ingeniería de software, puedes escribirme a lgonzalh@outlook.com o conectarte conmigo en LinkedIn o GitHub o por WhatsApp.",
                            style = MaterialTheme.typography.bodySmall,
                            color = theme.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text(
                    text = "© 2021–2026 Luis Gonzalez. Todos los derechos reservados.",
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.textDisabled,
                    textAlign = TextAlign.Center
                )
            }
    }
}

@Composable
private fun ContactIcon(drawableRes: Int, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(10.dp),
            // Los iconos de contacto son line-art negro: van sobre tarjeta blanca
            // (igual que el lienzo) para que se vean en tema claro y oscuro.
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.colors.border)
        ) {
            Image(
                painter = painterResource(drawableRes),
                contentDescription = label,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppTheme.colors.textPrimary
        )
    }
}
