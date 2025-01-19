package fr.enssat.singwithme.marteil_karamani.home

import fr.enssat.singwithme.marteil_karamani.data.SongModel
import fr.enssat.singwithme.marteil_karamani.data.SongsRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: SongsRepository
) : ViewModel() {

    private val _songs = MutableStateFlow<List<SongModel>>(emptyList())
    val songs: StateFlow<List<SongModel>> get() = _songs

    init {
        fetchSongs()
    }

    fun fetchSongs() {
        viewModelScope.launch {
            val fetchedSongs = repository.getSongs()
            _songs.value = fetchedSongs
        }
    }

    fun fetchSongsNetwork() {
        viewModelScope.launch {
            val fetchedSongs = repository.getSongsNetwork()
            _songs.value = fetchedSongs
        }
    }
}