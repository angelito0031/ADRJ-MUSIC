package com.adrj.music.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

// Caché en memoria: url -> Color. Evita recalcular el mismo color.
private val colorCache = ConcurrentHashMap<String, Color>()

// ImageLoader único (evita crear uno nuevo en cada llamada, mucho más rápido)
private var sharedLoader: ImageLoader? = null
private fun getLoader(ctx: Context): ImageLoader {
    return sharedLoader ?: ImageLoader(ctx.applicationContext).also { sharedLoader = it }
}

// Extrae color dominante rápido con caché
private suspend fun extractDominant(context: Context, url: String, fallback: Color): Color {
    // 1. Caché hit → instantáneo
    colorCache[url]?.let { return it }

    return withContext(Dispatchers.IO) {
        try {
            val loader = getLoader(context)
            val req = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false)
                .size(48) // Un poco más chico, más rápido
                .build()
            val result = loader.execute(req)
            val bmp = (result as? SuccessResult)?.drawable?.let {
                if (it is BitmapDrawable) it.bitmap else null
            } ?: return@withContext fallback
            
            val color = averageColorFast(bmp)
            colorCache[url] = color // Guardar en caché
            color
        } catch (_: Exception) { fallback }
    }
}

// Cálculo rápido usando IntArray (10x más rápido que getPixel)
private fun averageColorFast(bmp: Bitmap): Color {
    val w = bmp.width
    val h = bmp.height
    if (w == 0 || h == 0) return Color(0xFF444444)
    val pixels = IntArray(w * h)
    bmp.getPixels(pixels, 0, w, 0, 0, w, h)

    var r = 0L; var g = 0L; var b = 0L; var count = 0L
    // Analizar 1 de cada 16 píxeles (suficiente para un promedio)
    for (i in pixels.indices step 16) {
        val c = pixels[i]
        val rr = (c shr 16) and 0xFF
        val gg = (c shr 8) and 0xFF
        val bb = c and 0xFF
        val brightness = (rr + gg + bb) / 3
        if (brightness in 30..225) {
            r += rr; g += gg; b += bb; count++
        }
    }
    if (count == 0L) return Color(0xFF444444)
    val fr = (r / count).toInt().coerceIn(40, 220)
    val fg = (g / count).toInt().coerceIn(40, 220)
    val fb = (b / count).toInt().coerceIn(40, 220)
    return Color(0xFF000000.toInt() or (fr shl 16) or (fg shl 8) or fb)
}

@Composable
fun rememberDominantColor(url: String?, fallback: Color): Color {
    val ctx = LocalContext.current
    var color by remember(url) { mutableStateOf(fallback) }
    LaunchedEffect(url) {
        color = if (!url.isNullOrBlank()) extractDominant(ctx, url, fallback) else fallback
    }
    return color
}

suspend fun rememberDominantColorStatic(context: Context, url: String): Color? =
    try { extractDominant(context, url, Color(0xFF444444)) } catch (_: Exception) { null }
