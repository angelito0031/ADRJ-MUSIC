package com.adrj.music.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AdrjTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 15.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.5.sp)
)

fun Typography.scaled(s: Float): Typography = copy(
    displayLarge = displayLarge.copy(fontSize = displayLarge.fontSize * s),
    headlineMedium = headlineMedium.copy(fontSize = headlineMedium.fontSize * s),
    titleLarge = titleLarge.copy(fontSize = titleLarge.fontSize * s),
    titleMedium = titleMedium.copy(fontSize = titleMedium.fontSize * s),
    bodyLarge = bodyLarge.copy(fontSize = bodyLarge.fontSize * s),
    bodyMedium = bodyMedium.copy(fontSize = bodyMedium.fontSize * s),
    labelSmall = labelSmall.copy(fontSize = labelSmall.fontSize * s)
)
