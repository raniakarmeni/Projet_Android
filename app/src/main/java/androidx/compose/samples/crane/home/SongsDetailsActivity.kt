package androidx.compose.samples.crane.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.samples.crane.data.KaraokeLine
import androidx.compose.samples.crane.data.Lyrics
import androidx.compose.samples.crane.ui.CraneTheme
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class SongDetailsActivity : ComponentActivity() {
    private var player: ExoPlayer? = null
    private var savedPosition: Long = 0L // Pour sauvegarder la position actuelle de lecture

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val songName = intent.getStringExtra("SONG_NAME") ?: "Unknown Song"
        val lyricsUrl = intent.getStringExtra("SONG_LYRICS_URL") ?: ""

        // Initialiser le player ici au niveau de l’activité
        player = ExoPlayer.Builder(this).build()

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                super.onPause(owner)
                savedPosition = player?.currentPosition ?: 0L
                player?.setPlayWhenReady(false) // Met en pause la lecture
                Log.d("SongDetailsActivity", "Player paused, saved position: $savedPosition")
            }

            override fun onResume(owner: LifecycleOwner) {
                super.onResume(owner)
                player?.seekTo(savedPosition) // Reprendre à la position sauvegardée
                player?.setPlayWhenReady(true) // Reprend la lecture
                Log.d("SongDetailsActivity", "Player resumed, position restored: $savedPosition")
            }
        })

        setContent {
            CraneTheme {
                LyricsScreen(songName = songName, lyricsUrl = lyricsUrl, player = player)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        Log.d("SongDetailsActivity", "Player released onDestroy")
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun LyricsScreen(songName: String, lyricsUrl: String, player: ExoPlayer?) {
    var lyrics by remember { mutableStateOf<Lyrics?>(null) }
    var currentLine by remember { mutableStateOf<KaraokeLine?>(null) }
    var songListenedTime by remember { mutableLongStateOf(0L) }
    var isInPause by remember { mutableStateOf(false) }
    var dots by remember { mutableStateOf("") }
    var cursorProgress by remember { mutableStateOf(0f) } // Progression initiale du curseur

    val coroutineScope = rememberCoroutineScope()

    // Charger les paroles et démarrer la musique
    LaunchedEffect(lyricsUrl) {
        coroutineScope.launch(Dispatchers.IO) {
            val result = loadLyricsFromPath(lyricsUrl)
            lyrics = result.first

            result.second?.let { url ->
                withContext(Dispatchers.Main) {
                    player?.let { playSong(it, url) }
                }
            }
        }
    }

    // Suivi du temps de la chanson
    LaunchedEffect(player) {
        while (true) {
            delay(16)
            songListenedTime = player?.currentPosition?.div(1000) ?: 0L
            Log.d("LyricsScreen", "Current song time: $songListenedTime")
        }
    }

    // Animation des points de suspension pendant la pause
    LaunchedEffect(isInPause) {
        while (isInPause) {
            dots = when (dots) {
                "" -> "."
                "." -> ".."
                ".." -> "..."
                else -> ""
            }
            delay(500)
        }
    }

    // Synchronisation des paroles et calcul du curseur
    LaunchedEffect(songListenedTime, lyrics) {
        lyrics?.lyrics?.let { lines ->
            val filteredLines = lines.filter { it.timestamp <= songListenedTime.toInt() }
            currentLine = filteredLines.maxByOrNull { it.timestamp }

            // Calcul du curseur en fonction du temps de la chanson et de la ligne actuelle
            currentLine?.let { line ->
                val progress = (songListenedTime - line.timestamp).toFloat() / 5f // Ajustez le "5f" pour correspondre à la vitesse
                cursorProgress = progress.coerceIn(0f, 1f)
            }

            Log.d(
                "LyricsScreen",
                "Current line: ${currentLine?.timestamp} - ${currentLine?.text}, cursor progress: $cursorProgress"
            )
        }
    }

    // Animation fluide du curseur
    val animatedCursorProgress by animateFloatAsState(
        targetValue = cursorProgress,
        animationSpec = TweenSpec(durationMillis = 100) // Vous pouvez ajuster la durée de l'animation
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Now playing: $songName") },
                backgroundColor = Color.Black
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (isInPause) {
                // Afficher les "..." dynamiques pendant la pause
                Text(
                    text = dots,
                    modifier = Modifier.align(androidx.compose.ui.Alignment.Center),
                    style = androidx.compose.material.MaterialTheme.typography.h6,
                    color = Color.Gray
                )
            } else {
                // Affichage de la ligne actuelle avec le curseur animé
                currentLine?.text?.let { line ->
                    val splitIndex = (animatedCursorProgress * line.length).toInt() // Utiliser la version animée de cursorProgress
                    val readText = line.substring(0, splitIndex.coerceIn(0, line.length))
                    val unreadText = line.substring(splitIndex.coerceIn(0, line.length))

                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(color = Color.Black)) {
                                append(readText) // Texte déjà lu
                            }
                            withStyle(style = SpanStyle(color = Color.Red)) {
                                append(unreadText) // Texte non lu
                            }
                        },
                        style = androidx.compose.material.MaterialTheme.typography.h5,
                        modifier = Modifier.align(androidx.compose.ui.Alignment.Center)
                    )
                } ?: Text(
                    text = "Loading lyrics...",
                    modifier = Modifier.align(androidx.compose.ui.Alignment.Center),
                    style = androidx.compose.material.MaterialTheme.typography.h6,
                    color = Color.Gray
                )
            }
        }
    }
}




