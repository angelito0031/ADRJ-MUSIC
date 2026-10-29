package com.adrj.music.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.adrj.music.data.Prefs
import com.adrj.music.data.Song

@Composable
fun AddToPlaylistDialog(
    song: Song,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    var playlists by remember { mutableStateOf(Prefs.getPlaylists(ctx)) }
    var newName by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir a playlist") },
        text = {
            Column {
                if (creating) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        placeholder = { Text("Nombre nueva playlist") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Row(
                        Modifier.fillMaxWidth().clickable { creating = true }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Add, null,
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text("Crear nueva")
                    }
                    if (playlists.isEmpty()) {
                        Text("Sin playlists aún", style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp))
                    } else {
                        LazyColumn(Modifier.heightIn(max = 300.dp)) {
                            items(playlists.keys.toList()) { name ->
                                Row(
                                    Modifier.fillMaxWidth().clickable {
                                        val updated = playlists.toMutableMap()
                                        val current = updated[name] ?: emptyList()
                                        if (song.id !in current) {
                                            updated[name] = current + song.id
                                            Prefs.savePlaylists(ctx, updated)
                                        }
                                        onDismiss()
                                    }.padding(12.dp)
                                ) {
                                    Text(name)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (creating) {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        val updated = playlists.toMutableMap()
                        updated[newName.trim()] = listOf(song.id)
                        Prefs.savePlaylists(ctx, updated)
                        onDismiss()
                    }
                }) { Text("Crear y añadir") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}
