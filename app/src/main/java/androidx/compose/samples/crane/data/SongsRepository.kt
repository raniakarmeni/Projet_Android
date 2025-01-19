package androidx.compose.samples.crane.data

import javax.inject.Inject

class SongsRepository @Inject constructor(
    private val songsLocalDataSource: SongsLocalDataSource
) {
    // Combiner les chansons locales et celles récupérées dynamiquement
    suspend fun getSongs(): List<SongModel> {
        val songs = songsLocalDataSource.fetchSongs()
        return if (songs.isNotEmpty()) {
            songs
        } else {
            songsLocalDataSource.songs // Fallback aux chansons locales
        }
    }

    suspend fun getSongsNetwork(): List<SongModel> {
        val networkSongs = songsLocalDataSource.fetchSongsFromNetwork()
        return if (networkSongs.isNotEmpty()) {
            networkSongs
        } else {
            songsLocalDataSource.songs // Fallback aux chansons locales
        }
    }
}