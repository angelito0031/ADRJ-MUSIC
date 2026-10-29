package com.adrj.music.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adrj.music.data.MusicRepository
import com.adrj.music.downloader.SearchResult
import com.adrj.music.downloader.YtDlpWrapper
import com.adrj.music.ui.theme.GrisClaro
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun UrlDownloadScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { MusicRepository.create(ctx) }

    var url by remember { mutableStateOf("") }
    var downloading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Atrás",
                    tint = MaterialTheme.colorScheme.onBackground)
            }
            Text("Descargar por URL", style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground)
        }
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("https://youtube.com/watch?v=...") },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            leadingIcon = { Icon(Icons.Filled.Link, null) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                if (url.isBlank()) return@Button
                scope.launch {
                    downloading = true
                    status = "Obteniendo información..."
                    try {
                        val id = Regex("[?&]v=([A-Za-z0-9_-]{11})").find(url)?.groupValues?.get(1)
                            ?: Regex("youtu\\.be/([A-Za-z0-9_-]{11})").find(url)?.groupValues?.get(1)
                            ?: url.trim()
                        val result = SearchResult(
                            id = id,
                            title = "Descarga directa",
                            artist = "YouTube",
                            duration = 0L,
                            thumbnailUrl = "https://i.ytimg.com/vi/$id/maxresdefault.jpg",
                            url = "https://www.youtube.com/watch?v=$id"
                        )
                        status = "Descargando..."
                        val song = withContext(Dispatchers.IO) {
                            YtDlpWrapper.download(ctx, result) { p ->
                                status = "Descargando... ${(p * 100).toInt()}%"
                            }
                        }
                        withContext(Dispatchers.IO) { repo.save(song) }
                        status = "✓ Descargada: ${song.title}"
                        url = ""
                    } catch (e: Exception) {
                        status = "Error: ${e.message}"
                    }
                    downloading = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !downloading && url.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (downloading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(Icons.Filled.Download, null)
            }
            Spacer(Modifier.width(8.dp))
            Text(if (downloading) "Descargando..." else "Descargar")
        }

        status?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = GrisClaro)
        }
    }
}
