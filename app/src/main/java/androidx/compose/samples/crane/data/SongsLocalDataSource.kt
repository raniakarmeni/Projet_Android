package androidx.compose.samples.crane.data

import SongModelJsonAdapter
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import javax.inject.Inject
import javax.inject.Singleton
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@Singleton
class SongsLocalDataSource @Inject constructor() {

    var songs = listOf(
        HEYJUDE, BOHEMIANRHASPODY, GUNS
    )

    val moshi = Moshi.Builder()
        .add(SongModelJsonAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()

    val retrofit = Retrofit.Builder()
        .baseUrl("https://gcpa-enssat-24-25.s3.eu-west-3.amazonaws.com/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api = retrofit.create(PlaylistApi::class.java)

    // Méthode pour récupérer les chansons dynamiquement
    suspend fun fetchSongsFromNetwork(): List<SongModel> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("SongsLocalDataSource", "Fetching playlist...")
                val playlist = api.fetchPlaylist()
                Log.d("SongsLocalDataSource", "Fetched ${playlist.size} songs")
                playlist
            } catch (e: Exception) {
                Log.e("SongsLocalDataSource", "Error fetching playlist", e)
                emptyList()
            }
        }
    }

}

// Interface Retrofit pour l'API
interface PlaylistApi {
    @GET("playlist.json")
    suspend fun fetchPlaylist(): List<SongModel>
}