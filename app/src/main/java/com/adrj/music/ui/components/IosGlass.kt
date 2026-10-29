package com.adrj.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.adrj.music.data.PrefsState
import com.adrj.music.ui.theme.DynamicAccent

@Composable
fun Modifier.iosGlass(cornerRadius: Int = 24, topOnly: Boolean = false): Modifier {
    val isGlass = PrefsState.glassEnabled
    val accent = DynamicAccent.color ?: MaterialTheme.colorScheme.primary

    // OPTIMIZACIÓN: remember evita recrear estos objetos en cada recomposición
    val shape = remember(cornerRadius, topOnly) {
        if (topOnly) RoundedCornerShape(topStart = cornerRadius.dp, topEnd = cornerRadius.dp)
        else RoundedCornerShape(cornerRadius.dp)
    }
    val bgBrush = remember {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.04f)))
    }
    val borderBrush = remember(accent) {
        Brush.linearGradient(listOf(
            Color.White.copy(alpha = 0.35f),
            accent.copy(alpha = 0.20f),
            Color.White.copy(alpha = 0.05f)
        ))
    }

    return if (isGlass) {
        this.clip(shape).background(bgBrush).border(width = 1.dp, brush = borderBrush, shape = shape)
    } else {
        this.clip(shape).background(MaterialTheme.colorScheme.surface)
    }
}
