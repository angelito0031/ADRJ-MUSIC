package com.adrj.music.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adrj.music.data.MoodLearning
import com.adrj.music.data.PrefsState
import com.adrj.music.ui.theme.GrisClaro

data class MoodCategory(
    val name: String,
    val query: String,
    val icon: ImageVector,
    val colors: List<Color>
)

val POPULAR_ARTISTS = listOf(
    "Bad Bunny", "Feid", "Karol G", "Rauw Alejandro", "Peso Pluma",
    "Taylor Swift", "Drake", "The Weeknd", "Billie Eilish", "Dua Lipa",
    "Shakira", "Maluma", "J Balvin", "Rosalía", "Anuel AA",
    "Ed Sheeran", "Bruno Mars", "Ariana Grande", "Post Malone", "Coldplay"
)

val MOODS = listOf(
    MoodCategory("Feliz", "música feliz hits", Icons.Filled.SentimentVerySatisfied,
        listOf(Color(0xFFFFB300), Color(0xFFFF6F00))),
    MoodCategory("Triste", "baladas tristes", Icons.Filled.SentimentDissatisfied,
        listOf(Color(0xFF42A5F5), Color(0xFF1565C0))),
    MoodCategory("Relajado", "lofi chill music", Icons.Filled.SelfImprovement,
        listOf(Color(0xFF66BB6A), Color(0xFF2E7D32))),
    MoodCategory("Energético", "workout music mix", Icons.Filled.FitnessCenter,
        listOf(Color(0xFFE53935), Color(0xFFB71C1C))),
    MoodCategory("Enamorado", "love songs romanticas", Icons.Filled.Favorite,
        listOf(Color(0xFFEC407A), Color(0xFFAD1457))),
    MoodCategory("Concentrado", "focus study music", Icons.Filled.Psychology,
        listOf(Color(0xFFAB47BC), Color(0xFF6A1B9A))),
    MoodCategory("Fiesta", "party hits 2024", Icons.Filled.Celebration,
        listOf(Color(0xFFFF7043), Color(0xFFD84315))),
    MoodCategory("Dormir", "sleep music relax", Icons.Filled.Bedtime,
        listOf(Color(0xFF5C6BC0), Color(0xFF283593))),
    MoodCategory("Rock", "rock clasico hits", Icons.Filled.ElectricBolt,
        listOf(Color(0xFF795548), Color(0xFF3E2723))),
    MoodCategory("Latino", "reggaeton latino hits", Icons.Filled.MusicNote,
        listOf(Color(0xFFFFCA28), Color(0xFFF57F17))),
    MoodCategory("Pop", "pop hits español", Icons.Filled.Star,
        listOf(Color(0xFF26C6DA), Color(0xFF00838F))),
    MoodCategory("Indie", "indie alternativo mix", Icons.Filled.LocalCafe,
        listOf(Color(0xFF8D6E63), Color(0xFF4E342E)))
)

