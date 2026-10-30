package com.adrj.music.ui.screens

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.adrj.music.data.Prefs
import com.adrj.music.lyrics.LyricLine
import com.adrj.music.lyrics.LyricsApi
import com.adrj.music.player.PlayerHolder
import com.adrj.music.ui.theme.DynamicAccent
import com.adrj.music.ui.theme.GrisClaro
import com.adrj.music.ui.theme.rememberDominantColor

private val LineHeight = 64.dp

@Composable
fun FullPlayerScreen(onClose: () -> Unit) {
    val track = PlayerHolder.currentTrack
    if (track == null) { onClose(); return }

    val ctx = LocalContext.current
    val blurEnabled = com.adrj.music.data.PrefsState.blurBg

    val pos = PlayerHolder.position.coerceAtLeast(0)
    val dur = PlayerHolder.duration.coerceAtLeast(1)
    var slider by remember { mutableStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(pos) {
        if (!dragging) slider = pos.toFloat()
    }
    var mode by remember { mutableStateOf(0) }

    var syncedLines by remember(track.id) { mutableStateOf<List<LyricLine>>(emptyList()) }
    var plainLyrics by remember(track.id) { mutableStateOf("") }
    var loadingLyrics by remember(track.id) { mutableStateOf(true) }
    var currentIdx by remember { mutableStateOf(-1) }

    LaunchedEffect(track.id) {
        loadingLyrics = true
        // Esperar a que ExoPlayer sepa la duración real (para mejores matches)
        kotlinx.coroutines.delay(800)
        val realDur = if (PlayerHolder.duration > 0L) PlayerHolder.duration / 1000 else track.durationSec
        val l = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            LyricsApi.fetch(track.artist, track.title, realDur)
        }
        syncedLines = l?.synced ?: emptyList()
        plainLyrics = l?.plain ?: ""
        loadingLyrics = false
    }

    // FIX BUG 4: búsqueda optimizada (no recorre toda la letra cada 500ms)
    var lastIdx by remember { mutableStateOf(0) }
    LaunchedEffect(pos) {
        if (syncedLines.isEmpty()) {
            currentIdx = -1
            return@LaunchedEffect
        }
        var idx = lastIdx.coerceIn(0, syncedLines.size - 1)
        // Avanzar mientras el tiempo lo permita
        while (idx + 1 < syncedLines.size && syncedLines[idx + 1].timeMs <= pos) idx++
        // Retroceder si es necesario (seek hacia atrás)
        while (idx > 0 && syncedLines[idx].timeMs > pos) idx--
        if (idx != currentIdx) {
            currentIdx = idx
            lastIdx = idx
        }
    }

    val baseBg = MaterialTheme.colorScheme.background
    val dominant = rememberDominantColor(track.thumbnailUrl, baseBg)

    LaunchedEffect(dominant, com.adrj.music.data.PrefsState.dynamicColor) {
        if (com.adrj.music.data.PrefsState.dynamicColor) {
            DynamicAccent.color = dominant
        }
    }

    val topColor by animateColorAsState(dominant.copy(alpha = 0.65f), tween(1000), label = "top")
    val midColor by animateColorAsState(dominant.copy(alpha = 0.22f), tween(1000), label = "mid")
    val accent by animateColorAsState(dominant, tween(1000), label = "acc")
    val artScale by animateFloatAsState(
        if (PlayerHolder.isPlaying) 1f else 0.92f, tween(400), label = "art")

    // Gradiente animado (rotación suave)
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "bg")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(15000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "animatedGradientOffset"
    )

    Box(Modifier.fillMaxSize()) {
        if (blurEnabled && track.thumbnailUrl.isNotBlank()) {
            AsyncImage(
                model = track.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(25.dp).alpha(0.85f)
            )
        }

        Box(Modifier.fillMaxSize().background(
            Brush.linearGradient(
                colors = listOf(topColor, midColor, baseBg, midColor, topColor),
                start = androidx.compose.ui.geometry.Offset(animatedOffset % 400f, 0f),
                end = androidx.compose.ui.geometry.Offset(
                    (animatedOffset % 400f) + 400f, 800f
                )
            )
        ))

        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.KeyboardArrowDown, "Cerrar",
                        tint = MaterialTheme.colorScheme.onBackground)
                }
                Spacer(Modifier.weight(1f))
                Text(if (mode == 1) "Letra" else "Reproduciendo",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { mode = if (mode == 0) 1 else 0 }) {
                    Icon(
                        if (mode == 1) Icons.Filled.MusicNote else Icons.Filled.Lyrics,
                        "Letras", tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(onClick = {
                    com.adrj.music.data.PrefsState.landscapeMode = true
                }) {
                    Icon(Icons.Filled.ScreenRotation, "Modo horizontal",
                        tint = MaterialTheme.colorScheme.onBackground)
                }
                TextButton(onClick = {
                    com.adrj.music.player.PlaybackSpeed.cycle()
                }) {
                    Text(
                        com.adrj.music.player.PlaybackSpeed.label(),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (mode == 1) {
                LyricsArea(
                    loading = loadingLyrics,
                    synced = syncedLines,
                    plain = plainLyrics,
                    currentIdx = currentIdx,
                    accent = accent,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            } else {
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(8.dp))

                    Box(
                        Modifier.fillMaxWidth().aspectRatio(1f).scale(artScale)
                            .clip(RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(track.thumbnailUrl, null, Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop)
                    }
                    Spacer(Modifier.height(28.dp))
                    Text(track.title, style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(track.artist,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                    Spacer(Modifier.height(20.dp))
                    Slider(
                        value = if (dragging) slider else pos.toFloat(),
                        onValueChange = { dragging = true; slider = it },
                        onValueChangeFinished = {
                            PlayerHolder.seekTo(slider.toLong()); dragging = false
                        },
                        valueRange = 0f..dur.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.onBackground,
                            activeTrackColor = MaterialTheme.colorScheme.onBackground,
                            inactiveTrackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
                        )
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(fmt(if (dragging) slider.toLong() else pos),
                            color = GrisClaro, style = MaterialTheme.typography.labelSmall)
                        Text(fmt(dur), color = GrisClaro, style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { PlayerHolder.prev() }) {
                            Icon(Icons.Filled.SkipPrevious, null,
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(40.dp))
                        }
                        IconButton(onClick = {
                            PlayerHolder.seekTo((pos - 10_000).coerceAtLeast(0))
                        }) {
                            Icon(Icons.Filled.Replay10, "Retroceder 10s",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(40.dp))
                        }
                        IconButton(
                            onClick = { PlayerHolder.togglePlay() },
                            modifier = Modifier.size(72.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onBackground)
                        ) {
                            Icon(
                                if (PlayerHolder.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                null, tint = MaterialTheme.colorScheme.background,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        IconButton(onClick = {
                            PlayerHolder.seekTo((pos + 10_000).coerceAtMost(dur))
                        }) {
                            Icon(Icons.Filled.Forward10, "Adelantar 10s",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(40.dp))
                        }
                        IconButton(onClick = { PlayerHolder.next() }) {
                            Icon(Icons.Filled.SkipNext, null,
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(40.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsArea(
    loading: Boolean,
    synced: List<LyricLine>,
    plain: String,
    currentIdx: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val hasSynced = synced.isNotEmpty()

    // Autoscroll: calcular posición exacta con altura fija de item
    LaunchedEffect(currentIdx) {
        if (currentIdx < 0 || !hasSynced) return@LaunchedEffect
        try {
            // Con contentPadding vertical = 240dp y altura fija 64dp:
            // El item currentIdx queda centrado si scrolleamos a:
            // offset = currentIdx * 64dp - (viewportHeight/2 - 32dp)
            listState.animateScrollToItem(
                index = currentIdx,
                scrollOffset = -(100)  // ajuste fino para centrar
            )
        } catch (e: Exception) { android.util.Log.e("ADRJ_CRITICAL", "Error silencioso atrapado", e) }
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        when {
            loading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground)

            !hasSynced && plain.isBlank() -> Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.Lyrics, null,
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                    modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(12.dp))
                Text("Sin letra disponible",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            }

            !hasSynced -> {
                val scroll = rememberScrollState()
                Text(plain,
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 28.sp,
                    modifier = Modifier.fillMaxSize().verticalScroll(scroll)
                        .padding(horizontal = 8.dp, vertical = 20.dp))
            }

            else -> LazyColumn(
                state = listState,
                contentPadding = PaddingValues(vertical = 240.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    synced,
                    key = { i, _ -> i },
                    contentType = { _, _ -> "line" }
                ) { i, line ->
                    LyricLineView(
                        text = line.text,
                        active = i == currentIdx,
                        passed = i < currentIdx,
                        accent = accent
                    )
                }
            }
        }
    }
}

@Composable
private fun LyricLineView(text: String, active: Boolean, passed: Boolean, accent: Color) {
    val scale by animateFloatAsState(
        if (active) 1.06f else 1f,
        tween(300, easing = FastOutSlowInEasing), label = "scale"
    )
    val alpha by animateFloatAsState(
        when { active -> 1f; passed -> 0.5f; else -> 0.32f },
        tween(300), label = "alpha"
    )
    val color by animateColorAsState(
        if (active) MaterialTheme.colorScheme.onBackground
        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        tween(300), label = "color"
    )

    Box(
        Modifier.fillMaxWidth().height(LineHeight),
        contentAlignment = Alignment.Center
    ) {
        if (active) {
            Box(Modifier.fillMaxWidth(0.92f).height(58.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.horizontalGradient(listOf(
                    accent.copy(alpha = 0.05f),
                    accent.copy(alpha = 0.22f),
                    accent.copy(alpha = 0.05f)
                ))))
        }
        Text(
            text = text,
            color = color,
            fontSize = if (active) 22.sp else 17.sp,
            lineHeight = 30.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().scale(scale).alpha(alpha)
                .padding(horizontal = 16.dp)
        )
    }
}

private fun fmt(ms: Long): String {
    val t = ms / 1000
    return "%d:%02d".format(t / 60, t % 60)
}
