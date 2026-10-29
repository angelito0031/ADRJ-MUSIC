package com.adrj.music.data

import android.content.Context

object Prefs {
    private const val FILE = "adrj_prefs"
    private fun sp(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isDark(c: Context) = sp(c).getBoolean("dark", true)
    fun setDark(c: Context, v: Boolean) = sp(c).edit().putBoolean("dark", v).apply()

    fun accent(c: Context) = sp(c).getString("accent", "Blanco") ?: "Blanco"
    fun setAccent(c: Context, v: String) = sp(c).edit().putString("accent", v).apply()

    fun fontScale(c: Context) = sp(c).getFloat("font", 1f)
    fun setFontScale(c: Context, v: Float) = sp(c).edit().putFloat("font", v).apply()

    fun glassEnabled(c: Context) = sp(c).getBoolean("glass_enabled", true)
    fun setGlassEnabled(c: Context, v: Boolean) = sp(c).edit().putBoolean("glass_enabled", v).apply()

    fun dynamicColor(c: Context) = sp(c).getBoolean("dyn_color", false)
    fun setDynamicColor(c: Context, v: Boolean) = sp(c).edit().putBoolean("dyn_color", v).apply()

    fun blurBackground(c: Context) = sp(c).getBoolean("blur_bg", true)
    fun setBlurBackground(c: Context, v: Boolean) = sp(c).edit().putBoolean("blur_bg", v).apply()

    fun showMiniPlayerAlways(c: Context) = sp(c).getBoolean("mini_always", true)
    fun setShowMiniPlayerAlways(c: Context, v: Boolean) = sp(c).edit().putBoolean("mini_always", v).apply()

    fun autoplay(c: Context) = sp(c).getBoolean("autoplay", true)
    fun setAutoplay(c: Context, v: Boolean) = sp(c).edit().putBoolean("autoplay", v).apply()

    fun repeatMode(c: Context) = sp(c).getInt("repeat", 0)
    fun setRepeatMode(c: Context, v: Int) = sp(c).edit().putInt("repeat", v).apply()

    fun shuffle(c: Context) = sp(c).getBoolean("shuffle", false)
    fun setShuffle(c: Context, v: Boolean) = sp(c).edit().putBoolean("shuffle", v).apply()

    fun quality(c: Context) = sp(c).getString("quality", "Alta (m4a 128k)") ?: "Alta (m4a 128k)"
    fun setQuality(c: Context, v: String) = sp(c).edit().putString("quality", v).apply()

    fun animations(c: Context) = sp(c).getBoolean("anim", true)
    fun setAnimations(c: Context, v: Boolean) = sp(c).edit().putBoolean("anim", v).apply()

    fun reduceMotion(c: Context) = sp(c).getBoolean("reduce_motion", false)
    fun setReduceMotion(c: Context, v: Boolean) = sp(c).edit().putBoolean("reduce_motion", v).apply()

    fun imageCacheEnabled(c: Context) = sp(c).getBoolean("img_cache", true)
    fun setImageCacheEnabled(c: Context, v: Boolean) = sp(c).edit().putBoolean("img_cache", v).apply()

    fun ytdlpLastUpdate(c: Context) = sp(c).getLong("ytdlp_last_update", 0L)
    fun setYtdlpLastUpdate(c: Context, v: Long) = sp(c).edit().putLong("ytdlp_last_update", v).apply()

    fun landscapeMode(c: Context) = sp(c).getBoolean("landscape", false)
    fun setLandscapeMode(c: Context, v: Boolean) = sp(c).edit().putBoolean("landscape", v).apply()

    fun crossfadeEnabled(c: Context) = sp(c).getBoolean("crossfade", true)
    fun setCrossfadeEnabled(c: Context, v: Boolean) = sp(c).edit().putBoolean("crossfade", v).apply()

    fun crossfadeAudio(c: Context) = sp(c).getBoolean("audio_crossfade", false)
    fun setCrossfadeAudio(c: Context, v: Boolean) = sp(c).edit().putBoolean("audio_crossfade", v).apply()

    fun downloadOverWifiOnly(c: Context) = sp(c).getBoolean("dl_wifi", false)
    fun setDownloadOverWifiOnly(c: Context, v: Boolean) = sp(c).edit().putBoolean("dl_wifi", v).apply()

    fun getPlaylists(c: Context): Map<String, List<String>> {
        val raw = sp(c).getString("playlists", "{}") ?: "{}"
        return try {
            val obj = org.json.JSONObject(raw)
            obj.keys().asSequence().associateWith { key ->
                val arr = obj.getJSONArray(key)
                (0 until arr.length()).map { arr.getString(it) }
            }
        } catch (_: Exception) { emptyMap() }
    }

    fun savePlaylists(c: Context, playlists: Map<String, List<String>>) {
        val obj = org.json.JSONObject()
        playlists.forEach { (name, ids) -> obj.put(name, org.json.JSONArray(ids)) }
        sp(c).edit().putString("playlists", obj.toString()).apply()
    }
}
