package com.adrj.music.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adrj.music.data.Song
import com.adrj.music.ui.components.iosGlass
import com.adrj.music.ui.theme.GrisClaro
import com.adrj.music.ui.components.frostedGlass

@Composable
fun SongRow(
    song: Song,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    var showAddPlaylist by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val baseShape = remember { RoundedCornerShape(24.dp) }

    // Animación de entrada escalonada
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val entranceAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(350),
        label = "entranceAlpha"
    )
    val entranceOffset by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 0f else 30f,
        animationSpec = androidx.compose.animation.core.tween(350),
        label = "entranceOffset"
    )

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(120), label = "rowScale"
    )
    val bg by animateColorAsState(
        targetValue = if (pressed) MaterialTheme.colorScheme.surfaceVariant
        else MaterialTheme.colorScheme.surface,
        animationSpec = tween(180), label = "rowBg"
    )

    Box(
        Modifier
            .fillMaxWidth()
            .alpha(entranceAlpha)
            .offset(y = entranceOffset.dp)
            .scale(scale)
            .iosGlass(24)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
    ) {
Row(
            Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        Cover(url = song.thumbnailUrl, size = 56.dp, corner = 12.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(3.dp))
            Text(
                song.artist,
                color = GrisClaro,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onFavorite) {
            val favColor by animateColorAsState(
                targetValue = if (song.favorite) Color(0xFFE53935) else GrisClaro,
                animationSpec = tween(250), label = "favColor"
            )
            val favScale by animateFloatAsState(
                targetValue = if (song.favorite) 1.15f else 1f,
                animationSpec = tween(250), label = "favScale"
            )
            Icon(
                if (song.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "Favorito",
                tint = favColor,
                modifier = Modifier.scale(favScale)
            )
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Filled.MoreVert, "Más", tint = GrisClaro)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Añadir a playlist") },
                    onClick = { menu = false; showAddPlaylist = true }
                )
                DropdownMenuItem(
                    text = { Text("Compartir") },
                    onClick = { menu = false; shareSong(song) }
                )
                DropdownMenuItem(text = { Text("Eliminar") }, onClick = {
                    menu = false; onDelete()
                })
            }
        }
        }
    }

    if (showAddPlaylist) {
        AddToPlaylistDialog(song = song, onDismiss = { showAddPlaylist = false })
    }
}

private fun shareSong(song: Song) {
    val ctx = com.adrj.music.MyAppContext.get() ?: return
    val url = "https://www.youtube.com/watch?v=${song.id}"
    val text = "${song.title} - ${song.artist}\n$url"
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, text)
        putExtra(android.content.Intent.EXTRA_SUBJECT, song.title)
    }
    val chooser = android.content.Intent.createChooser(intent, "Compartir").apply {
        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    ctx.startActivity(chooser)
}
