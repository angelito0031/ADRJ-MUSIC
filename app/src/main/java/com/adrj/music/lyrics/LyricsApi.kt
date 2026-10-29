package com.adrj.music.lyrics

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.Normalizer

data class LyricLine(val timeMs: Long, val text: String)
data class Lyrics(val synced: List<LyricLine>, val plain: String)

object LyricsApi {

    private var cacheDir: File? = null

    fun init(context: Context) {
        cacheDir = File(context.cacheDir, "lyrics").apply { mkdirs() }
    }

    suspend fun fetch(artist: String, title: String, durationSec: Long): Lyrics? =
        withContext(Dispatchers.IO) {
            val cleanPair = parseArtistTitle(artist, title)
            val cleanArt = cleanPair.first
            val cleanTit = cleanPair.second
            if (cleanArt.isBlank() && cleanTit.isBlank()) return@withContext null

            val key = cacheKeyOf(cleanArt, cleanTit, durationSec)
            readCache(key)?.let { return@withContext it }

            // 1) /api/get con duración (match exacto, el más fiable)
            if (cleanArt.isNotBlank() && cleanTit.isNotBlank()) {
                tryGet(cleanArt, cleanTit, durationSec)?.let {
                    writeCache(key, it); return@withContext it
                }
            }

            // 2) /api/search con query "artist title"
            val q1 = if (cleanArt.isNotBlank()) "$cleanArt $cleanTit" else cleanTit
            searchLrclibSmart(q1, cleanArt, cleanTit, durationSec)?.let {
                writeCache(key, it); return@withContext it
            }

            // 3) /api/search solo con título (por si el artista vino mal)
            if (cleanArt.isNotBlank()) {
                searchLrclibSmart(cleanTit, cleanArt, cleanTit, durationSec)?.let {
                    writeCache(key, it); return@withContext it
                }
            }

            // 4) Fallback lyrics.ovh (solo plain)
            lyricsOvh(cleanArt, cleanTit)?.let {
                writeCache(key, it); return@withContext it
            }

            null
        }

    // ==================== PARSER INTELIGENTE ====================

    private fun parseArtistTitle(artistRaw: String, titleRaw: String): Pair<String, String> {
        var art = cleanArtist(artistRaw)
        var tit = titleRaw.trim()

        // Detectar "Artist - Song" duplicado en el título
        val dashSplit = tit.split(Regex("\\s+[-–—]\\s+"), limit = 2)
        if (dashSplit.size == 2) {
            val prefix = dashSplit[0].trim()
            val suffix = dashSplit[1].trim()
            val prefixMatchesArtist = art.isNotBlank() &&
                similarity(normalize(prefix), normalize(art)) > 0.65
            val artistEmpty = art.isBlank()

            // Solo dividir si el prefijo ES el artista (o el artista está vacío)
            if (prefixMatchesArtist || artistEmpty) {
                tit = suffix
                if (artistEmpty) art = cleanArtist(prefix)
            }
        }

        // "Song | Album" o "Song // Album" → solo lo primero
        tit = tit.split(Regex("\\s+[|/]{1,2}\\s+"), limit = 2)[0].trim()

        return art to cleanTitle(tit)
    }

    private val BRACKET_REGEX = Regex("""[\(\[\{].*?[\)\]\}]""", RegexOption.DOT_MATCHES_ALL)
    private val JUNK_WORDS = setOf(
        "official", "oficial", "video", "audio", "lyric", "lyrics", "letra", "letras",
        "hd", "hq", "4k", "8k", "1080p", "720p",
        "remaster", "remastered", "remasterizado", "remix", "edit", "edition",
        "live", "en vivo", "acoustic", "acustico", "acústico", "demo",
        "version", "versión", "cover", "instrumental", "karaoke",
        "prod", "produced", "producer", "original", "extended", "radio",
        "visualizer", "music video", "videoclip", "topic", "vevo",
        "explicit", "clean", "dirty", "censored", "sped up", "slowed",
        "reverb", "nightcore", "tiktok", "from the"
    )

    private fun cleanTitle(raw: String): String {
        var t = raw

        // Quitar corchetes con contenido basura
        t = BRACKET_REGEX.replace(t) { m ->
            val content = m.value.lowercase()
            if (JUNK_WORDS.any { content.contains(it) }) " " else m.value
        }

        // Quitar años sueltos: (2019), (1999)
        t = t.replace(Regex("""\(\s*\d{4}\s*\)"""), " ")

        // Quitar feat./ft./featuring/con/prod y lo que sigue
        t = t.replace(Regex("""(?i)\s+(ft|feat|featuring|con|prod|produced)\s*\.?\s+.+$"""), "")

        // Quitar sufijos con guion: - Remastered, - Live, etc.
        t = t.replace(Regex("""(?i)\s*[-–—]\s*(remaster(ed)?|remix|edit|version|en vivo|live|acoustic|demo|cover|karaoke|instrumental|sped up|slowed).*$"""), "")

        // Quitar HD, 4K sueltos
        t = t.replace(Regex("""(?i)\b(hd|hq|4k|8k|1080p|720p)\b"""), " ")

        // Quitar "Topic" / "VEVO" sueltos
        t = t.replace(Regex("""(?i)\b(topic|vevo)\b"""), " ")

        // Normalizar espacios
        t = t.replace(Regex("""\s+"""), " ").trim()

        // Quitar puntuación al inicio/final
        return t.trim(' ', '-', '–', '—', '|', '/', ':', ',', '.', ';', '_')
    }

