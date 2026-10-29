package com.adrj.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.adrj.music.downloader.SearchResult
import com.adrj.music.ui.components.iosGlass
import com.adrj.music.player.PlayerHolder
import com.adrj.music.ui.theme.Gris
import com.adrj.music.ui.theme.GrisClaro
import com.adrj.music.ui.components.VoiceSearchButton

@Composable
fun SearchScreen(vm: SearchViewModel = viewModel()) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(20.dp))
        Text("Buscar", style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = vm.query, onValueChange = vm::onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Canción, artista...", color = GrisClaro) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    VoiceSearchButton { text ->
                        vm.onQueryChange(text)
                        vm.search()
                    }
                    IconButton(onClick = { vm.search() }) {
                        Icon(Icons.Filled.Search, "Buscar")
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Gris,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(Modifier.height(16.dp))
        if (vm.loading) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        vm.error?.let {
            Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp))
        }
        LazyColumn(Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 160.dp)) {
            itemsIndexed(vm.results, key = { _, r -> r.id }, contentType = { _, _ -> "result" }) { i, r ->
                ResultRow(r, r.id in vm.downloadingIds, r.id in vm.downloadedIds,
                    vm.progressFor(r.id),
                    { PlayerHolder.playStreaming(vm.results, i) },
                    { vm.download(r) })
            }
        }
    }
}

@Composable
private fun ResultRow(
    result: SearchResult, downloading: Boolean, downloaded: Boolean,
    progress: Float, onPlay: () -> Unit, onDownload: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth()
            .clickable { onPlay() }
            .iosGlass(24)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(result.thumbnailUrl, null,
            Modifier.size(56.dp).clip(RoundedCornerShape(24.dp)).background(Gris),
            contentScale = ContentScale.Crop)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(result.title, color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            Text(result.artist, color = GrisClaro,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (downloading) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = MaterialTheme.colorScheme.primary, trackColor = Gris)
            }
        }
        Spacer(Modifier.width(8.dp))
        when {
            downloaded -> Icon(Icons.Filled.Check, "Descargado",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(8.dp))
            downloading -> Text("${(progress * 100).toInt()}%", color = GrisClaro,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(8.dp))
            else -> IconButton(onClick = onDownload) {
                Icon(Icons.Filled.Download, "Descargar",
                    tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
