package com.adrj.music.updates

import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
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
        "https://adrj-music.onrender.com/api/version"

    private fun toast(ctx: Context, msg: String) {
        try {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                try {
                    Toast.makeText(ctx.applicationContext, msg, Toast.LENGTH_SHORT).show()
                } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {}
    }

    suspend fun check(ctx: Context, force: Boolean = false): UpdateInfo? = withContext(Dispatchers.IO) {
        toast(ctx, "1. Iniciando check...")

        // PASO 1: Descargar JSON con HttpURLConnection
        toast(ctx, "2. Descargando JSON...")
        val body: String
        try {
            val url = URL(CONFIG_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 30000
            conn.readTimeout = 30000
            conn.setRequestProperty("User-Agent", "ADRJmusic-Android")
            conn.setRequestProperty("Accept", "application/json")
            
            toast(ctx, "3. Conectando...")
            conn.connect()
            toast(ctx, "3.5 Conectado!")
            
            val code = conn.responseCode
            toast(ctx, "3.6 HTTP: $code")
            
            if (code != 200) {
                return@withContext null
            }
            
            toast(ctx, "3.7 Leyendo body...")
            body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            
            toast(ctx, "3.8 Body OK (${body.length} chars)")
        } catch (e: Exception) {
            toast(ctx, "3. ERROR: ${e.javaClass.simpleName}: ${e.message}")
            return@withContext null
        }

        toast(ctx, "4. JSON descargado")

        // PASO 2: Parsear JSON
        val json = try {
            JSONObject(body)
        } catch (e: Exception) {
            toast(ctx, "4. ERROR parse: ${e.message}")
            return@withContext null
        }

        val signature = json.optString("signature", "")
        if (signature.isBlank()) {
            toast(ctx, "4. ERROR: sin signature")
            return@withContext null
        }

        // PASO 3: Construir payload
        val payload = buildPayload(json)

        // PASO 4: Calcular firma
        val expectedSig = hmacSha256(payload)

        // PASO 5: Comparar
        if (signature != expectedSig) {
            toast(ctx, "7. FIRMA INVALIDA")
            toast(ctx, "Exp: ${expectedSig.take(16)}")
            toast(ctx, "Rec: ${signature.take(16)}")
            return@withContext null
        }
        toast(ctx, "7. Firma OK")

        // PASO 6: Comparar versiones
        val currentCode = currentVersionCode(ctx)
        val latestCode = json.getInt("latestVersionCode")
        toast(ctx, "8. Version: $currentCode vs $latestCode")
        if (currentCode >= latestCode) {
            toast(ctx, "8. Sin update")
            return@withContext null
        }

        // PASO 7: Devolver info
        toast(ctx, "9. Update disponible!")
        UpdateInfo(
            latestVersionCode = latestCode,
            latestVersionName = json.getString("latestVersionName"),
            forceUpdate = json.getBoolean("forceUpdate"),
            minimumVersionCode = json.getInt("minimumVersionCode"),
            message = json.getString("message"),
            downloadUrl = json.getString("downloadUrl")
        )
    }

    private fun buildPayload(json: JSONObject): String {
        val sb = StringBuilder()
        sb.append("{")
        sb.append("\"latestVersionCode\":").append(json.getInt("latestVersionCode")).append(",")
        sb.append("\"latestVersionName\":").append(quoteAscii(json.getString("latestVersionName"))).append(",")
        sb.append("\"forceUpdate\":").append(if (json.getBoolean("forceUpdate")) "true" else "false").append(",")
        sb.append("\"minimumVersionCode\":").append(json.getInt("minimumVersionCode")).append(",")
        sb.append("\"message\":").append(quoteAscii(json.getString("message"))).append(",")
        sb.append("\"downloadUrl\":").append(quoteAscii(json.getString("downloadUrl"))).append(",")
        sb.append("\"publishedAt\":").append(quoteAscii(json.getString("publishedAt")))
        sb.append("}")
        return sb.toString()
    }

    private fun quoteAscii(s: String): String {
        val sb = StringBuilder()
        sb.append('"')
        for (ch in s) {
            when {
                ch == '"' -> sb.append("\\\"")
                ch == '\\' -> sb.append("\\\\")
                ch == '\n' -> sb.append("\\n")
                ch == '\r' -> sb.append("\\r")
                ch == '\t' -> sb.append("\\t")
                ch.code < 0x20 -> sb.append(String.format("\\u%04x", ch.code))
                ch.code > 0x7E -> sb.append(String.format("\\u%04x", ch.code))
                else -> sb.append(ch)
            }
        }
        sb.append('"')
        return sb.toString()
    }

    fun currentVersionCode(ctx: Context): Int = try {
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionCode
        }
    } catch (_: PackageManager.NameNotFoundException) { 1 }

    private fun hmacSha256(data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(SIGNING_KEY.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(data.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}
