package com.adrj.music.ui.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.adrj.music.player.PlayerHolder
import com.adrj.music.ui.components.iosGlass
import com.adrj.music.ui.theme.Gris

@Composable
fun MiniPlayer(onExpand: () -> Unit) {
    val track = PlayerHolder.currentTrack
    val miniAlways = com.adrj.music.data.PrefsState.miniAlways

    if (track == null) {
        if (!miniAlways) return
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp)
                .iosGlass(24)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Sin reproducción",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    // OPTIMIZACIÓN: derivedStateOf evita recomposición por cambios mínimos de posición
    val progress by androidx.compose.runtime.derivedStateOf {
        if (PlayerHolder.duration > 0)
            PlayerHolder.position.toFloat() / PlayerHolder.duration else 0f
    }

    Column(
        Modifier.fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .iosGlass(24)
            .clickable { onExpand() }
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(track.thumbnailUrl, null,
                Modifier.size(46.dp).clip(CircleShape).background(Gris),
                contentScale = ContentScale.Crop)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold)
                Text(track.artist, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = { PlayerHolder.prev() }) {
                Icon(Icons.Filled.SkipPrevious, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { PlayerHolder.togglePlay() }) {
                Icon(
                    if (PlayerHolder.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    null, tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
            IconButton(onClick = { PlayerHolder.next() }) {
                Icon(Icons.Filled.SkipNext, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
