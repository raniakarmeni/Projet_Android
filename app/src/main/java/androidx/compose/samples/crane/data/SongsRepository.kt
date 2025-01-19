package androidx.compose.samples.crane.data

import javax.inject.Inject

class SongsRepository @Inject constructor(
    private val songsLocalDataSource: SongsLocalDataSource
) {
    // Combiner les chansons locales et celles récupérées dynamiquement
    suspend fun getSongs(): List<SongModel> {
        val networkSongs = songsLocalDataSource.fetchSongs()
        return if (networkSongs.isNotEmpty()) {
            networkSongs
        } else {
            songsLocalDataSource.songs // Fallback aux chansons locales
        }
    }

    // Méthode pour trouver une chanson spécifique
    fun getDestination(songName: String): SongModel? {
        return songsLocalDataSource.songs.firstOrNull {
            it.name == songName
        }
    }
}