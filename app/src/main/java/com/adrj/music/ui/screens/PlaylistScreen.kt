package com.adrj.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.unit.dp
import com.adrj.music.data.MusicRepository
import com.adrj.music.data.Prefs
import com.adrj.music.data.Song
import com.adrj.music.player.PlayerHolder
import com.adrj.music.ui.components.iosGlass
import com.adrj.music.ui.components.SongRow
import com.adrj.music.ui.theme.GrisClaro
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

@Composable
fun PlaylistScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { MusicRepository.create(ctx) }

    var playlists by remember { mutableStateOf(Prefs.getPlaylists(ctx)) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var openPlaylist by remember { mutableStateOf<String?>(null) }
    var allSongs by remember { mutableStateOf<List<Song>>(emptyList()) }

    LaunchedEffect(Unit) {
        allSongs = repo.all.first()
    }

    // Vista de playlist abierta
    openPlaylist?.let { name ->
        val ids = playlists[name] ?: emptyList()
        val tracks = ids.mapNotNull { id -> allSongs.find { it.id == id } }
        PlaylistDetail(
            name = name,
            songs = tracks,
            onBack = { openPlaylist = null },
            onRemove = { songId ->
                val updated = playlists.toMutableMap()
                updated[name] = (updated[name] ?: emptyList()).filter { it != songId }
                Prefs.savePlaylists(ctx, updated)
                playlists = updated
            }
        )
        return
    }

    // Lista de playlists
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Atrás",
                    tint = MaterialTheme.colorScheme.onBackground)
            }
            Text("Playlists", style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground)
        }
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { showCreateDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(Icons.Filled.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Nueva playlist")
        }

        Spacer(Modifier.height(16.dp))

        if (playlists.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Sin playlists", color = GrisClaro)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(playlists.keys.toList()) { name ->
                    val count = playlists[name]?.size ?: 0
                    Row(
                        Modifier.fillMaxWidth()
                            .iosGlass(16)
                            .clickable { openPlaylist = name }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.QueueMusic, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name, color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold)
                            Text("$count canciones", color = GrisClaro,
                                style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = {
                            val updated = playlists.toMutableMap()
                            updated.remove(name)
                            Prefs.savePlaylists(ctx, updated)
                            playlists = updated
                        }) {
                            Icon(Icons.Filled.Delete, "Eliminar", tint = GrisClaro)
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Nueva playlist") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("Nombre") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        val updated = playlists.toMutableMap()
                        updated[newName.trim()] = emptyList()
                        Prefs.savePlaylists(ctx, updated)
                        playlists = updated
                        newName = ""
                        showCreateDialog = false
                    }
                }) { Text("Crear") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun PlaylistDetail(
    name: String,
    songs: List<Song>,
    onBack: () -> Unit,
    onRemove: (String) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Atrás",
                    tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(name, style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground)
        }
        Spacer(Modifier.height(16.dp))

        if (songs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Sin canciones en esta playlist", color = GrisClaro)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 160.dp)) {
                itemsIndexed(songs, key = { _, s -> s.id }) { index, s ->
                    SongRow(
                        song = s,
                        onClick = { PlayerHolder.playFromList(songs, index) },
                        onFavorite = {},
                        onDelete = { onRemove(s.id) }
                    )
                }
            }
        }
    }
}
