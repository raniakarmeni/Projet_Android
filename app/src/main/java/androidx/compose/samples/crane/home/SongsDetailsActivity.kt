package androidx.compose.samples.crane.home

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.samples.crane.data.KaraokeLine
import androidx.compose.samples.crane.data.Lyrics
import androidx.compose.samples.crane.ui.CraneTheme
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SongDetailsActivity : ComponentActivity() {
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val songName = intent.getStringExtra("SONG_NAME") ?: "Unknown Song"
        val lyricsUrl = intent.getStringExtra("SONG_LYRICS_URL") ?: ""

        // Initialiser le player ici au niveau de l'activité
        player = ExoPlayer.Builder(this).build()

        setContent {
            CraneTheme {
                LyricsScreen(songName = songName, lyricsUrl = lyricsUrl, player = player)
            }
        }

        // Ajouter l'écouteur pour le bouton retour
        onBackPressedDispatcher.addCallback(this) {
            stopMusic()
            finish()  // Termine l'activité après avoir arrêté la musique
        }
    }

    private fun stopMusic() {
        player?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        player = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMusic() // Garantir que la musique soit arrêtée si l'activité est détruite
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun LyricsScreen(songName: String, lyricsUrl: String, player: ExoPlayer?) {
    var lyrics by remember { mutableStateOf<Lyrics?>(null) }
    var currentLine by remember { mutableStateOf<KaraokeLine?>(null) }
    var soundtrackUrl by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val exoPlayer = rememberUpdatedState(player)

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(lyricsUrl) {
        coroutineScope.launch(Dispatchers.IO) {
            val result = loadLyricsFromPath(lyricsUrl)
            lyrics = result.first
            soundtrackUrl = result.second

            // Démarrer la lecture de la chanson si l'URL est disponible
            result.second?.let { url ->
                withContext(Dispatchers.Main) {
                    // Utiliser l'instance de player
                    exoPlayer.value?.let { playSong(it, url) }
                }
            }
        }
    }

    LaunchedEffect(lyrics) {
        lyrics?.lyrics?.let { lines ->
            for (line in lines) {
                currentLine = line
                delay(2000)
            }
        }
    }

    Scaffold(
        topBar = {
            androidx.compose.material.TopAppBar(
                title = { Text("Now playing: $songName") },
                backgroundColor = androidx.compose.ui.graphics.Color.Black
            )
        }
    ) {
        currentLine?.text?.let { line ->
            Text(
                text = line,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                style = androidx.compose.material.MaterialTheme.typography.h5
            )
        } ?: Text("Loading lyrics...", modifier = Modifier.fillMaxSize())
    }
}

@OptIn(UnstableApi::class)
fun playSong(player: ExoPlayer, songPath : String) {
    val mediaItem = MediaItem.fromUri(songPath)
    player.setMediaItem(mediaItem)
    player.prepare()
    player.play()
}





suspend fun loadLyricsFromPath(path: String): Pair<Lyrics?, String?> {
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
        minutes * 60 + seconds
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
