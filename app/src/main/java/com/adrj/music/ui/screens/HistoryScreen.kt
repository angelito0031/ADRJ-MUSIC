package com.adrj.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adrj.music.data.HistoryEntity
import com.adrj.music.data.MusicRepository
import com.adrj.music.downloader.SearchResult
import com.adrj.music.player.PlayerHolder
import com.adrj.music.ui.components.iosGlass
import com.adrj.music.ui.components.Cover
import com.adrj.music.ui.theme.GrisClaro
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { MusicRepository.create(ctx) }
    var items by remember { mutableStateOf<List<HistoryEntity>>(emptyList()) }

    LaunchedEffect(Unit) {
        repo.history.collectLatest { items = it }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Atrás",
                    tint = MaterialTheme.colorScheme.onBackground)
            }
            Text("Historial", style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground)
        }
        Text("${items.size} canciones", color = GrisClaro,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 56.dp))
        Spacer(Modifier.height(16.dp))

        if (items.isNotEmpty()) {
            Button(
                onClick = { scope.launch(Dispatchers.IO) { repo.clearHistory() } },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Icon(Icons.Filled.Delete, null)
                Spacer(Modifier.width(8.dp))
                Text("Borrar historial")
            }
            Spacer(Modifier.height(16.dp))
        }

        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.History, null, tint = GrisClaro,
                        modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Sin historial", color = GrisClaro)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 160.dp)) {
                items(items, key = { it.id + it.playedAt }) { item ->
                    Row(
                        Modifier.fillMaxWidth()
                            .iosGlass(14)
                            .clickable {
                                // FIX BUG 2: findSong en IO, play en Main
                                scope.launch {
                                    val local = withContext(Dispatchers.IO) { repo.findSong(item.id) }
                                    if (local != null) {
                                        PlayerHolder.playFromList(listOf(local), 0)
                                    } else {
                                        val sr = SearchResult(
                                            id = item.id,
                                            title = item.title,
                                            artist = item.artist,
                                            duration = 0L,
                                            thumbnailUrl = item.thumbnailUrl,
                                            url = "https://www.youtube.com/watch?v=${item.id}"
                                        )
                                        PlayerHolder.playStreaming(listOf(sr), 0)
                                    }
                                }
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Cover(url = item.thumbnailUrl, size = 52.dp, corner = 10.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title, color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold)
                            Text(item.artist, color = GrisClaro,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = {
                            scope.launch {
                                val local = withContext(Dispatchers.IO) { repo.findSong(item.id) }
                                if (local != null) {
                                    PlayerHolder.playFromList(listOf(local), 0)
                                } else {
                                    val sr = SearchResult(
                                        id = item.id,
                                        title = item.title,
                                        artist = item.artist,
                                        duration = 0L,
                                        thumbnailUrl = item.thumbnailUrl,
                                        url = "https://www.youtube.com/watch?v=${item.id}"
                                    )
                                    PlayerHolder.playStreaming(listOf(sr), 0)
                                }
                            }
                        }) {
                            Icon(Icons.Filled.PlayArrow, "Reproducir",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