fun playSong(player: ExoPlayer, songPath: String) {
    val mediaItem = MediaItem.fromUri(songPath)
    player.setMediaItem(mediaItem)
    player.prepare()
    player.play()
}

fun loadLyricsFromPath(path: String): Pair<Lyrics?, String?> {
    return try {
        val content = java.net.URL(path).readText()
        var soundtrackPath: String? = null

        // Rechercher le nom de la chanson dans le contenu
        val regex = Regex("# soundtrack\\s+(.*)")
        val matchResult = regex.find(content)

        if (matchResult != null) {
            val soundtrack = matchResult.groupValues[1]
            println("Nom du fichier MP3 : $soundtrack")

            // Construire le chemin complet du fichier MP3
            soundtrackPath = path.substringBeforeLast("/") + "/$soundtrack"
            println("Nouveau path : $soundtrackPath")
        } else {
            println("Aucun fichier MP3 trouvé.")
        }

        println("Lyrics content loaded: $content") // Log pour vérifier le contenu
        Pair(parseLyrics(content), soundtrackPath)
    } catch (e: Exception) {
        e.printStackTrace()
        Pair(null, null)
    }
}

fun reformatLyrics(content: String): String {
    val regex = Regex("\\}(\\s*\\{)") // Trouve les cas où "}" est suivi par "{"
    return content.replace(regex, "}\n{") // Ajoute un saut de ligne entre "}" et "{"
}

fun parseTimestamp(timestamp: String): Float {
    val parts = timestamp.split(":")
    return if (parts.size == 2) {
        val minutes = parts[0].toIntOrNull() ?: throw IllegalArgumentException("Invalid minutes in timestamp: $timestamp")
        val seconds = parts[1].toFloatOrNull() ?: throw IllegalArgumentException("Invalid seconds in timestamp: $timestamp")
        minutes.toFloat() * 60 + seconds // Convertir minutes en Float avant d'ajouter
    } else {
        throw IllegalArgumentException("Invalid timestamp format: $timestamp")
    }
}

fun parseLyrics(content: String): Lyrics {
    // Prétraitement pour réorganiser les lignes
    val formattedContent = reformatLyrics(content)
    println("Formatted content:\n$formattedContent")

    val lines = formattedContent.lines()
        .filter { it.contains("{") && it.contains("}") } // Filtrer les lignes avec des timestamps
        .mapNotNull { line ->
            try {
                val firstTimestamp = line.substringAfter("{").substringBefore("}").trim()
                val timestamp = parseTimestamp(firstTimestamp) // Convertir en secondes
                // Retirer les accolades restantes et extraire le texte nettoyé
                val text = line.replace(Regex("\\{.*?\\}"), "").trim()
                println("Parsed line -> Timestamp: $timestamp, Text: $text")
                KaraokeLine(timestamp, text)
            } catch (e: Exception) {
                println("Error parsing line: $line, Exception: ${e.message}")
                null
            }
        }

    return Lyrics(
        title = "Parsed Title",
        author = "Parsed Author",
        soundtrack = "Parsed Soundtrack",
        lyrics = lines
    )
}
