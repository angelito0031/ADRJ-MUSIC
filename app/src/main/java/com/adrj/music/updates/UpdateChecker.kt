package com.adrj.music.updates

import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
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

    // Cliente OkHttp con SSL permisivo (para redes que bloquean certificados)
    private val client: OkHttpClient by lazy {
        try {
            val trustAllCerts = arrayOf<javax.net.ssl.TrustManager>(
                object : javax.net.ssl.X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = emptyArray()
                }
            )
            val sslContext = javax.net.ssl.SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            
            val builder = OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as javax.net.ssl.X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .callTimeout(15, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()
            builder
        } catch (e: Throwable) {
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .callTimeout(15, TimeUnit.SECONDS)
                .build()
        }
    }

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

        // PASO 1: Descargar JSON con OkHttp
        toast(ctx, "2. Descargando JSON...")
        val body: String
        try {
            val request = Request.Builder()
                .url(CONFIG_URL)
                .header("User-Agent", "ADRJmusic-Android")
                .header("Cache-Control", "no-cache")
                .build()
            toast(ctx, "3. Ejecutando request...")
            client.newCall(request).execute().use { response ->
                toast(ctx, "3.5 HTTP: ${response.code}")
                if (!response.isSuccessful) {
                    return@withContext null
                }
                toast(ctx, "3.6 Leyendo body...")
                body = response.body?.string() ?: run {
                    toast(ctx, "3.7 Body null")
                    return@withContext null
                }
                toast(ctx, "3.8 Body OK (${body.length} chars)")
            }
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
