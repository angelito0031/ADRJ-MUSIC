package com.adrj.music.player

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.adrj.music.data.MusicRepository
import com.adrj.music.data.Song
import com.adrj.music.downloader.SearchResult
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import java.io.File

data class PlayingTrack(
    val id: String, val title: String, val artist: String,
    val thumbnailUrl: String, val isLocal: Boolean,
    val durationSec: Long = 0L
)

object PlayerHolder {
    var controller: MediaController? = null; private set
    var repository: MusicRepository? = null
    var currentTrack by mutableStateOf<PlayingTrack?>(null); private set
    var isPlaying by mutableStateOf(false); private set
    var position by mutableStateOf(0L); private set
    var duration by mutableStateOf(0L); private set
    var lastError by mutableStateOf<String?>(null)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private var future: ListenableFuture<MediaController>? = null
    private var ticking = false
    private var contextRef: Context? = null
    private var fetchingSimilar = false
    private var currentPlayJob: kotlinx.coroutines.Job? = null

    private fun updateDynamicAccent(thumbnailUrl: String) {
        if (thumbnailUrl.isBlank()) return
        if (!com.adrj.music.data.PrefsState.dynamicColor) return
        val ctx = contextRef ?: return
        scope.launch(Dispatchers.IO) {
            val c = com.adrj.music.ui.theme.rememberDominantColorStatic(ctx, thumbnailUrl)
            if (c != null) {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    com.adrj.music.ui.theme.DynamicAccent.color = c
                }
            }
        }
    }

    // LISTENER GUARDADO PARA PODER REMOVERLO
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            isPlaying = playing
            if (playing) startTick() else stopTick()
        }
        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_READY) duration = controller?.duration?.coerceAtLeast(0) ?: 0
            if (state == Player.STATE_ENDED) {
                try {
                    if (controller?.repeatMode == Player.REPEAT_MODE_OFF) {
                        fetchAndAppendSimilar(controller, controller?.currentMediaItem)
                    }
                } catch (e: Throwable) { android.util.Log.e("ADRJ", "autoplay err", e) }
            }
        }
        override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
            item?.let {
                val id = it.mediaId
                val meta = it.mediaMetadata
                if (id.isNotBlank()) {
                    val t = meta.title?.toString() ?: ""
                    val a = meta.artist?.toString() ?: ""
                    val art = meta.artworkUri?.toString() ?: ""
                    scope.launch(Dispatchers.IO) {
                        try { repository?.recordPlay(id, t, a, art) } catch (_: Throwable) {}
                    }
                }
                val local = controller?.currentMediaItem?.localConfiguration?.uri?.scheme == "file"
                currentTrack = PlayingTrack(
                    id = id,
                    title = meta.title?.toString() ?: "",
                    artist = meta.artist?.toString() ?: "",
                    thumbnailUrl = meta.artworkUri?.toString() ?: "",
                    isLocal = local,
                    durationSec = (meta.extras?.getLong("durationSec")) ?: 0L
                )
            }
            item?.mediaMetadata?.artworkUri?.let { uri ->
                updateDynamicAccent(uri.toString())
            }
        }
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            lastError = "Player: ${error.errorCodeName}"
        }
    }

    fun init(context: Context) {
        contextRef = context.applicationContext
        if (controller != null) return
        val token = SessionToken(context, ComponentName(context, PlayerService::class.java))
        val f = MediaController.Builder(context, token).buildAsync()
        future = f
        f.addListener({
            controller = f.get().also { c ->
                c.repeatMode = Player.REPEAT_MODE_OFF
                c.addListener(playerListener)
            }
        }, MoreExecutors.directExecutor())
    }

    private val tickRunnable = object : Runnable {
        override fun run() {
            val c = controller ?: return
            position = c.currentPosition
            duration = c.duration.coerceAtLeast(0)
            if (ticking) handler.postDelayed(this, 500)
        }
    }
    private fun startTick() { if (!ticking) { ticking = true; handler.post(tickRunnable) } }
    private fun stopTick() { ticking = false; handler.removeCallbacks(tickRunnable) }

    private fun songToItem(song: Song) = MediaItem.Builder()
        .setUri(File(song.filePath).toURI().toString())
        .setMediaId(song.id)
        .setMediaMetadata(MediaMetadata.Builder()
            .setTitle(song.title).setArtist(song.artist)
            .setArtworkUri(android.net.Uri.parse(song.thumbnailUrl))
            .setExtras(android.os.Bundle().apply { putLong("durationSec", song.duration) })
            .build())
        .build()

    fun playFromList(songs: List<Song>, startIndex: Int) {
        ensureInit()
        val c = controller ?: return
        if (songs.isEmpty()) return
        val s = songs[startIndex]
        // FIX BUG 1: cancelar el job anterior
        currentPlayJob?.cancel()
        try { c.stop() } catch (_: Throwable) {}
        currentTrack = PlayingTrack(s.id, s.title, s.artist, s.thumbnailUrl, true, s.duration)
        duration = s.duration * 1000
        currentPlayJob = scope.launch(Dispatchers.IO) {
            try {
                val items = songs.map { songToItem(it) }
                if (!isActive) return@launch
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    if (!isActive) return@withContext
                    c.setMediaItems(items, startIndex, 0)
                    c.prepare(); c.play()
                    updateDynamicAccent(s.thumbnailUrl)
                }
            } catch (_: kotlinx.coroutines.CancellationException) {
            } catch (e: Throwable) {
                kotlinx.coroutines.withContext(Dispatchers.Main) { lastError = "Error: ${e.message}" }
            }
        }
    }

    fun playDownloaded(song: Song) { playFromList(listOf(song), 0) }

    fun playStreaming(results: List<SearchResult>, startIndex: Int) {
        ensureInit()
        val c = controller ?: return
        val r = results[startIndex]
        // FIX BUG 1: cancelar el job anterior para que no compita
        currentPlayJob?.cancel()
        // Detener reproducción actual inmediatamente
        try { c.stop() } catch (_: Throwable) {}
        // Actualizar UI inmediatamente
        currentTrack = PlayingTrack(r.id, r.title, r.artist, r.thumbnailUrl, false, r.duration)
        duration = r.duration * 1000
        lastError = "Resolviendo..."
        currentPlayJob = scope.launch(Dispatchers.IO) {
            try {
                val ctx = contextRef ?: return@launch
                com.adrj.music.YtdlpHelper.ensureInit(ctx)
                val streamUrl = com.adrj.music.YtdlpHelper.resolveStreamUrl(r.id) ?: run {
                    kotlinx.coroutines.withContext(Dispatchers.Main) { lastError = "No se pudo obtener URL" }
                    return@launch
                }
                if (!isActive) return@launch
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    if (!isActive) return@withContext
                    lastError = null
                    val item = MediaItem.Builder()
                        .setUri(streamUrl).setMediaId(r.id)
                        .setMediaMetadata(MediaMetadata.Builder()
                            .setTitle(r.title).setArtist(r.artist)
                            .setArtworkUri(android.net.Uri.parse(r.thumbnailUrl))
                            .setExtras(android.os.Bundle().apply { putLong("durationSec", r.duration) })
                            .build())
                        .build()
                    c.setMediaItem(item); c.prepare(); c.play()
                    // FIX BUG 3: forzar actualización del color dinámico
                    updateDynamicAccent(r.thumbnailUrl)
                }
            } catch (_: kotlinx.coroutines.CancellationException) {
            } catch (e: Throwable) {
                kotlinx.coroutines.withContext(Dispatchers.Main) { lastError = "Error: ${e.message}" }
            }
        }
    }

    fun togglePlay() { ensureInit(); controller?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun next() { ensureInit(); controller?.seekToNextMediaItem() }
    fun prev() { ensureInit(); controller?.seekToPreviousMediaItem() }
    fun seekTo(ms: Long) { ensureInit(); controller?.seekTo(ms) }

    private var initCalled = false
    private fun ensureInit() {
        if (initCalled) return
        val ctx = contextRef ?: com.adrj.music.MyAppContext.get() ?: return
        initCalled = true
        init(ctx)
    }

    fun release() {
        stopTick()
        controller?.removeListener(playerListener) // FIX: remover listener
        future?.let { MediaController.releaseFuture(it) }
        controller = null
    }

    fun setShuffle(on: Boolean) { controller?.shuffleModeEnabled = on }
    fun toggleShuffle() { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }

    fun setRepeat(mode: Int) {
        controller?.repeatMode = when (mode) {
            1 -> Player.REPEAT_MODE_ALL
            2 -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    private fun fetchAndAppendSimilar(c: MediaController?, lastItem: MediaItem?) {
        if (fetchingSimilar || c == null) return
        fetchingSimilar = true
        val meta = lastItem?.mediaMetadata
        val artist = meta?.artist?.toString() ?: ""
        val title = meta?.title?.toString() ?: ""
        scope.launch(Dispatchers.IO) {
            try {
                val query = if (artist.isNotBlank() && artist != "Desconocido") "$artist canciones"
                else if (title.isNotBlank()) "$title radio"
                else { fetchingSimilar = false; return@launch }

                val results = com.adrj.music.downloader.YtDlpWrapper.searchMix(query, 15)
                if (results.isEmpty()) { fetchingSimilar = false; return@launch }

                val existingIds = mutableSetOf<String>()
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    for (i in 0 until c.mediaItemCount) {
                        val id = c.getMediaItemAt(i).mediaId
                        if (id.isNotBlank()) existingIds.add(id)
                    }
                }
                val newOnes = results.filter { it.id !in existingIds }
                if (newOnes.isEmpty()) { fetchingSimilar = false; return@launch }

                val newItems = mutableListOf<MediaItem>()
                for (r in newOnes) {
                    try {
                        val streamUrl = com.adrj.music.YtdlpHelper.resolveStreamUrl(r.id)
                        if (streamUrl.isNullOrBlank()) continue
                        newItems.add(MediaItem.Builder().setUri(streamUrl).setMediaId(r.id)
                            .setMediaMetadata(MediaMetadata.Builder()
                                .setTitle(r.title).setArtist(r.artist)
                                .setArtworkUri(android.net.Uri.parse(r.thumbnailUrl))
                                .build())
                            .build())
                    } catch (_: Throwable) {}
                }
                if (newItems.isEmpty()) { fetchingSimilar = false; return@launch }

                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    c.addMediaItems(newItems)
                    if (!c.isPlaying && c.playbackState == Player.STATE_ENDED) {
                        c.seekTo(c.mediaItemCount - newItems.size, 0)
                        c.play()
                    }
                }
            } catch (_: Throwable) {} finally {
                fetchingSimilar = false
            }
        }
    }
}
