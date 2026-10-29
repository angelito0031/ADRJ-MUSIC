package com.adrj.music.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.adrj.music.downloader.YtDlpWrapper

class PlayerService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

    override fun onCreate() {
        super.onCreate()
        createChannel()

        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(YtDlpWrapper.USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(60000)

        val universal = DefaultDataSource.Factory(this, httpFactory)
        val mediaFactory = DefaultMediaSourceFactory(universal)

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaFactory)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        // Inicializar ecualizador con el sessionId del ExoPlayer real
        try {
            val sessionId = player.audioSessionId
            android.util.Log.d("ADRJ-EQ", "ExoPlayer audioSessionId=$sessionId")
            if (sessionId != 0) {
                EqManager.init(sessionId)
                if (EqManager.enabled) EqManager.enable(true)
            }
        } catch (e: Exception) {
            android.util.Log.e("ADRJ-EQ", "init desde service fallo", e)
        }

        mediaSession = MediaSession.Builder(this, player)
            .setId("adrj_session")
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)

            val playback = NotificationChannel(
                "adrj_playback",
                "Reproductor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controles del reproductor"
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
                setSound(null, null)
            }
            nm.createNotificationChannel(playback)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        val p = mediaSession?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        mediaSession?.run { player.release(); release(); mediaSession = null }
        super.onDestroy()
    }
}
