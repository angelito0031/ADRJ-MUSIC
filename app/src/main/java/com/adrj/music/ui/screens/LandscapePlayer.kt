package com.adrj.music.ui.screens

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.adrj.music.lyrics.LyricLine
import com.adrj.music.lyrics.LyricsApi
import com.adrj.music.player.PlayerHolder
import com.adrj.music.ui.theme.rememberDominantColor

@Composable
fun LandscapePlayer(onClose: () -> Unit) {
    val track = PlayerHolder.currentTrack
    if (track == null) { onClose(); return }

    val pos = PlayerHolder.position.coerceAtLeast(0)
    val dur = PlayerHolder.duration.coerceAtLeast(1)
    val baseBg = MaterialTheme.colorScheme.background
    val dominant = rememberDominantColor(track.thumbnailUrl, baseBg)

    var syncedLines by remember(track.id) { mutableStateOf<List<LyricLine>>(emptyList()) }
    var currentIdx by remember { mutableStateOf(-1) }
    var controlsVisible by remember { mutableStateOf(false) }

    LaunchedEffect(track.id) {
        val l = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            LyricsApi.fetch(track.artist, track.title, track.durationSec)
        }
        syncedLines = l?.synced ?: emptyList()
    }
    LaunchedEffect(pos) {
        if (syncedLines.isEmpty()) return@LaunchedEffect
        var idx = -1
        for (i in syncedLines.indices) {
            if (syncedLines[i].timeMs <= pos) idx = i else break
        }
        currentIdx = idx
    }

    val listState = rememberLazyListState()
    LaunchedEffect(currentIdx) {
        if (currentIdx >= 0) try {
            listState.scrollToItem(currentIdx, scrollOffset = -100)
        } catch (e: Exception) { android.util.Log.e("ADRJ_CRITICAL", "Error silencioso atrapado", e) }
    }

    Box(
        Modifier.fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { controlsVisible = !controlsVisible }
    ) {
        // Fondo con gradiente del color dominante
        Box(Modifier.fillMaxSize().background(
            Brush.horizontalGradient(listOf(
                dominant.copy(alpha = 0.55f),
                baseBg,
                baseBg
            ))
        ))

        // Contenido principal
        Row(Modifier.fillMaxSize()) {
            // Mitad izquierda: portada + barra de progreso (sin título)
            Column(
                Modifier.weight(1f).fillMaxHeight().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AsyncImage(
                    model = track.thumbnailUrl, contentDescription = null,
                    modifier = Modifier.fillMaxWidth(0.85f).aspectRatio(1f)
                        .clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(16.dp))
                // Barra de progreso minimalista
                Box(
                    Modifier.fillMaxWidth(0.85f).height(3.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
                ) {
                    val progress = if (dur > 0) pos.toFloat() / dur else 0f
                    Box(
                        Modifier.fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.onBackground)
                    )
                }
            }

            // Mitad derecha: letra
            Box(Modifier.weight(1f).fillMaxHeight().padding(20.dp)) {
                if (syncedLines.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Sin letra disponible",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 180.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(syncedLines, key = { i, _ -> i }) { i, line ->
                            val active = i == currentIdx
                            Text(
                                line.text,
                                color = if (active) MaterialTheme.colorScheme.onBackground
                                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                                fontSize = if (active) 20.sp else 15.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // Overlay con botones (visible al tocar)
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            Box(
                Modifier.fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { PlayerHolder.prev() }) {
                        Icon(Icons.Filled.SkipPrevious, null,
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(48.dp))
                    }
                    IconButton(
                        onClick = { PlayerHolder.togglePlay() },
                        modifier = Modifier.size(80.dp).clip(CircleShape)
                            .background(androidx.compose.ui.graphics.Color.White)
                    ) {
                        Icon(
                            if (PlayerHolder.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            null,
                            tint = androidx.compose.ui.graphics.Color.Black,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    IconButton(onClick = { PlayerHolder.next() }) {
                        Icon(Icons.Filled.SkipNext, null,
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(48.dp))
                    }
                }
            }
        }

        // Botón X arriba a la izquierda (siempre visible)
        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
        ) {
            Icon(Icons.Filled.Close, "Salir",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(28.dp))
        }
    }
}
