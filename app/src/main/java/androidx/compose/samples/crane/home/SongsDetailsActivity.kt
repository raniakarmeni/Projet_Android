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

        setContent {
            CraneTheme {
                LyricsScreen(songName = songName, lyricsUrl = lyricsUrl)
            }
        }

        // Ajouter l'écouteur pour le bouton retour
        onBackPressedDispatcher.addCallback(this) {
            stopMusic()
            super.onBackPressed()
        }
    }
    private fun stopMusic() {
        player?.let {
            it.stop() // Arrêter la lecture de la musique
            it.release() // Libérer les ressources
            player = null
        }
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun LyricsScreen(songName: String, lyricsUrl: String) {
    var lyrics by remember { mutableStateOf<Lyrics?>(null) }
    var currentLine by remember { mutableStateOf<KaraokeLine?>(null) }
    var soundtrackUrl by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Charger les paroles et l'URL de la chanson en arrière-plan
    LaunchedEffect(lyricsUrl) {
        coroutineScope.launch(Dispatchers.IO) {
            val result = loadLyricsFromPath(lyricsUrl)
            lyrics = result.first
            soundtrackUrl = result.second

            // Démarrer la lecture de la chanson si l'URL est disponible
            result.second?.let { url ->
                withContext(Dispatchers.Main) {
                    playSong(context, url)
                }
            }
        }
    }

    // Afficher les lignes une par une avec un délai
    LaunchedEffect(lyrics) {
        lyrics?.lyrics?.let { lines ->
            for (line in lines) {
                currentLine = line
                delay(2000) // Affiche chaque ligne toutes les 2 secondes
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
        // Affichage de la ligne actuelle
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



@OptIn(UnstableApi::class)
fun playSong(context: Context, songPath : String)
{
    val player = ExoPlayer.Builder(context)
        .setLooper(Looper.getMainLooper())
        .build()
    // Build the media item
    val mediaItem = MediaItem.fromUri(songPath)
    // Set the media item to be played.
    player.setMediaItem(mediaItem)
    // Prepare the player.
    player.prepare()
    // Start the playback.
    player.play()
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
