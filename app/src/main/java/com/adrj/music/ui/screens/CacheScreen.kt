package com.adrj.music.ui.screens

import androidx.compose.foundation.background
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
import com.adrj.music.data.MusicRepository
import com.adrj.music.data.Song
import com.adrj.music.ui.components.iosGlass
import com.adrj.music.ui.components.Cover
import com.adrj.music.ui.theme.GrisClaro
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

data class CacheItem(val id: String, val file: File, val size: Long)

@Composable
fun CacheScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { MusicRepository.create(ctx) }

    var downloadedSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var cacheItems by remember { mutableStateOf<List<CacheItem>>(emptyList()) }
    var total by remember { mutableStateOf(0L) }

    fun load() {
        scope.launch(Dispatchers.IO) {
            val songs = withContext(Dispatchers.IO) {
                // FIX BUG 1: first() en lugar de collect (que nunca terminaba)
                repo.all.first()
            }
            downloadedSongs = songs
            val dir = File(ctx.cacheDir, "adrj_audio")
            val items = withContext(Dispatchers.IO) {
                dir.listFiles()?.filter { it.isFile }?.map {
                    CacheItem(it.name.removeSuffix(".m4a"), it, it.length())
                } ?: emptyList()
            }
            cacheItems = items
            total = items.sumOf { it.size }
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Atrás",
                    tint = MaterialTheme.colorScheme.onBackground)
            }
            Text("Descargas y caché", style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground)
        }
        Text("${downloadedSongs.size} descargadas · caché ${"%.1f".format(total / (1024f * 1024f))} MB",
            color = GrisClaro, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 56.dp))
        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    withContext(Dispatchers.IO) {
                        File(ctx.cacheDir, "adrj_audio").listFiles()?.forEach { it.delete() }
                    }
                    load()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        ) {
            Icon(Icons.Filled.Delete, null)
            Spacer(Modifier.width(8.dp))
            Text("Borrar caché temporal")
        }
        Spacer(Modifier.height(16.dp))

        Text("Descargadas", style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))

        if (downloadedSongs.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                Text("Sin descargas", color = GrisClaro)
            }
        } else {
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(downloadedSongs, key = { it.id }, contentType = { "song" }) { song ->
                    Row(
                        Modifier.fillMaxWidth()
                            .iosGlass(14)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Cover(url = song.thumbnailUrl, size = 52.dp, corner = 10.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(song.title, color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold)
                            Text(song.artist, color = GrisClaro,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = {
                            scope.launch(Dispatchers.IO) {
                                repo.toggleFavorite(song.id, !song.favorite)
                                load()
                            }
                        }) {
                            Icon(
                                if (song.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                "Favorito",
                                tint = if (song.favorite) androidx.compose.ui.graphics.Color(0xFFE53935) else GrisClaro
                            )
                        }
                        IconButton(onClick = {
                            scope.launch(Dispatchers.IO) {
                                withContext(Dispatchers.IO) {
                                    File(song.filePath).delete()
                                    repo.delete(song)
                                }
                                load()
                            }
                        }) {
                            Icon(Icons.Filled.Delete, "Eliminar", tint = GrisClaro)
                        }
                    }
                }
            }
        }
    }
}
