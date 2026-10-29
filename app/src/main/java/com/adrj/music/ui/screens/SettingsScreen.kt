package com.adrj.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adrj.music.data.Prefs
import com.adrj.music.data.PrefsState
import com.adrj.music.ui.components.iosGlass
import com.adrj.music.ui.theme.GrisClaro
import com.adrj.music.ui.theme.accentColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Settings(
    val dark: Boolean,
    val accent: String = "Blanco",
    val fontScale: Float,
    val dynamicColor: Boolean = false
)

@Composable
fun SettingsScreen(settings: Settings, onSettingsChange: (Settings) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    // quality ahora vive en PrefsState (reactivo)
    var cacheSize by remember { mutableStateOf("Calculando…") }

    LaunchedEffect(Unit) {
        cacheSize = withContext(Dispatchers.IO) {
            val size = ctx.cacheDir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }
            "%.1f MB".format(size / (1024f * 1024f))
        }
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(20.dp))
        Text("Ajustes", style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(24.dp))

        // === APARIENCIA ===
        SectionTitle("Apariencia")
        SectionCard {
            Column {
                SwitchRow(Icons.Filled.DarkMode, "Tema oscuro", "Blanco, gris y negro",
                    PrefsState.dark) {
                    PrefsState.setDark(ctx, it)
                    onSettingsChange(settings.copy(dark = it))
                }

                Divider()

                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Palette, null, tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Text("Color de acento", color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("Blanco","Cian","Rojo","Azul","Verde","Morado","Ámbar","Rosa").forEach { name ->
                            val c = accentColor(name)
                            Box(
                                Modifier.size(36.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .then(
                                        if (settings.accent == name)
                                            Modifier.border(3.dp, androidx.compose.ui.graphics.Color.White, CircleShape)
                                        else Modifier
                                    )
                                    .clickable {
                                        PrefsState.setAccent(ctx, name)
                                        onSettingsChange(settings.copy(accent = name))
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (settings.accent == name) {
                                    Box(Modifier.size(14.dp).clip(CircleShape)
                                        .background(androidx.compose.ui.graphics.Color.White))
                                }
                            }
                        }
                    }
                }

                Divider()
                var sliderValue by remember { mutableFloatStateOf(PrefsState.fontScale) }
                LaunchedEffect(PrefsState.fontScale) { sliderValue = PrefsState.fontScale }
                SliderRow(Icons.Filled.FormatSize, "Tamaño de texto",
                    sliderValue, "${(sliderValue * 100).toInt()}%",
                    0.85f..1.3f,
                    onChange = { sliderValue = it },
                    onChangeFinished = {
                        PrefsState.setFontScale(ctx, it)
                        onSettingsChange(settings.copy(fontScale = it))
                    }
                )

                Divider()
                SwitchRow(Icons.Filled.AutoAwesome, "Liquid Glass",
                    "Efecto vidrio translúcido en toda la UI",
                    PrefsState.glassEnabled) {
                    PrefsState.setGlassEnabled(ctx, it)
                }

                Divider()
                SwitchRow(Icons.Filled.ColorLens, "Color dinámico",
                    "La UI adopta el color de la portada",
                    PrefsState.dynamicColor) {
                    PrefsState.setDynamicColor(ctx, it)
                    onSettingsChange(settings.copy(dynamicColor = it))
                }

                Divider()
                SwitchRow(Icons.Filled.Speed, "Reducir movimiento", "Menos animaciones",
                    PrefsState.reduceMotion) { PrefsState.setReduceMotion(ctx, it) }
                Divider()
                SwitchRow(Icons.Filled.ScreenRotation, "Modo horizontal",
                    "Reproductor en horizontal al girar",
                    PrefsState.landscapeMode) {
                    PrefsState.setLandscapeMode(ctx, it)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // === REPRODUCCIÓN ===
        SectionTitle("Reproducción")
        SectionCard {
            Column {
                SwitchRow(Icons.Filled.PlayCircle, "Autoplay", "Siguiente al terminar",
                    PrefsState.autoplay) { PrefsState.setAutoplay(ctx, it) }
                Divider()
                SwitchRow(Icons.Filled.Shuffle, "Aleatorio", "Orden aleatorio",
                    PrefsState.shuffle) { PrefsState.setShuffle(ctx, it) }
                Divider()
                ActionRow(Icons.Filled.Repeat, "Repetir",
                    repeatLabel(PrefsState.repeatMode)) {
                    PrefsState.setRepeat(ctx, (PrefsState.repeatMode + 1) % 3)
                }
                Divider()
                SwitchRow(Icons.Filled.PictureInPicture, "Mini siempre",
                    "Mini reproductor visible sin música",
                    PrefsState.miniAlways) { PrefsState.setMiniAlways(ctx, it) }
            }
        }

        Spacer(Modifier.height(20.dp))

        // === DESCARGAS ===
        SectionTitle("Descargas")
        SectionCard {
            Column {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Calidad", color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium)
                    Text(PrefsState.quality, color = GrisClaro,
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    listOf("Alta (m4a 128k)", "Media (m4a 96k)", "Baja (m4a 64k)").forEach { q ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = PrefsState.quality == q, onClick = {
                                PrefsState.setQuality(ctx, q)
                            })
                            Text(q, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                Divider()
                SwitchRow(Icons.Filled.Wifi, "Solo WiFi", "No descargar con datos",
                    PrefsState.wifiOnly) { PrefsState.setWifiOnly(ctx, it) }
            }
        }

        Spacer(Modifier.height(20.dp))

        // === ALMACENAMIENTO ===
        SectionTitle("Almacenamiento")
        SectionCard {
            Column {
                SwitchRow(Icons.Filled.Image, "Caché portadas", "Guardar offline",
                    PrefsState.imageCache) { PrefsState.setImageCache(ctx, it) }
                Divider()
                SwitchRow(Icons.Filled.Gradient, "Transiciones imagen", "Desvanecido al cargar",
                    PrefsState.crossfade) { PrefsState.setCrossfade(ctx, it) }
                Divider()
                ActionRow(Icons.Filled.CleaningServices, "Limpiar caché", cacheSize) {
                    scope.launch(Dispatchers.IO) {
                        withContext(Dispatchers.IO) {
                            ctx.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
                        }
                        cacheSize = "0.0 MB"
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // === ACERCA DE ===
        SectionTitle("Acerca de")
        SectionCard {
            ActionRow(Icons.Filled.Info, "ADRJmusic", "v1.0 · yt-dlp embebido") {}
        }

        Spacer(Modifier.height(160.dp))
    }
}

private fun repeatLabel(mode: Int) = when (mode) {
    0 -> "Desactivado"
    1 -> "Repetir todo"
    else -> "Repetir una"
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = GrisClaro,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .iosGlass(24)
            .padding(vertical = 4.dp)
    ) {
        Column(Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).padding(start = 56.dp)
        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)))
}

@Composable
private fun SwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
            Text(subtitle, color = GrisClaro, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SliderRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String, value: Float, valueText: String,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    onChangeFinished: (Float) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                Text(valueText, color = GrisClaro, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(8.dp))
        Slider(
            value = value,
            onValueChange = onChange,
            onValueChangeFinished = { onChangeFinished(value) },
            valueRange = range
        )
    }
}

@Composable
private fun ActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String, subtitle: String, onClick: () -> Unit
) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
            Text(subtitle, color = GrisClaro, style = MaterialTheme.typography.bodyMedium)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = GrisClaro)
    }
}
