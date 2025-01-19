package fr.enssat.singwithme.marteil_kermani.data

data class Lyrics(
    val title: String,
    val author: String,
    val soundtrack: String,
    val lyrics: List<KaraokeLine>
)

data class KaraokeLine(
    val timestamp: Float,
    var text: String,
    val timestamps: List<Pair<Float, String>>
)