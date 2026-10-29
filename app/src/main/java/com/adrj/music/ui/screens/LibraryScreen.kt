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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adrj.music.player.PlayerHolder
import com.adrj.music.ui.components.SongRow
import com.adrj.music.ui.components.iosGlass
import com.adrj.music.ui.theme.GrisClaro

enum class SortMode(val label: String) {
    RECENT("Recientes"),
    NAME("Nombre"),
    ARTIST("Artista")
}

@Composable
fun LibraryScreen(vm: LibraryViewModel = viewModel()) {
    val songs by vm.songs.collectAsState()
    val favs by vm.favorites.collectAsState()
    var tab by remember { mutableStateOf(0) }
    var showPlaylists by remember { mutableStateOf(false) }
    var showCache by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showUrl by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(SortMode.RECENT) }
    var showSortMenu by remember { mutableStateOf(false) }

    if (showPlaylists) { PlaylistScreen(onBack = { showPlaylists = false }); return }
    if (showCache) { CacheScreen(onBack = { showCache = false }); return }
    if (showHistory) { HistoryScreen(onBack = { showHistory = false }); return }
    if (showUrl) { UrlDownloadScreen(onBack = { showUrl = false }); return }

    // FIX BUG 1: remember para no recalcular filter/sort en cada frame
    val list = remember(songs, favs, tab, searchQuery, sortMode) {
        val baseList = if (tab == 0) songs else favs
        val filtered = if (searchQuery.isBlank()) baseList
        else baseList.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true)
        }
        when (sortMode) {
            SortMode.RECENT -> filtered.sortedByDescending { it.downloadedAt }
            SortMode.NAME -> filtered.sortedBy { it.title.lowercase() }
            SortMode.ARTIST -> filtered.sortedBy { it.artist.lowercase() }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
            Text("Biblioteca", style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickTile(
                    icon = Icons.Filled.QueueMusic,
                    title = "Playlists",
                    onClick = { showPlaylists = true },
                    modifier = Modifier.weight(1f)
                )
                QuickTile(
                    icon = Icons.Filled.CloudDownload,
                    title = "En caché",
                    onClick = { showCache = true },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickTile(
                    icon = Icons.Filled.History,
                    title = "Historial",
                    onClick = { showHistory = true },
                    modifier = Modifier.weight(1f)
                )
                QuickTile(
                    icon = Icons.Filled.Link,
                    title = "Por URL",
                    onClick = { showUrl = true },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(16.dp))

            // Barra de búsqueda + ordenar + aleatorio
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Buscar...", color = GrisClaro) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    leadingIcon = {
                        Icon(Icons.Filled.Search, null, tint = GrisClaro)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, null, tint = GrisClaro)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
                // Botón ordenar
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Filled.Sort, "Ordenar",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        SortMode.values().forEach { mode ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (sortMode == mode) {
                                            Icon(Icons.Filled.Check, null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                        } else {
                                            Spacer(Modifier.width(26.dp))
                                        }
                                        Text(mode.label)
                                    }
                                },
                                onClick = {
                                    sortMode = mode
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
                // Botón aleatorio
                IconButton(onClick = {
                    if (list.isNotEmpty()) {
                        val idx = list.indices.random()
                        PlayerHolder.playFromList(list, idx)
                    }
                }) {
                    Icon(Icons.Filled.Shuffle, "Aleatorio",
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(12.dp))

            TabRow(selectedTabIndex = tab,
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Todas") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Favoritos") })
            }

            if (searchQuery.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("${list.size} resultados para \"$searchQuery\"",
                    color = GrisClaro,
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
        LazyColumn(Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 160.dp)) {
            if (list.isEmpty()) {
                item {
                    Text(
                        when {
                            searchQuery.isNotBlank() -> "Sin resultados"
                            tab == 0 -> "Sin canciones"
                            else -> "Sin favoritos"
                        },
                        color = GrisClaro, modifier = Modifier.padding(20.dp)
                    )
                }
            }
            itemsIndexed(list, key = { _, s -> s.id }, contentType = { _, _ -> "song" }) { index, s ->
                SongRow(s, { PlayerHolder.playFromList(list, index) },
                    { vm.toggleFavorite(s) }, { vm.delete(s) })
            }
        }
    }
}

@Composable
private fun QuickTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .iosGlass(24)
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(10.dp))
        Text(title, color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium)
    }
}
