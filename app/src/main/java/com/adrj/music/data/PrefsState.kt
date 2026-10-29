package com.adrj.music.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.adrj.music.player.PlayerHolder

object PrefsState {
    var dark by mutableStateOf(true)
    var accent by mutableStateOf("Blanco")
    var fontScale by mutableStateOf(1f)
    var glassEnabled by mutableStateOf(true)
    var dynamicColor by mutableStateOf(false)
    var blurBg by mutableStateOf(true)
    var miniAlways by mutableStateOf(true)
    var animations by mutableStateOf(true)
    var reduceMotion by mutableStateOf(false)
    var autoplay by mutableStateOf(true)
    var shuffle by mutableStateOf(false)
    var repeatMode by mutableStateOf(0)
    var crossfade by mutableStateOf(true)
    var imageCache by mutableStateOf(true)
    var quality by mutableStateOf("Alta (m4a 128k)")
    var wifiOnly by mutableStateOf(false)
    var landscapeMode by mutableStateOf(false)
    var loaded = false

    fun load(ctx: Context) {
        if (loaded) return
        dark = Prefs.isDark(ctx)
        accent = Prefs.accent(ctx)
        fontScale = Prefs.fontScale(ctx)
        glassEnabled = Prefs.glassEnabled(ctx)
        dynamicColor = Prefs.dynamicColor(ctx)
        blurBg = Prefs.blurBackground(ctx)
        miniAlways = Prefs.showMiniPlayerAlways(ctx)
        animations = Prefs.animations(ctx)
        reduceMotion = Prefs.reduceMotion(ctx)
        autoplay = Prefs.autoplay(ctx)
        shuffle = Prefs.shuffle(ctx)
        repeatMode = Prefs.repeatMode(ctx)
        crossfade = Prefs.crossfadeEnabled(ctx)
        imageCache = Prefs.imageCacheEnabled(ctx)
        quality = Prefs.quality(ctx)
        wifiOnly = Prefs.downloadOverWifiOnly(ctx)
        loaded = true
    }

    fun setDark(c: Context, v: Boolean) { dark = v; Prefs.setDark(c, v) }
    fun setAccent(c: Context, v: String) { accent = v; Prefs.setAccent(c, v) }
    fun setFontScale(c: Context, v: Float) { fontScale = v; Prefs.setFontScale(c, v) }
    fun setGlassEnabled(c: Context, v: Boolean) { glassEnabled = v; Prefs.setGlassEnabled(c, v) }
    fun setDynamicColor(c: Context, v: Boolean) { dynamicColor = v; Prefs.setDynamicColor(c, v) }
    fun setBlurBg(c: Context, v: Boolean) { blurBg = v; Prefs.setBlurBackground(c, v) }
    fun setMiniAlways(c: Context, v: Boolean) { miniAlways = v; Prefs.setShowMiniPlayerAlways(c, v) }
    fun setAnimations(c: Context, v: Boolean) { animations = v; Prefs.setAnimations(c, v) }
    fun setReduceMotion(c: Context, v: Boolean) { reduceMotion = v; Prefs.setReduceMotion(c, v) }
    fun setAutoplay(c: Context, v: Boolean) { autoplay = v; Prefs.setAutoplay(c, v) }
    fun setShuffle(c: Context, v: Boolean) {
        shuffle = v; Prefs.setShuffle(c, v); PlayerHolder.setShuffle(v)
    }
    fun setRepeat(c: Context, v: Int) {
        repeatMode = v; Prefs.setRepeatMode(c, v); PlayerHolder.setRepeat(v)
    }
    fun setCrossfade(c: Context, v: Boolean) { crossfade = v; Prefs.setCrossfadeEnabled(c, v) }
    fun setImageCache(c: Context, v: Boolean) { imageCache = v; Prefs.setImageCacheEnabled(c, v) }
    fun setQuality(c: Context, v: String) { quality = v; Prefs.setQuality(c, v) }
    fun setWifiOnly(c: Context, v: Boolean) { wifiOnly = v; Prefs.setDownloadOverWifiOnly(c, v) }
    fun setLandscapeMode(c: Context, v: Boolean) { landscapeMode = v; Prefs.setLandscapeMode(c, v) }
    }
