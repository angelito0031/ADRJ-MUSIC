package com.adrj.music.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.adrj.music.data.PrefsState
import com.adrj.music.ui.theme.DynamicAccent

@Composable
fun IosGlassBackground(content: @Composable () -> Unit) {
    val isGlass = PrefsState.glassEnabled
    val base = MaterialTheme.colorScheme.background
    val accent = DynamicAccent.color ?: MaterialTheme.colorScheme.primary

    // FIX BUG 7: brushes y efectos recordados por accent (no recrear al cambiar canción)
    val brush1 = remember(accent) {
        Brush.radialGradient(listOf(accent.copy(alpha = 0.55f), Color.Transparent))
    }
    val brush2 = remember(accent) {
        val comp = Color(
            red = (accent.red * 0.6f + 0.4f).coerceAtMost(1f),
            green = accent.green * 0.8f,
            blue = (accent.blue * 0.6f + 0.4f).coerceAtMost(1f)
        )
        Brush.radialGradient(listOf(comp.copy(alpha = 0.45f), Color.Transparent))
    }

    Box(Modifier.fillMaxSize().background(base)) {
        if (isGlass) {
            Box(
                Modifier
                    .size(450.dp)
                    .offset(x = 200.dp, y = (-100).dp)
                    .graphicsLayer {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            renderEffect = RenderEffect
                                .createBlurEffect(180f, 180f, Shader.TileMode.CLAMP)
                                .asComposeRenderEffect()
                        }
                    }
                    .background(brush1, shape = CircleShape)
            )

            Box(
                Modifier
                    .size(500.dp)
                    .offset(x = (-150).dp, y = 900.dp)
                    .graphicsLayer {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            renderEffect = RenderEffect
                                .createBlurEffect(200f, 200f, Shader.TileMode.CLAMP)
                                .asComposeRenderEffect()
                        }
                    }
                    .background(brush2, shape = CircleShape)
            )
        }
        content()
    }
}
