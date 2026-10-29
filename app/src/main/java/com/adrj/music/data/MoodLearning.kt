package com.adrj.music.data

import android.content.Context

/**
 * Sistema simple de "aprendizaje":
 * - Guarda cuántas veces el usuario elige cada mood
 * - Guarda qué artistas ha buscado
 * - Ordena los moods por frecuencia de uso
 */
object MoodLearning {
    private const val FILE = "mood_learning"
    private fun sp(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    // Mood counts
    fun getMoodCount(c: Context, mood: String): Int =
        sp(c).getInt("mood_$mood", 0)

    fun incrementMood(c: Context, mood: String) {
        val v = getMoodCount(c, mood) + 1
        sp(c).edit().putInt("mood_$mood", v).apply()
    }

    // Devuelve los moods ordenados por frecuencia (los más usados primero)
    fun getTopMoods(c: Context): List<String> {
        val all = sp(c).all
        return all.entries
            .filter { it.key.startsWith("mood_") }
            .sortedByDescending { it.value as? Int ?: 0 }
            .map { it.key.removePrefix("mood_") }
    }

    // Artistas buscados recientemente
    fun getRecentArtists(c: Context): List<String> {
        val raw = sp(c).getString("recent_artists", "") ?: ""
        return if (raw.isBlank()) emptyList()
        else raw.split("|").filter { it.isNotBlank() }.take(8)
    }

    fun addRecentArtist(c: Context, artist: String) {
        val current = getRecentArtists(c).toMutableList()
        current.remove(artist)  // evitar duplicados
        current.add(0, artist)  // al frente
        val trimmed = current.take(8).joinToString("|")
        sp(c).edit().putString("recent_artists", trimmed).apply()
    }

    // Artista favorito (el más escuchado)
    fun getFavoriteArtist(c: Context): String? = getRecentArtists(c).firstOrNull()

    // Sugerencia de mood según la hora del día
    fun suggestedMoodByTime(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 6..11 -> "Feliz"
            in 12..14 -> "Concentrado"
            in 15..19 -> "Energético"
            in 20..22 -> "Relajado"
            else -> "Dormir"
        }
    }
}
