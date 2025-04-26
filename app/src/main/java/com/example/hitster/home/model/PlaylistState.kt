package com.example.hitster.home.model

sealed interface PlaylistViewState {
    data object Loading : PlaylistViewState
    data class Success(val playlist: Playlist) : PlaylistViewState
    data object Error : PlaylistViewState
}