@Composable
fun MoodScreen(
    onMoodSelected: (MoodCategory) -> Unit,
    onArtistMoodSelected: (String, MoodCategory?) -> Unit,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(0) }

    // Para la pestaña de artista
    var selectedArtist by remember { mutableStateOf<String?>(null) }
    var artistQuery by remember { mutableStateOf("") }

    // FIX BUG 8: getTopMoods / getRecentArtists en IO con estado reactivo
    var topMoodNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var recentArtists by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            topMoodNames = MoodLearning.getTopMoods(ctx)
            recentArtists = MoodLearning.getRecentArtists(ctx)
        }
    }
    val orderedMoods = remember(topMoodNames) {
        if (topMoodNames.isEmpty()) MOODS
        else {
            val top = topMoodNames.mapNotNull { name -> MOODS.find { it.name == name } }
            val rest = MOODS.filter { it !in top }
            top + rest
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (tab == 1 && selectedArtist != null) {
                    selectedArtist = null  // volver a elegir artista
                } else {
                    onBack()
                }
            }) {
                Icon(Icons.Filled.ArrowBack, "Atrás",
                    tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                when {
                    tab == 1 && selectedArtist != null -> "$selectedArtist — ¿Cómo te sientes?"
                    else -> "Descubre música"
                },
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
        }

        // Tabs
        TabRow(
            selectedTabIndex = tab,
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground
        ) {
            Tab(
                selected = tab == 0,
                onClick = { tab = 0; selectedArtist = null },
                text = { Text("Por ánimo", maxLines = 1) },
                icon = { Icon(Icons.Filled.AutoAwesome, null,
                    modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = tab == 1,
                onClick = { tab = 1; selectedArtist = null },
                text = { Text("Por artista", maxLines = 1) },
                icon = { Icon(Icons.Filled.Person, null,
                    modifier = Modifier.size(18.dp)) }
            )
        }

        Spacer(Modifier.height(8.dp))

        when {
            // === PESTAÑA 1: POR ÁNIMO ===
            tab == 0 -> {
                Text("¿Cómo estás hoy?",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                Text(
                    if (topMoodNames.isEmpty())
                        "Elige un estado de ánimo para descubrir música"
                    else
                        "Basado en lo que más escuchas",
                    color = GrisClaro,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(orderedMoods, key = { it.name }) { mood ->
                        MoodTile(mood) {
                            scope.launch(Dispatchers.IO) {
                                MoodLearning.incrementMood(ctx, mood.name)
                            }
                            onMoodSelected(mood)
                        }
                    }
                    item { Spacer(Modifier.height(160.dp)) }
                }
            }

            // === PESTAÑA 2: POR ARTISTA (paso 1 = elegir artista) ===
            tab == 1 && selectedArtist == null -> {
                ArtistPicker(
                    query = artistQuery,
                    onQueryChange = { artistQuery = it },
                    recentArtists = recentArtists,
                    onArtistChosen = { artist ->
                        scope.launch(Dispatchers.IO) {
                            MoodLearning.addRecentArtist(ctx, artist)
                        }
                        selectedArtist = artist
                    }
                )
            }

            // === PESTAÑA 2: POR ARTISTA (paso 2 = elegir mood) ===
            tab == 1 && selectedArtist != null -> {
                val artistName = selectedArtist ?: ""
                Text("¿Cómo te sientes con $artistName?",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                Text("O elige 'Todos' para música general",
                    color = GrisClaro,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Opción "Todos"
                    item {
                        MoodTile(
                            MoodCategory(
                                "Todos", "", Icons.Filled.QueueMusic,
                                listOf(Color(0xFF616161), Color(0xFF212121))
                            )
                        ) {
                            onArtistMoodSelected(artistName, null)
                        }
                    }
                    items(MOODS, key = { it.name }) { mood ->
                        MoodTile(mood) {
                            onArtistMoodSelected(artistName, mood)
                        }
                    }
                    item { Spacer(Modifier.height(160.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ArtistPicker(
    query: String,
    onQueryChange: (String) -> Unit,
    recentArtists: List<String>,
    onArtistChosen: (String) -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        Text("Elige un artista o escribe el tuyo",
            color = GrisClaro,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium)

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            placeholder = { Text("Nombre del artista...", color = GrisClaro) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = GrisClaro) },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { onArtistChosen(query) }) {
                        Icon(Icons.Filled.ArrowForward, "Continuar",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )

        if (recentArtists.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text("Recientes",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(8.dp))
            ArtistChips(recentArtists, onArtistChosen)
        }

        Spacer(Modifier.height(20.dp))
        Text("Artistas populares",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(8.dp))
        ArtistChips(POPULAR_ARTISTS, onArtistChosen)

        Spacer(Modifier.height(160.dp))
    }
}

@Composable
private fun ArtistChips(artists: List<String>, onArtistChosen: (String) -> Unit) {
    Column(
        Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        artists.chunked(2).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { artist ->
                    Surface(
                        onClick = { onArtistChosen(artist) },
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Person, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(artist,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1)
                        }
                    }
                }
                if (row.size < 2) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MoodTile(mood: MoodCategory, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(mood.colors))
            .then(
                if (PrefsState.glassEnabled) Modifier.border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(listOf(
                        Color.White.copy(alpha = 0.60f),
                        Color.White.copy(alpha = 0.10f),
                        Color.Transparent
                    )),
                    shape = RoundedCornerShape(24.dp)
                ) else Modifier
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(mood.icon, null,
                tint = Color.White,
                modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(6.dp))
            Text(mood.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center)
        }
    }
}
