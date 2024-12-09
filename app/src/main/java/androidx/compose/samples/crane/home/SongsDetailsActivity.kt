package androidx.compose.samples.crane.home

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SongDetailsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val songName = intent.getStringExtra("SONG_NAME") ?: "Unknown Song"
        val lyricsUrl = intent.getStringExtra("SONG_LYRICS_URL") ?: ""

        setContent {
            CraneTheme {
                LyricsScreen(songName = songName, lyricsUrl = lyricsUrl)
            }
        }
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun LyricsScreen(songName: String, lyricsUrl: String) {
    var lyrics by remember { mutableStateOf<Lyrics?>(null) }
    var currentLine by remember { mutableStateOf<KaraokeLine?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Charger les paroles en arrière-plan
    LaunchedEffect(lyricsUrl) {
        coroutineScope.launch(Dispatchers.IO) {
            lyrics = loadLyricsFromPath(lyricsUrl)
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

suspend fun loadLyricsFromPath(path: String): Lyrics? {
    return try {
        val content = java.net.URL(path).readText()
        parseLyrics(content)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Analyse du contenu des paroles
fun parseLyrics(content: String): Lyrics {
    val lines = content.lines()
        .filter { it.contains("{") && it.contains("}") }
        .map { line ->
            val timestamp = line.substringAfter("{").substringBefore("}").toFloat()
            val text = line.substringAfter("}").trim()
            KaraokeLine(timestamp, text)
        }
    return Lyrics(
        title = "Parsed Title",
        author = "Parsed Author",
        soundtrack = "Parsed Soundtrack",
        lyrics = lines
    )
}