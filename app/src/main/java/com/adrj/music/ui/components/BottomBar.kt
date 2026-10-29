package com.adrj.music.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.adrj.music.ui.components.iosGlass

enum class Dest(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Inicio", Icons.Filled.Home),
    Search("search", "Buscar", Icons.Filled.Search),
    Library("library", "Biblioteca", Icons.Filled.LibraryMusic),
    Settings("settings", "Ajustes", Icons.Filled.Settings)
}

@Composable
fun AdrjBottomBar(current: String, onSelect: (Dest) -> Unit) {
    Box(Modifier.fillMaxWidth().iosGlass(cornerRadius = 24, topOnly = true)) {
    NavigationBar(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        tonalElevation = 0.dp
    ) {
        Dest.values().forEach { d ->
            val selected = current == d.route
            val scale by animateFloatAsState(
                targetValue = if (selected) 1.15f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ), label = "navScale"
            )
            val color by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(250), label = "navColor"
            )
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(d) },
                icon = {
                    Box(
                        Modifier.scale(scale).clip(CircleShape).then(
                            if (selected) Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(8.dp)
                            else Modifier.padding(4.dp)
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(d.icon, contentDescription = d.label, tint = color)
                    }
                },
                label = { Text(d.label, style = MaterialTheme.typography.labelSmall, color = color) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        }
    }
    }
}
