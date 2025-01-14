package androidx.compose.samples.crane.base

import android.content.Context
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer


@OptIn(UnstableApi::class)
fun SongPlayer(context: Context)
{
    val videoUri = "https://gcpa-enssat-24-25.s3.eu-west-3.amazonaws.com/Bohemian/Bohemian.mp3"
    val player = ExoPlayer.Builder(context)
        .setLooper(Looper.getMainLooper())
        .build()
    // Build the media item
    val mediaItem = MediaItem.fromUri(videoUri)
    // Set the media item to be played.
    player.setMediaItem(mediaItem)
    // Prepare the player.
    player.prepare()
    // Start the playback.
    player.play()
}