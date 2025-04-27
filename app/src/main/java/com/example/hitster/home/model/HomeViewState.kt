package com.example.hitster.home.model

data class HomeViewState(
    val players: List<String> = emptyList(),
    val playlists: List<PlaylistViewState> = emptyList(),
    val dialogState: HomeDialogState = HomeDialogState.Closed,
    val playerInputState: HomePlayerInputState = HomePlayerInputState.Closed,
    val spotifyItem: SpotifyItem = SpotifyLoading
)
