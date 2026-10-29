package com.adrj.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.adrj.music.downloader.SearchResult
import com.adrj.music.player.PlayerHolder
import com.adrj.music.ui.components.SongRow
import com.adrj.music.ui.components.iosGlass
import com.adrj.music.ui.theme.Gris
import com.adrj.music.ui.theme.GrisClaro

@Composable
fun HomeScreen(vm: HomeViewModel = viewModel()) {
    val songs by vm.songs.collectAsState()
    // FIX BUG 3: remember para no recalcular take(10) en cada frame
    val recientes = remember(songs) { songs.take(10) }
    // FIX indexOf: Map songId -> index (O(1) en click, evita recorrer toda la lista)
    val songsIndexMap = remember(songs) { songs.mapIndexed { i, s -> s.id to i }.toMap() }
    var showMoodScreen by remember { mutableStateOf(false) }
    // FIX BUG 4: re-cargar cuando se vacían (ej: descargaste todas)
    LaunchedEffect(vm.recommended.isEmpty()) { vm.loadRecommendations() }

    if (showMoodScreen) {
        com.adrj.music.ui.screens.MoodScreen(
            onMoodSelected = { mood ->
                showMoodScreen = false
                vm.searchByMood(mood.query)
            },
            onArtistMoodSelected = { artist, mood ->
                showMoodScreen = false
                val q = if (mood != null) {
                    "$artist ${mood.query}"
                } else {
                    "$artist canciones"
                }
                vm.searchByMood(q)
            },
            onBack = { showMoodScreen = false }
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 20.dp, bottom = 160.dp)
    ) {
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("Inicio", style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(4.dp))
                Text(if (songs.isEmpty()) "Busca y descarga tu primera canción"
                     else "${songs.size} canciones en biblioteca", color = GrisClaro)
            }
            Spacer(Modifier.height(16.dp))

            // Botón estado de ánimo
            Row(
                Modifier.fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .iosGlass(24)
                    .clickable { showMoodScreen = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Mood, "Estado de ánimo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Estado de ánimo",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold)
                    Text("Música según cómo te sientes",
                        color = GrisClaro,
                        style = MaterialTheme.typography.bodyMedium)
                }
                Icon(Icons.Filled.ChevronRight, null,
                    tint = GrisClaro)
            }
            Spacer(Modifier.height(24.dp))
        }

        // Carrusel grande de recientes
        if (recientes.isNotEmpty()) {
            item {
                Text("Recientes", style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(12.dp))
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(recientes, key = { it.id }, contentType = { "carousel" }) { song ->
                        BigCoverCard(
                            song = song,
                            onClick = { PlayerHolder.playFromList(songs, songsIndexMap[song.id] ?: 0) },
                            onFavorite = { vm.toggleFavorite(song) }
                        )
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
        }

        item {
            Text(vm.title, style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(12.dp))
        }

        if (vm.loading) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        } else if (vm.recommended.isNotEmpty()) {
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(vm.recommended, key = { it.id }, contentType = { "rec" }) { r ->
                        RecommendedCard(r, r.id in vm.downloadingIds) { vm.download(r) }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun BigCoverCard(
    song: com.adrj.music.data.Song,
    onClick: () -> Unit,
    onFavorite: () -> Unit
) {
    Column(
        Modifier.width(180.dp).clickable { onClick() }
    ) {
        Box(
            Modifier.size(180.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Gris)
        ) {
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            IconButton(
                onClick = onFavorite,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                    .size(32.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f))
            ) {
                Icon(
                    if (song.favorite) Icons.Filled.Favorite
                    else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint = if (song.favorite)
                        androidx.compose.ui.graphics.Color(0xFFE53935)
                    else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            song.title,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            song.artist,
            color = GrisClaro,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun RecommendedCard(r: SearchResult, downloading: Boolean, onDownload: () -> Unit) {
    Column(Modifier.width(150.dp)) {
        Box(Modifier.size(150.dp).clip(RoundedCornerShape(24.dp)).background(Gris)) {
            AsyncImage(r.thumbnailUrl, null, Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop)
            if (downloading) {
                Box(Modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            IconButton(onClick = onDownload,
                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                    .size(34.dp).clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primary)) {
                Icon(Icons.Filled.Download, null, tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(r.title, color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
        Text(r.artist, color = GrisClaro, maxLines = 1,
            overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
    }
}
