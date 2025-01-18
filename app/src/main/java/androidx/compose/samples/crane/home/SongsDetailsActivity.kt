package androidx.compose.samples.crane.home

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

class SongDetailsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val songName = intent.getStringExtra("SONG_NAME") ?: "Unknown Song"
        val lyricsUrl = intent.getStringExtra("SONG_LYRICS_URL") ?: ""

        setContent {
            CraneTheme {
                LyricsScreen(
                    songName = songName,
                    lyricsUrl = lyricsUrl,
                    onFinish = { finish() } // Fermer l'activité à la fin de la chanson
                )
            }
        }
    }
}@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun LyricsScreen(songName: String, lyricsUrl: String, onFinish: () -> Unit) {
    var lyrics by remember { mutableStateOf<Lyrics?>(null) }
    var currentLine by remember { mutableStateOf<KaraokeLine?>(null) }
    var isInPause by remember { mutableStateOf(false) } // Indique si on est dans une pause
    var dots by remember { mutableStateOf("") } // Pour afficher les "..." dynamiques
    var cursorProgress by remember { mutableStateOf(0f) } // Position du curseur dans la ligne
    val coroutineScope = rememberCoroutineScope()

    // Charger les paroles en arrière-plan
    LaunchedEffect(lyricsUrl) {
        coroutineScope.launch(Dispatchers.IO) {
            lyrics = loadLyricsFromPath(lyricsUrl)
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
            delay(500) // Intervalle entre les changements de points
        }
    }

    // Afficher les lignes une par une avec un délai
    LaunchedEffect(lyrics) {
        lyrics?.lyrics?.let { lines ->
            for (i in lines.indices) {
                val line = lines[i]
                val nextLineTimestamp = if (i + 1 < lines.size) lines[i + 1].timestamp else line.timestamp + 3 // Durée par défaut pour la dernière ligne
                val duration = nextLineTimestamp - line.timestamp // Durée pour afficher la ligne
                val gapDuration = nextLineTimestamp - (line.timestamp + duration) // Écart entre les lignes

                isInPause = false // Pas de pause pendant la lecture de la ligne
                currentLine = line

                // Progression du curseur sur la ligne
                for (progress in 0..100) { // Curseur progresse de 0 à 100 %
                    cursorProgress = progress / 100f
                    delay((duration * 10).toLong()) // Ajuste la vitesse pour la durée de la ligne
                }
                cursorProgress = 0f // Réinitialiser le curseur pour la ligne suivante

                // Respecter l'écart avant la prochaine ligne, si gapDuration > 0
                if (gapDuration > 0) {
                    isInPause = true // Activer le mode pause
                    delay((gapDuration * 1000).toLong()) // Convertir l'écart en millisecondes
                }
            }

            // Une fois la chanson terminée, revenir à l'accueil
            onFinish()
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
                    color = androidx.compose.ui.graphics.Color.Gray
                )
            } else {
                // Affichage de la ligne actuelle avec le curseur
                currentLine?.text?.let { line ->
                    val splitIndex = (cursorProgress * line.length).toInt()
                    val readText = line.substring(0, splitIndex.coerceIn(0, line.length))
                    val unreadText = line.substring(splitIndex.coerceIn(0, line.length))

                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(color = androidx.compose.ui.graphics.Color.Black)) {
                                append(readText) // Texte déjà lu
                            }
                            withStyle(style = SpanStyle(color = androidx.compose.ui.graphics.Color.Red)) {
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
                    color = androidx.compose.ui.graphics.Color.Gray
                )
            }
        }
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
