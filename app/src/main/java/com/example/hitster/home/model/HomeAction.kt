package com.example.hitster.home.model

sealed interface HomeAction {

    data class AddPlayer(val name: String) : HomeAction
    data class RemovePlayer(val name: String) : HomeAction
    data class OnPlayerInputChange(val value: String) : HomeAction

    data class OpenRemovePlayerDialog(val name: String) : HomeAction
    data class OnDialogItemChecked(val item: PlaylistSelectionItem, val checked: Boolean) : HomeAction
    data object DismissDialog : HomeAction

    data object AddPlaylistByLink : HomeAction
    data class AddPlaylistByLinkValueChange(val link: String) : HomeAction
    data object AddPlaylistFromLibrary : HomeAction
    data class AddPlaylist(val playlistUrl: String) : HomeAction
    data class AddPlaylists(val playlists: List<Playlist>) : HomeAction
    data class RemovePlaylist(val playlist: Playlist) : HomeAction
}