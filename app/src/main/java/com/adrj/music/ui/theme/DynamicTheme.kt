package com.adrj.music.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.adrj.music.data.PrefsState

object DynamicAccent {
    var color by mutableStateOf<Color?>(null)
}

@Composable
fun rememberAccent(fallback: Color): Color {
    // Reactivo: lee de PrefsState (que sí es observado por Compose)
    if (!PrefsState.dynamicColor) return fallback
    return DynamicAccent.color ?: fallback
}
