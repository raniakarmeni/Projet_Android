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

package fr.enssat.singwithme.marteil_karamani.home

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.BackdropScaffold
import androidx.compose.material.BackdropValue
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.rememberBackdropScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import fr.enssat.singwithme.marteil_karamani.base.KaraokeDrawer
import fr.enssat.singwithme.marteil_karamani.base.SongSectionWrapper
import fr.enssat.singwithme.marteil_karamani.data.SongModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

typealias OnSongItemClicked = (SongModel) -> Unit


@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun KaraokeHome(
    onSongItemClicked: OnSongItemClicked,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    val scaffoldState = rememberScaffoldState()
    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier.statusBarsPadding(),
        drawerContent = {
            KaraokeDrawer()
        }
    ) {
        KaraokeHomeContent(
            modifier = modifier,
            onSongItemClicked = onSongItemClicked,
            viewModel = viewModel // Passer le ViewModel ici
        )
    }
}


fun openSongDetails(context: Context, song: SongModel) {
    val intent = Intent(context, SongDetailsActivity::class.java).apply {
        putExtra("SONG_NAME", song.name)
        putExtra("SONG_LYRICS_URL", "https://gcpa-enssat-24-25.s3.eu-west-3.amazonaws.com/"+song.path)
    }
    context.startActivity(intent)
}

@Composable
fun KaraokeHomeContent(
    onSongItemClicked: OnSongItemClicked,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val songs by viewModel.songs.collectAsStateWithLifecycle()

    BackdropScaffold(
        modifier = modifier,
        scaffoldState = rememberBackdropScaffoldState(BackdropValue.Revealed),
        frontLayerScrimColor = Color.Unspecified,
        appBar = {
            TopAppBar(
                title = { Text(text = "Karaoke Songs") },
                backgroundColor = MaterialTheme.colors.primary
            )
        },
        backLayerContent = {
        },
        frontLayerContent = {
            SongSectionWrapper(
                modifier = Modifier.fillMaxSize(),
                title = "Explore Songs",
                onItemClicked = { song ->
                    openSongDetails(context, song)
                }
            )
        }
    )
}
