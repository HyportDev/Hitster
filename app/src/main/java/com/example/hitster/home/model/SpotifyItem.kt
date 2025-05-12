package com.example.hitster.home.model

import androidx.annotation.StringRes
import com.example.hitster.R

abstract class SpotifyItem(@StringRes val title: Int)

data object SpotifyLoading : SpotifyItem(title = R.string.home_spotifyLoading)
data object SpotifyDisconnected : SpotifyItem(title = R.string.home_spotifyDisconnected)
data object SpotifyConnected : SpotifyItem(title = R.string.home_spotifyConnected)
