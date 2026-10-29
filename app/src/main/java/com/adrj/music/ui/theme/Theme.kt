package com.adrj.music.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val GrisMuyClaro = Color(0xFFEEEEEE)

fun accentColor(name: String): Color = when (name) {
    "Blanco"  -> Color(0xFFFAFAFA)
    "Cian"    -> Color(0xFF00E5FF)
    "Rojo"    -> Color(0xFFE53935)
    "Azul"    -> Color(0xFF42A5F5)
    "Verde"   -> Color(0xFF66BB6A)
    "Morado"  -> Color(0xFFAB47BC)
    "Ámbar"   -> Color(0xFFFFB300)
    "Rosa"    -> Color(0xFFEC407A)
    else      -> Color(0xFFFAFAFA)
}

// Bordes 90° (24dp) en TODA la app
private val AdrjShapes = Shapes(
    extraSmall = RoundedCornerShape(16.dp),
    small = RoundedCornerShape(20.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun ADRJmusicTheme(
    darkTheme: Boolean = true,
    accent: String = "Blanco",
    fontScale: Float = 1f,
    content: @Composable () -> Unit
) {
    // FIX BUG 3: leer DynamicAccent.color reactivamente para que MaterialTheme.primary cambie con la portada
    val dynamicAccentColor = DynamicAccent.color
    val isDynamic = com.adrj.music.data.PrefsState.dynamicColor
    val ac = if (isDynamic && dynamicAccentColor != null) {
        dynamicAccentColor
    } else {
        accentColor(accent)
    }
    val typography = AdrjTypography.scaled(fontScale)

    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = ac,
            onPrimary = Negro,
            secondary = GrisClaro,
            background = Color(0xFF020204),  // Casi negro puro para máximo contraste vibrante
            onBackground = Blanco,
            surface = Color(0xFF141418),  // FIX BUG 9: opaco, IosGlass maneja la transparencia
            onSurface = Blanco,
            surfaceVariant = Color(0x801E1E22),
            onSurfaceVariant = GrisClaro,
            outline = Gris
        )
    } else {
        lightColorScheme(
            primary = ac,
            onPrimary = Blanco,
            secondary = GrisMedio,
            background = Color(0xFFF7F7FA),
            onBackground = Negro,
            surface = Color(0xFFFFFFFF),
            onSurface = Negro,
            surfaceVariant = Color(0xFFEDEDF0),
            onSurfaceVariant = Gris,
            outline = GrisClaro
        )
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = typography,
        shapes = AdrjShapes,
        content = content
    )
}