    private fun cleanArtist(raw: String): String {
        var a = raw
        a = a.replace(Regex("""(?i)\s*[-–—]\s*(topic|vevo)\s*$"""), "")
        a = a.replace(Regex("""(?i)\s+(vevo|official|music|records|channel)\s*$"""), "")
        a = a.replace(Regex("""(?i)\s+(ft|feat|featuring|con)\s*\.?\s+.+$"""), "")
        a = a.replace(Regex("""\s+"""), " ").trim()
        return a.trim(' ', '-', '–', '—', '|', '/', ':', ',', '.', ';', '_')
    }

    // ==================== /api/get ====================

    private fun tryGet(artist: String, title: String, durationSec: Long): Lyrics? {
        if (artist.isBlank() || title.isBlank()) return null
        return try {
            val a = URLEncoder.encode(artist, "UTF-8")
            val t = URLEncoder.encode(title, "UTF-8")
            val d = if (durationSec > 0) "&duration=$durationSec" else ""
            val body = httpGet("https://lrclib.net/api/get?artist_name=$a&track_name=$t$d") ?: return null
            val j = JSONObject(body)
            val synced = j.optString("syncedLyrics", "")
            val plain = j.optString("plainLyrics", "")
            if (synced.isBlank() && plain.isBlank()) return null
            // Sanity: si tenemos duración, debe coincidir ±15s
            val dur = j.optLong("duration", 0L)
            if (durationSec > 0 && dur > 0 && kotlin.math.abs(dur - durationSec) > 15) return null
            Lyrics(parseLrc(synced), plain)
        } catch (_: Exception) { null }
    }

    // ==================== /api/search con scoring ====================

