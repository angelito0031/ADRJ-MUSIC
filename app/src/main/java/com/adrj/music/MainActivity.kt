package com.adrj.music

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.adrj.music.data.MusicRepository
import com.adrj.music.data.PrefsState
import com.adrj.music.player.PlayerHolder
import com.adrj.music.ui.components.AdrjBottomBar
import com.adrj.music.ui.components.MiniPlayer
import com.adrj.music.ui.components.IosGlassBackground
import com.adrj.music.ui.screens.*
import com.adrj.music.ui.theme.ADRJmusicTheme

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        createNotifChannel()
        requestNotifPermission()
        PrefsState.load(applicationContext)

        // FIX ARRANQUE: PlayerHolder.init() se llama en el primer play, no al abrir.
        // El repository se crea rápido (solo instancia Room).
        try { PlayerHolder.repository = MusicRepository.create(applicationContext) } catch (e: Throwable) { android.util.Log.e("ADRJ_CRITICAL", "Error silencioso atrapado", e) }

        setContent {
            val ctx = LocalContext.current
            // FIX: settings derivado solo cuando cambian sus campos (no en cada recomposición)
            val settings = remember(PrefsState.dark, PrefsState.accent, PrefsState.fontScale, PrefsState.dynamicColor) {
                Settings(
                    dark = PrefsState.dark,
                    accent = PrefsState.accent,
                    fontScale = PrefsState.fontScale,
                    dynamicColor = PrefsState.dynamicColor
                )
            }

            ADRJmusicTheme(
                darkTheme = settings.dark,
                accent = settings.accent,
                fontScale = settings.fontScale
            ) {
                if (PrefsState.landscapeMode && PlayerHolder.currentTrack != null) {
                    LandscapePlayer(onClose = { PrefsState.setLandscapeMode(ctx, false) })
                } else {
                    AdrjRoot(settings) { newSettings ->
                        // FIX: El callback AHORA sí actualiza las prefs
                        if (newSettings.dark != PrefsState.dark) PrefsState.setDark(ctx, newSettings.dark)
                        if (newSettings.accent != PrefsState.accent) PrefsState.setAccent(ctx, newSettings.accent)
                        if (newSettings.fontScale != PrefsState.fontScale) PrefsState.setFontScale(ctx, newSettings.fontScale)
                        if (newSettings.dynamicColor != PrefsState.dynamicColor) PrefsState.setDynamicColor(ctx, newSettings.dynamicColor)
                    }
                }
            }
        }
    }

    private fun createNotifChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel("adrj_playback", "Reproducción", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(ch)
        }
    }

    private fun requestNotifPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun AdrjRoot(settings: Settings, onSettingsChange: (Settings) -> Unit) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route ?: "home"
    var showFullPlayer by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = showFullPlayer,
        transitionSpec = {
            if (targetState) {
                (slideInVertically(initialOffsetY = { it }) + fadeIn(tween(300))).togetherWith(fadeOut(tween(200)))
            } else {
                (slideInVertically(initialOffsetY = { it / 3 }) + fadeIn(tween(250))).togetherWith(fadeOut(tween(200)))
            }
        },
        label = "player"
    ) { full ->
        if (full) {
            FullPlayerScreen(onClose = { showFullPlayer = false })
        } else {
            Scaffold(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                bottomBar = {
                    Column {
                        AnimatedVisibility(
                            visible = PlayerHolder.currentTrack != null || PrefsState.miniAlways,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        ) { MiniPlayer(onExpand = { showFullPlayer = true }) }
                        AdrjBottomBar(current) { dest ->
                            nav.navigate(dest.route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                }
            ) { pad ->
                Box(Modifier.fillMaxSize().padding(pad)) {
                    IosGlassBackground {
                        NavHost(navController = nav, startDestination = "home") {
                            composable("home")     { HomeScreen() }
                            composable("search")   { SearchScreen() }
                            composable("library")  { LibraryScreen() }
                            composable("settings") { SettingsScreen(settings, onSettingsChange) }
                        }
                    }
                }
            }
        }
    }
}
