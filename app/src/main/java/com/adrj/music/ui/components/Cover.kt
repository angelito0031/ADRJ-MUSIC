package com.adrj.music.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.adrj.music.ui.theme.Gris

@Composable
fun Cover(
    url: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    corner: Dp = 8.dp
) {
    var loaded by remember(url) { mutableStateOf(false) }

    Box(
        modifier.size(size).clip(RoundedCornerShape(corner)).background(Gris)
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            onSuccess = { loaded = true }
        )
        if (!loaded) ShimmerOverlay()
    }
}

@Composable
fun ShimmerOverlay() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -400f, targetValue = 800f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "x"
    )
    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                start = Offset(x, 0f),
                end = Offset(x + 300f, 300f)
            )
        )
    )
}
