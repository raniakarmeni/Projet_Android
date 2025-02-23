/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.enssat.singwithme.marteil_karamani.base

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import fr.enssat.singwithme.marteil_karamani.ui.BottomSheetShape
import fr.enssat.singwithme.marteil_karamani.ui.karaoke_caption
import fr.enssat.singwithme.marteil_karamani.ui.karaoke_divider_color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.material.FloatingActionButton
import androidx.compose.runtime.collectAsState
import fr.enssat.singwithme.marteil_karamani.data.SongModel
import fr.enssat.singwithme.marteil_karamani.home.MainViewModel
import fr.enssat.singwithme.marteil_karamani.home.OnSongItemClicked
import androidx.hilt.navigation.compose.hiltViewModel


@Composable
fun SongSection(
    modifier: Modifier = Modifier,
    title: String,
    onItemClicked: OnSongItemClicked,
    mainViewModel: MainViewModel // Utilise ton ViewModel ici
) {
    // Observer l'état des chansons depuis le ViewModel
    val songList by mainViewModel.songs.collectAsState(initial = emptyList())

    Surface(modifier = modifier.fillMaxSize(), color = Color.White, shape = BottomSheetShape) {
        Column(modifier = Modifier.padding(start = 24.dp, top = 20.dp, end = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.caption.copy(color = karaoke_caption),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Mettre à jour la playlist",
                    style = MaterialTheme.typography.button.copy(color = MaterialTheme.colors.primary),
                    modifier = Modifier
                        .clickable {
                            // Mettre à jour la playlist en appelant fetchSongsNetwork() dans le ViewModel
                            mainViewModel.fetchSongsNetwork()
                        }
                        .padding(8.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.weight(1f)) {
                val listState = rememberLazyListState()
                SongList(songList, onItemClicked, listState = listState)

                val showButton by remember {
                    derivedStateOf {
                        listState.firstVisibleItemIndex > 0
                    }
                }
                if (showButton) {
                    val coroutineScope = rememberCoroutineScope()
                    FloatingActionButton(
                        backgroundColor = MaterialTheme.colors.primary,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .navigationBarsPadding()
                            .padding(bottom = 8.dp),
                        onClick = {
                            coroutineScope.launch {
                                listState.scrollToItem(0)
                            }
                        }
                    ) {
                        Text("Up!")
                    }
                }
            }
        }
    }
}


@Composable
fun SongSectionWrapper(
    modifier: Modifier = Modifier,
    title: String,
    onItemClicked: OnSongItemClicked
) {
    // Utilisation du ViewModel avec Hilt
    val mainViewModel: MainViewModel = hiltViewModel()

    SongSection(
        modifier = modifier,
        title = title,
        mainViewModel = mainViewModel,
        onItemClicked = onItemClicked
    )
}



@Composable
private fun SongList(
    songList: List<SongModel>,
    onItemClicked: OnSongItemClicked,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = WindowInsets.navigationBars.asPaddingValues(),
        state = listState
    ) {
        items(songList) { songItem ->
            Column(Modifier.fillParentMaxWidth()) {
                SongItem(
                    modifier = Modifier.fillParentMaxWidth(),
                    item = songItem,
                    onItemClicked = onItemClicked
                )
                Divider(color = karaoke_divider_color)
            }
        }
    }
}

@Composable
private fun SongItem(
    modifier: Modifier = Modifier,
    item: SongModel,
    onItemClicked: OnSongItemClicked
) {
    Row(
        modifier = modifier
            .clickable { onItemClicked(item) }
            .padding(top = 12.dp, bottom = 12.dp)
    ) {
        Spacer(Modifier.width(24.dp))
        Column {
            Text(
                text = item.name,
                style = MaterialTheme.typography.h6
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = item.artist,
                style = MaterialTheme.typography.caption.copy(color = karaoke_caption)
            )
        }
    }
}
