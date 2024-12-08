package androidx.compose.samples.crane.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongsLocalDataSource @Inject constructor() {

    var songs = listOf(
        HEYJUDE, BOHEMIANRHASPODY, GUNS
    )

    // Retrofit pour récupérer les chansons en ligne
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://gcpa-enssat-24-25.s3.eu-west-3.amazonaws.com/")
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    private val api = retrofit.create(PlaylistApi::class.java)

    // Méthode pour récupérer les chansons dynamiquement
    suspend fun fetchSongsFromNetwork(): List<Song> {
        return withContext(Dispatchers.IO) {
            try {
                api.fetchPlaylist()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
}

// Interface Retrofit pour l'API
interface PlaylistApi {
    @GET("playlist.json")
    suspend fun fetchPlaylist(): List<Song>
}