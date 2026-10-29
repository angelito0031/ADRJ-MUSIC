package com.adrj.music.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.adrj.music.data.PrefsState
import com.adrj.music.player.PlayerHolder
import com.adrj.music.ui.theme.DynamicAccent

/**
 * Tarjetas translúcidas SIMPLES encima del fondo difuminado.
 * No intenta blur interno (Android no lo permite bien).
 * Solo blanco alpha que se ve distinto porque el fondo tiene la portada difuminada grande.
 */
@Composable
fun Modifier.frostedGlass(corner: Dp = 24.dp): Modifier {
    val isGlass = PrefsState.glassEnabled
    val accent = DynamicAccent.color ?: MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(corner)

    return if (isGlass) {
        this
            .shadow(
                elevation = 12.dp,
                shape = shape,
                ambientColor = accent.copy(alpha = 0.40f),
                spotColor = accent.copy(alpha = 0.55f)
            )
            .clip(shape)
            .background(Color.White.copy(alpha = 0.10f))
    } else {
        this
            .clip(shape)
            .iosGlass(24)
    }
}

/**
 * Fondo global con portada difuminada grande (blur 100px).
 * Aquí sí se ve el vidrio REAL porque el fondo es la portada borrosa grande.
 */
@Composable
fun FrostedBackground(content: @Composable () -> Unit) {
    val isGlass = PrefsState.glassEnabled
    val base = MaterialTheme.colorScheme.background
    val accent = DynamicAccent.color ?: MaterialTheme.colorScheme.primary
    val track = PlayerHolder.currentTrack
    val artUrl = track?.thumbnailUrl

    Box(Modifier.fillMaxSize().background(base)) {
        if (isGlass && !artUrl.isNullOrBlank()) {
            // Portada difuminada GRANDE (fondo real del vidrio)
            AsyncImage(
                model = artUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            renderEffect = RenderEffect
                                .createBlurEffect(100f, 100f, Shader.TileMode.CLAMP)
                                .asComposeRenderEffect()
                        }
                    }
                    .alpha(0.55f)
            )
            // Scrim oscuro para legibilidad
            Box(
                Modifier.fillMaxSize().background(base.copy(alpha = 0.40f))
            )
        } else if (isGlass) {
            // Sin canción: gradiente radial del acento
            Box(
                Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.20f),
                            Color.Transparent
                        ),
                        radius = 1400f
                    )
                )
            )
        }
        content()
    }
}
