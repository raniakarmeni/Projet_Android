package fr.enssat.singwithme.marteil_karamani.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.enssat.singwithme.marteil_karamani.data.KaraokeLine
import fr.enssat.singwithme.marteil_karamani.data.Lyrics
import fr.enssat.singwithme.marteil_karamani.ui.KaraokeTheme
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
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
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.InputStream
import java.net.URL
import androidx.compose.ui.geometry.Offset

class SongDetailsActivity : ComponentActivity() {
    private var player: ExoPlayer? = null
    private var savedPosition: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val songName = intent.getStringExtra("SONG_NAME") ?: "Unknown Song"
        val lyricsUrl = intent.getStringExtra("SONG_LYRICS_URL") ?: ""

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
            KaraokeTheme {
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
    var nextLine by remember { mutableStateOf<KaraokeLine?>(null) }
    var songListenedTime by remember { mutableLongStateOf(0L) }
    var cursorProgress by remember { mutableStateOf(0f) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Charger les paroles et démarrer la musique
    LaunchedEffect(lyricsUrl) {
        coroutineScope.launch(Dispatchers.IO) {
            val result = loadLyricsFromPath(context,lyricsUrl)
            lyrics = result.first

            result.second?.let { url ->
                withContext(Dispatchers.Main) {
                    player?.let { playSong(it, url) }
                }
            }
        }
    }

    // Suivi en temps réel du temps de la chanson
    LaunchedEffect(player) {
        while (true) {
            songListenedTime = player?.currentPosition?.div(100)?.toLong() ?: 0L
            delay(16)
        }
    }

    // Synchronisation des paroles et calcul du curseur
    LaunchedEffect(songListenedTime, lyrics) {
        lyrics?.lyrics?.let { lines ->
            val currentTimestamp = (songListenedTime / 10).toInt() // Convertir en secondes
            val currentIndex = lines.indexOfLast { it.timestamp <= currentTimestamp }

            if (currentIndex != -1) {
                currentLine = lines[currentIndex]
                nextLine = lines.getOrNull(currentIndex + 1)

                val currentLineTimestamp = currentLine!!.timestamp
                val nextLineTimestamp = nextLine?.timestamp ?: currentLineTimestamp + 5

                val lineDuration = (nextLineTimestamp - currentLineTimestamp).toFloat()
                val elapsedTime = (songListenedTime / 10f - currentLineTimestamp).toFloat()

                cursorProgress = (elapsedTime / lineDuration).coerceIn(0f, 1f)
            }
        }
    }

    // Animation fluide du curseur
    val animatedCursorProgress by animateFloatAsState(
        targetValue = cursorProgress,
        animationSpec = TweenSpec(durationMillis = 16)
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
            // Affichage des paroles synchronisées avec curseur
            currentLine?.text?.let { line ->
                val splitIndex = (animatedCursorProgress * line.length).toInt()
                val readText = line.substring(0, splitIndex.coerceIn(0, line.length))
                val unreadText = line.substring(splitIndex.coerceIn(0, line.length))

                Box(
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.Center)
                        .padding(vertical = 16.dp)
                ) {
                    // Mesurer la largeur réelle du texte
                    val textLayoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

                    // Texte des paroles
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(color = Color.Black)) {
                                append(readText)
                            }
                            withStyle(style = SpanStyle(color = Color.Red)) {
                                append(unreadText)
                            }
                        },
                        style = androidx.compose.material.MaterialTheme.typography.h5,
                        onTextLayout = { layoutResult ->
                            textLayoutResult.value = layoutResult
                        }
                    )

                    // Curseur aligné avec les mots
                    textLayoutResult.value?.let { layoutResult ->
                        val textWidth = layoutResult.size.width.toFloat() // Convertir en Float
                        val cursorX = (animatedCursorProgress * textWidth).coerceIn(0f, textWidth) // Limiter au texte

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .padding(top = 5.dp)
                        ) {
                            drawLine(
                                color = Color.Gray,
                                start = Offset(cursorX, 0f),
                                end = Offset(cursorX, size.height),
                                strokeWidth = 6f
                            )
                        }
                    }
                }
            } ?: Text(
                text = "Loading lyrics...",
                modifier = Modifier.align(androidx.compose.ui.Alignment.Center),
                style = androidx.compose.material.MaterialTheme.typography.h6,
                color = Color.Gray
            )
        }
    }
}

fun playSong(player: ExoPlayer, songPath: String) {
    val mediaItem = MediaItem.fromUri(songPath)
    player.setMediaItem(mediaItem)
    player.prepare()
    player.play()
}

fun loadLyricsFromPath(context: Context, path: String): Pair<Lyrics?, String?> {
    return try {
        val content = URL(path).readText()  // Charger le texte des paroles
        var soundtrackPath: String? = null

        val regex = Regex("# soundtrack\\s+(.*)")
        val matchResult = regex.find(content)

        if (matchResult != null) {
            val soundtrack = matchResult.groupValues[1]
            println("Nom du fichier MP3 : $soundtrack")
            // Vérifier si le fichier MP3 est déjà téléchargé localement
            val localFilePath = context.getFileStreamPath(soundtrack).absolutePath

            if (File(localFilePath).exists()) {
                println("Le fichier existe déjà localement.")
                soundtrackPath = localFilePath  // Utiliser le fichier local
            } else {
                println("Le fichier n'existe pas localement, téléchargement en cours...")
                // Télécharger et enregistrer le fichier
                val inputStream: InputStream = URL(path.substringBeforeLast("/") + "/$soundtrack").openStream()
                val outputFile = File(context.filesDir, soundtrack)

                // Sauvegarder le fichier téléchargé localement
                outputFile.outputStream().use { output ->
                    inputStream.copyTo(output)
                    println("Fichier MP3 téléchargé et enregistré à : ${outputFile.absolutePath}")
                }

                soundtrackPath = outputFile.absolutePath  // Chemin du fichier local téléchargé
            }
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
