package com.adrj.music

import android.app.Application
import android.os.Handler
import android.os.Looper
import com.adrj.music.data.Prefs
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AdrjApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        try { MyAppContext.set(this) } catch (_: Throwable) {}

        // FIX ARRANQUE: TODO diferido 1500ms después del primer frame.
        // La UI abre primero, y mientras tanto se inicializa el motor.
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val t = Thread {
                    try {
                        // 1. Init del motor (necesario para buscar)
                        YtdlpHelper.init(this)

                        // 2. Update SOLO si pasaron 24h desde el último (no en cada apertura)
                        val lastUpdate = Prefs.ytdlpLastUpdate(this)
                        val now = System.currentTimeMillis()
                        if (now - lastUpdate > 24 * 60 * 60 * 1000L) {
                            YtdlpHelper.updateInBackground(this)
                            Prefs.setYtdlpLastUpdate(this, now)
                        }

                        // 3. Servidor local (necesario para streaming)
                        try {
                            LocalServer(this, 8080).start(1000, false)
                        } catch (e: Throwable) {
                            android.util.Log.e("ADRJ", "LocalServer error", e)
                        }
                    } catch (e: Throwable) {
                        android.util.Log.e("ADRJ", "Init error", e)
                    }
                }
                t.priority = Thread.MIN_PRIORITY
                t.start()
            } catch (_: Throwable) {}
        }, 1500L) // Esperar a que la UI pinte el primer frame
    }

    override fun newImageLoader(): ImageLoader {
        return try {
            val dispatcher = Dispatcher().apply {
                maxRequestsPerHost = 6
                maxRequests = 12
            }
            val client = OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            ImageLoader.Builder(this)
                .okHttpClient(client)
                .memoryCache {
                    // 25% de RAM para caché de imágenes (balance memoria/velocidad)
                    MemoryCache.Builder(this)
                        .maxSizePercent(0.25)
                        .strongReferencesEnabled(false)
                        .build()
                }
                .diskCache {
                    // 80MB de caché en disco (antes 150MB, más liviano)
                    DiskCache.Builder()
                        .directory(cacheDir.resolve("image_cache"))
                        .maxSizeBytes(80L * 1024 * 1024)
                        .build()
                }
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .networkCachePolicy(CachePolicy.ENABLED)
                .crossfade(120)
                .respectCacheHeaders(false)
                .allowHardware(false)
                .build()
        } catch (e: Throwable) {
            ImageLoader.Builder(this).build()
        }
    }
}
