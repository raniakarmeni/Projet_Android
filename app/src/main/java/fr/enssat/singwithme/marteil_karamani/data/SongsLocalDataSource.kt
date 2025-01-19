package fr.enssat.singwithme.marteil_karamani.data

import SongModelJsonAdapter
import android.util.Log
import fr.enssat.singwithme.marteil_karamani.util.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import javax.inject.Inject
import javax.inject.Singleton
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
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

    suspend fun fetchSongs(): List<SongModel> {
        // 1. Vérifier si la playlist est dans le cache
        val cachedPlaylist = getCachedPlaylist()
        if (cachedPlaylist != null) {
            // Si elle existe dans le cache, la retourner
            Log.d("SongsLocalDataSource", "Returning cached playlist")
            return cachedPlaylist
        }

        // 2. Si la playlist n'est pas dans le cache, la récupérer depuis le réseau
        Log.d("SongsLocalDataSource", "Fetching playlist from network...")
        val playlistFromNetwork = fetchSongsFromNetwork()

        return playlistFromNetwork
    }

    private fun cachePlaylist(playlist: List<SongModel>) {
        val playlistType = Types.newParameterizedType(List::class.java, SongModel::class.java)
        val moshiAdapter = moshi.adapter<List<SongModel>>(playlistType)
        val playlistJson = moshiAdapter.toJson(playlist)
        PreferencesManager.putString("cached_playlist", playlistJson)
    }

    private fun getCachedPlaylist(): List<SongModel>? {
        val playlistType = Types.newParameterizedType(List::class.java, SongModel::class.java)
        val moshiAdapter = moshi.adapter<List<SongModel>>(playlistType)
        val cachedJson = PreferencesManager.getString("cached_playlist")
        return if (cachedJson != null) {
            try {
                moshiAdapter.fromJson(cachedJson)
            } catch (e: Exception) {
                Log.e("SongsLocalDataSource", "Error parsing cached playlist", e)
                null
            }
        } else {
            null
        }
    }

    suspend fun fetchSongsFromNetwork(): List<SongModel> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("SongsLocalDataSource", "Fetching playlist from network...")
                val playlist = api.fetchPlaylist()
                val filteredPlaylist = playlist.filter { it.path != null }
                Log.d("SongsLocalDataSource", "Fetched ${filteredPlaylist.size} songs")
                cachePlaylist(filteredPlaylist)
                filteredPlaylist
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