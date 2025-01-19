import fr.enssat.singwithme.marteil_karamani.data.SongModel
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson

class SongModelJsonAdapter {
    @FromJson
    fun fromJson(json: SongModelJson): SongModel {
        return SongModel(
            name = json.name,
            artist = json.artist,
            locked = json.locked ?: false, // Utiliser false comme valeur par défaut
            path = json.path
        )
    }

    @ToJson
    fun toJson(songModel: SongModel): SongModelJson {
        return SongModelJson(
            name = songModel.name,
            artist = songModel.artist,
            locked = songModel.locked,
            path = songModel.path
        )
    }

    data class SongModelJson(
        val name: String,
        val artist: String,
        val locked: Boolean? = false, // Rendre nullable et fournir une valeur par défaut
        val path: String?
    )
}