    private fun searchLrclibSmart(query: String, wantArtist: String, wantTitle: String, durationSec: Long): Lyrics? {
        if (query.isBlank()) return null
        return try {
            val q = URLEncoder.encode(query, "UTF-8")
            val body = httpGet("https://lrclib.net/api/search?q=$q") ?: return null
            val arr = JSONArray(body)
            if (arr.length() == 0) return null

            data class Candidate(val score: Double, val obj: JSONObject)
            var best: Candidate? = null

            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val trackName = o.optString("trackName", "")
                val artistName = o.optString("artistName", "")
                val synced = o.optString("syncedLyrics", "")
                val plain = o.optString("plainLyrics", "")
                val dur = o.optLong("duration", 0L)
                if (synced.isBlank() && plain.isBlank()) continue

                var score = 0.0
                score += similarity(normalize(wantTitle), normalize(trackName)) * 0.45
                score += similarity(normalize(wantArtist), normalize(artistName)) * 0.30
                if (durationSec > 0 && dur > 0) {
                    val diff = kotlin.math.abs(dur - durationSec)
                    score += when {
                        diff <= 3 -> 0.20
                        diff <= 8 -> 0.15
                        diff <= 15 -> 0.08
                        else -> 0.0
                    }
                }
                if (synced.isNotBlank()) score += 0.05

                if (best == null || score > best!!.score) best = Candidate(score, o)
            }

            val chosen = best ?: return null
            // Umbral ALTO (0.65) o alternativamente 0.55 + duración estricta
            if (chosen.score < 0.65) {
                if (!(durationSec > 0 && chosen.score >= 0.55 && durationMatch(chosen.obj, durationSec))) {
                    return null
                }
            }
            Lyrics(
                parseLrc(chosen.obj.optString("syncedLyrics", "")),
                chosen.obj.optString("plainLyrics", "")
            )
        } catch (_: Exception) { null }
    }

    private fun durationMatch(obj: JSONObject, wantDur: Long): Boolean {
        val dur = obj.optLong("duration", 0L)
        if (dur <= 0 || wantDur <= 0) return false
        return kotlin.math.abs(dur - wantDur) <= 5
    }

    // ==================== lyrics.ovh fallback ====================

    private fun lyricsOvh(artist: String, title: String): Lyrics? {
        if (artist.isBlank() || title.isBlank()) return null
        return try {
            val a = URLEncoder.encode(artist, "UTF-8")
            val t = URLEncoder.encode(title, "UTF-8")
            val body = httpGet("https://api.lyrics.ovh/v1/$a/$t") ?: return null
            val plain = JSONObject(body).optString("lyrics", "")
            if (plain.isBlank() || plain.length < 20) return null
            Lyrics(emptyList(), plain)
        } catch (_: Exception) { null }
    }

    // ==================== SIMILITUD ====================

    private fun normalize(s: String): String {
        val nfd = Normalizer.normalize(s.lowercase().trim(), Normalizer.Form.NFD)
        return nfd.replace(Regex("""\p{Mn}+"""), "")
            .replace(Regex("""[^\p{L}\p{N}\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun similarity(a: String, b: String): Double {
        if (a.isBlank() || b.isBlank()) return 0.0
        val s1 = normalize(a); val s2 = normalize(b)
        if (s1 == s2) return 1.0
        if (s1.contains(s2) || s2.contains(s1)) return 0.85
        val maxLen = maxOf(s1.length, s2.length)
        if (maxLen == 0) return 1.0
        val words1 = s1.split(" ").filter { it.isNotBlank() }.toSet()
        val words2 = s2.split(" ").filter { it.isNotBlank() }.toSet()
        val jaccard = if (words1.isNotEmpty() && words2.isNotEmpty()) {
            (words1 intersect words2).size.toDouble() / (words1 union words2).size
        } else 0.0
        val lev = 1.0 - levenshtein(s1, s2).toDouble() / maxLen
        return maxOf(jaccard, lev)
    }

    private fun levenshtein(a: String, b: String): Int {
        val m = a.length; val n = b.length
        if (m == 0) return n; if (n == 0) return m
        val dp = IntArray(n + 1) { it }
        for (i in 1..m) {
            var prev = dp[0]; dp[0] = i
            for (j in 1..n) {
                val tmp = dp[j]
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[j] = minOf(dp[j] + 1, dp[j - 1] + 1, prev + cost)
                prev = tmp
            }
        }
        return dp[n]
    }

    // ==================== HTTP ====================

    private fun httpGet(urlStr: String): String? {
        return try {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("User-Agent", "ADRJmusic/1.0 (https://github.com/adrj-music)")
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.bufferedReader().readText()
        } catch (_: Exception) { null }
    }

    // ==================== CACHE ====================

    private fun cacheKeyOf(artist: String, title: String, duration: Long): String {
        // Bucket de duración a 5s para tolerar redondeos
        val durBucket = if (duration > 0) (duration / 5) * 5 else 0
        val raw = "${normalize(artist)}|${normalize(title)}|$durBucket".lowercase()
        return raw.hashCode().toString() + "_" + raw.length + ".json"
    }

    private fun readCache(key: String): Lyrics? {
        val dir = cacheDir ?: return null
        val f = File(dir, key)
        if (!f.exists()) return null
        return try {
            val j = JSONObject(f.readText())
            val arr = j.optJSONArray("synced") ?: JSONArray()
            val synced = (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i); LyricLine(o.getLong("t"), o.getString("x"))
            }
            val plain = j.optString("plain", "")
            if (synced.isEmpty() && plain.isBlank()) null else Lyrics(synced, plain)
        } catch (_: Exception) { null }
    }

    private fun writeCache(key: String, lyrics: Lyrics) {
        val dir = cacheDir ?: return
        try {
            val j = JSONObject(); val arr = JSONArray()
            lyrics.synced.forEach { l ->
                arr.put(JSONObject().apply { put("t", l.timeMs); put("x", l.text) })
            }
            j.put("synced", arr); j.put("plain", lyrics.plain)
            File(dir, key).writeText(j.toString())
        } catch (_: Exception) {}
    }

    fun clearCache() { try { cacheDir?.listFiles()?.forEach { it.delete() } } catch (_: Exception) {} }
    fun cacheSize(): Long = try { cacheDir?.listFiles()?.sumOf { it.length() } ?: 0L } catch (_: Exception) { 0L }

    // ==================== PARSE LRC ====================

    private fun parseLrc(raw: String): List<LyricLine> {
        if (raw.isBlank()) return emptyList()
        val re = Regex("""\[(\d+):(\d+)(?:[\.:](\d+))?\](.*)""")
        return raw.lineSequence().mapNotNull { line ->
            val m = re.find(line) ?: return@mapNotNull null
            val min = m.groupValues[1].toLongOrNull() ?: 0
            val sec = m.groupValues[2].toLongOrNull() ?: 0
            val frac = m.groupValues[3].let {
                when (it.length) { 0 -> 0L; 1 -> it.toLong() * 100; 2 -> it.toLong() * 10; else -> it.take(3).toLong() }
            }
            LyricLine(min * 60_000 + sec * 1000 + frac, m.groupValues[4].trim())
        }.filter { it.text.isNotBlank() }.sortedBy { it.timeMs }.toList()
    }
}
