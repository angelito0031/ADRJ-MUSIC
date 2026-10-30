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
import com.adrj.music.updates.UpdateChecker
import com.adrj.music.updates.UpdateInfo
import com.adrj.music.updates.UpdateModal

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // FIX: Forzar a R8 a mantener las clases de updates
        // (R8 las elimina porque no puede rastrear su uso desde Compose)
        try {
            val clazz = com.adrj.music.updates.UpdateChecker::class.java
            val clazz2 = com.adrj.music.updates.UpdateInfo::class.java
            val clazz3 = Class.forName("com.adrj.music.updates.UpdateModalKt")
            android.util.Log.d("ADRJ", "Updates classes loaded: ${clazz.simpleName}, ${clazz2.simpleName}, ${clazz3.simpleName}")
        } catch (e: Throwable) {
            android.util.Log.e("ADRJ", "Updates classes MISSING: ${e.message}")
        }
        
        enableEdgeToEdge()
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // DIAGNÓSTICO: Toast al arrancar
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                android.widget.Toast.makeText(this, "App iniciada", android.widget.Toast.LENGTH_SHORT).show()
            } catch (_: Throwable) {}
        }, 500)

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

            // Estado de actualizaciones
            var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
            var updateDismissed by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                android.widget.Toast.makeText(ctx, "Check iniciado...", android.widget.Toast.LENGTH_SHORT).show()
                try {
                    val result = UpdateChecker.check(ctx, force = true)
                    if (result != null) {
                        android.widget.Toast.makeText(ctx, "Update: v${result.latestVersionName}", android.widget.Toast.LENGTH_LONG).show()
                    } else {
                        android.widget.Toast.makeText(ctx, "Sin update (null)", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    updateInfo = result
                } catch (e: Throwable) {
                    android.widget.Toast.makeText(ctx, "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
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

            // Modal de actualización (encima de todo, fuera del tema)
            if (updateInfo != null && !updateDismissed) {
                UpdateModal(
                    info = updateInfo!!,
                    onDismiss = { updateDismissed = true }
                )
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
