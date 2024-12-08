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

package androidx.compose.samples.crane.home

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.BackdropScaffold
import androidx.compose.material.BackdropValue
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberBackdropScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.samples.crane.base.CraneDrawer
import androidx.compose.samples.crane.base.SongSection
import androidx.compose.samples.crane.data.Song
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

typealias OnSongItemClicked = (Song) -> Unit


@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun KaraokeHome(
    onSongItemClicked: OnSongItemClicked,
    modifier: Modifier = Modifier,
) {
    val scaffoldState = rememberScaffoldState()
    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier.statusBarsPadding(),
        drawerContent = {
            CraneDrawer()
        }
    ) {
        KaraokeHomeContent(
            modifier = modifier,
            onSongItemClicked = onSongItemClicked
        )
    }
}

@Composable
fun KaraokeHomeContent(
    onSongItemClicked: OnSongItemClicked,
    modifier: Modifier = Modifier,
    viewModel: MainBisViewModel = viewModel(),
) {
    // TODO Codelab: collectAsStateWithLifecycle step - consume stream of data from the ViewModel
    // val suggestedDestinations: List<ExploreModel> = remember { emptyList() }
    val songs by viewModel.songs.collectAsStateWithLifecycle()

    BackdropScaffold(
        modifier = modifier,
        scaffoldState = rememberBackdropScaffoldState(BackdropValue.Revealed),
        frontLayerScrimColor = Color.Unspecified,
        appBar = {
        },
        backLayerContent = {
        },
        frontLayerContent = {
            SongSection(
                title = "Explore Songs",
                songList = songs,
                onItemClicked = onSongItemClicked
            )
        }
    )
}
