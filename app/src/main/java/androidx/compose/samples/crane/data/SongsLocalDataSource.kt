package androidx.compose.samples.crane.data

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Annotated with Singleton as the class created a lot of objects.
 */
@Singleton
class SongsLocalDataSource @Inject constructor() {

    var songs = listOf(
        HEYJUDE, BOHEMIANRHASPODY, GUNS
    )
}