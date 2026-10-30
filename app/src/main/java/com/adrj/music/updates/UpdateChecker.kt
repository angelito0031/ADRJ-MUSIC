package com.adrj.music.updates

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class UpdateInfo(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val forceUpdate: Boolean,
    val minimumVersionCode: Int,
    val message: String,
    val downloadUrl: String
)

object UpdateChecker {

    private const val SIGNING_KEY = "db35004ffd72aceeb2ba9235c44a3e53ea2f372853277e7bef21f46afd396f85"

    private const val CONFIG_URL =
        "https://gitlab.com/adrj-group/adrj-music/-/raw/config/force_update/force_update.json"

    private const val CHECK_INTERVAL_MS = 15 * 60 * 1000L
    @Volatile private var lastCheck = 0L
    @Volatile private var cached: UpdateInfo? = null

    suspend fun check(ctx: Context, force: Boolean = false): UpdateInfo? = withContext(Dispatchers.IO) {
        logToFile(ctx, "=== check() iniciado ===")
        // TOAST de diagnóstico
        kotlinx.coroutines.withContext(Dispatchers.Main) {
            try {
                android.widget.Toast.makeText(ctx, "🔄 Chequeando update...", android.widget.Toast.LENGTH_LONG).show()
            } catch (_: Throwable) {}
        }
        val now = System.currentTimeMillis()
        if (!force && now - lastCheck < CHECK_INTERVAL_MS) return@withContext cached
        lastCheck = now

        try {
            val conn = (URL(CONFIG_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "ADRJmusic-Android")
                // Evitar cache CDN de GitHub
                setRequestProperty("Cache-Control", "no-cache")
                useCaches = false
            }
            logToFile(ctx, "HTTP ${conn.responseCode}")
            if (conn.responseCode != 200) {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    try { android.widget.Toast.makeText(ctx, "❌ HTTP ${conn.responseCode}", android.widget.Toast.LENGTH_LONG).show() } catch (_: Throwable) {}
                }
                android.util.Log.e("ADRJ", "Update: HTTP ${conn.responseCode}")
                cached = null
                return@withContext null
            }
            val body = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(body)

            logToFile(ctx, "JSON: ${body.length} chars")
            val signature = json.optString("signature", "")
            if (signature.isBlank()) {
                android.util.Log.e("ADRJ", "Update sin firma")
                cached = null
                return@withContext null
            }

            // ═══════════════════════════════════════════════════════════════
            // PAYLOAD CANÓNICO: mismos campos, orden alfabético, sin signature
            // (idéntico al que genera el script bash con json.dumps sort_keys=True)
            // ═══════════════════════════════════════════════════════════════
            // Payload canónico: mismos bytes que Python con ensure_ascii=True
            // (escapa tildes a \u00f3 para que coincida byte a byte)
            fun quoteAscii(s: String): String {
                val sb = StringBuilder()
                sb.append('"')
                for (ch in s) {
                    when {
                        ch == '"' -> sb.append("\\\"")
                        ch == '\\' -> sb.append("\\\\")
                        ch == '\n' -> sb.append("\\n")
                        ch == '\r' -> sb.append("\\r")
                        ch == '\t' -> sb.append("\\t")
                        ch.code < 0x20 -> sb.append("\\u%04x".format(ch.code))
                        ch.code > 0x7E -> sb.append("\\u%04x".format(ch.code))
                        else -> sb.append(ch)
                    }
                }
                sb.append('"')
                return sb.toString()
            }

            val canonicalPayload = buildString {
                append("{")
                append("\"downloadUrl\":")
                append(quoteAscii(json.getString("downloadUrl")))
                append(",")
                append("\"forceUpdate\":")
                append(json.getBoolean("forceUpdate"))
                append(",")
                append("\"latestVersionCode\":")
                append(json.getInt("latestVersionCode"))
                append(",")
                append("\"latestVersionName\":")
                append(quoteAscii(json.getString("latestVersionName")))
                append(",")
                append("\"message\":")
                append(quoteAscii(json.getString("message")))
                append(",")
                append("\"minimumVersionCode\":")
                append(json.getInt("minimumVersionCode"))
                append(",")
                append("\"publishedAt\":")
                append(quoteAscii(json.getString("publishedAt")))
                append("}")
            }

            android.util.Log.d("ADRJ", "Payload canónico: $canonicalPayload")
            val expectedSig = hmacSha256(canonicalPayload)
            logToFile(ctx, "Firma esperada: $expectedSig")
            android.util.Log.d("ADRJ", "Firma esperada: $expectedSig")
            logToFile(ctx, "Firma recibida: $signature")
            android.util.Log.d("ADRJ", "Firma recibida: $signature")

            if (!constantTimeEquals(signature, expectedSig)) {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                try { android.widget.Toast.makeText(ctx, "❌ Firma inválida", android.widget.Toast.LENGTH_LONG).show() } catch (_: Throwable) {}
            }
            android.util.Log.e("ADRJ", "Firma inválida")
                cached = null
                return@withContext null
            }

            logToFile(ctx, "Firma verificada OK")
            val currentCode = currentVersionCode(ctx)
            val latestCode = json.getInt("latestVersionCode")
            logToFile(ctx, "versionCode actual=$currentCode, latest=$latestCode")
            if (currentCode >= latestCode) {
                cached = null
                return@withContext null
            }

            val info = UpdateInfo(
                latestVersionCode = latestCode,
                latestVersionName = json.getString("latestVersionName"),
                forceUpdate = json.getBoolean("forceUpdate"),
                minimumVersionCode = json.getInt("minimumVersionCode"),
                message = json.getString("message"),
                downloadUrl = json.getString("downloadUrl")
            )
            logToFile(ctx, "✅ Update disponible: v${info.latestVersionName}")
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                try { android.widget.Toast.makeText(ctx, "✅ Update: v${info.latestVersionName}", android.widget.Toast.LENGTH_LONG).show() } catch (_: Throwable) {}
            }
            android.util.Log.d("ADRJ", "Update disponible: v${info.latestVersionName}")
            cached = info
            info
        } catch (e: Exception) {
            logToFile(ctx, "ERROR: ${e.message}")
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                try { android.widget.Toast.makeText(ctx, "❌ Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show() } catch (_: Throwable) {}
            }
            android.util.Log.e("ADRJ", "Update check error", e)
            cached = null
            null
        }
    }

    fun currentVersionCode(ctx: Context): Int = try {
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionCode
        }
    } catch (_: PackageManager.NameNotFoundException) { 1 }

    fun currentVersionName(ctx: Context): String = try {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "?"
    } catch (_: Exception) { "?" }

    private fun logToFile(ctx: Context, msg: String) {
        try {
            val dir = java.io.File(ctx.getExternalFilesDir(null), "update_logs").apply { mkdirs() }
            val f = java.io.File(dir, "update.log")
            f.appendText("${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())} $msg\n")
        } catch (_: Throwable) {}
    }

    private fun hmacSha256(data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(SIGNING_KEY.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(data.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var r = 0
        for (i in a.indices) r = r or (a[i].code xor b[i].code)
        return r == 0
    }
}
