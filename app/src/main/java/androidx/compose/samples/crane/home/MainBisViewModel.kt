package androidx.compose.samples.crane.home

import androidx.compose.samples.crane.data.Song
import androidx.compose.samples.crane.data.SongsRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MainBisViewModel @Inject constructor(
    private val repository: SongsRepository
) : ViewModel() {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> get() = _songs

    init {
        fetchSongs()
    }

    private fun fetchSongs() {
        viewModelScope.launch {
            val fetchedSongs = repository.getSongs()
            _songs.value = fetchedSongs
        }
    }
}