package com.example.hitster.home

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hitster.data.AccessTokenProvider
import com.example.hitster.R
import com.example.hitster.home.model.HomeAction
import com.example.hitster.home.model.HomeAction.*
import com.example.hitster.home.model.HomeDialogState
import com.example.hitster.home.model.HomeDialogState.DeleteDialog
import com.example.hitster.home.model.HomeDialogState.PlaylistDialog
import com.example.hitster.home.model.HomeDialogState.SelectionDialog
import com.example.hitster.home.model.HomePlayerInputState
import com.example.hitster.home.model.HomeViewState
import com.example.hitster.home.model.Playlist
import com.example.hitster.home.model.PlaylistSelectionItem
import com.example.hitster.home.model.PlaylistViewState
import com.example.hitster.res.Text
import com.example.hitster.res.toText
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class HomeViewModel(
    private val accessTokenProvider: AccessTokenProvider
) : ViewModel() {
    private val _viewState = MutableStateFlow(HomeViewState())
    val viewState = _viewState.asStateFlow()

    private val _event = MutableSharedFlow<Int>()
    val event = _event.asSharedFlow()

    private var spotifyPlaylists = emptyList<Playlist>()

    fun onAction(action: HomeAction) {
        when (action) {
            is AddPlayer -> addPlayer(action.name)
            AddPlaylistByLink -> updateAddPlaylistDialog()
            AddPlaylistFromLibrary -> loadUserPlaylists(accessTokenProvider.getAccessToken())
            DismissDialog -> closeDialog()
            is OnPlayerInputChange -> changePlayerInput(action.value)
            is OpenRemovePlayerDialog -> openRemovePlayerDialog(action.name)
            is RemovePlayer -> {
                closeDialog()
                removePlayer(action.name)
            }
            is RemovePlaylist -> removePlaylist(action.playlist)
            is AddPlaylists -> {
                closeDialog()
                addPlaylists(action.playlists)
            }
            is OnDialogItemChecked -> toggleSelectionValue(action.item, action.checked)
            is AddPlaylist -> {
                closeDialog()
                addPlaylist(action.playlistUrl)
            }
            is AddPlaylistByLinkValueChange -> updateAddPlaylistDialog(action.link)
        }
    }

    private fun changePlayerInput(value: String) {
        _viewState.update { it.copy(playerInputState = HomePlayerInputState.Editing(value)) }
    }

    private fun addPlayer(name: String) {
        val trimmedName = name.trim()
        _viewState.update {
            when {
                trimmedName.isBlank() -> it.copy(playerInputState = HomePlayerInputState.Closed)
                it.players.contains(trimmedName) -> {
                    viewModelScope.launch {
                        sendEvent(R.string.home_playerAlreadyAdded)
                    }
                    it
                }
                else -> it.copy(
                    players = it.players + trimmedName,
                    playerInputState = HomePlayerInputState.Closed
                )
            }
        }
    }

    private fun removePlayer(name: String) {
        _viewState.update { it.copy(players = it.players - name) }
    }

    private fun openRemovePlayerDialog(name: String) {
        _viewState.update {
            it.copy(dialogState = DeleteDialog(
                text = Text.Resource(R.string.home_deletePlayer, name),
                action = RemovePlayer(name)
            )
            )
        }
    }

    private fun updateAddPlaylistDialog(value: String = "") {
        _viewState.update {
            it.copy(dialogState = PlaylistDialog(
                text = value.toText(),
                action = AddPlaylist(value)
            )
            )
        }
    }

    private fun closeDialog() {
        _viewState.update { it.copy(dialogState = HomeDialogState.Closed) }
    }

    private fun addPlaylist(playlistUrl: String) {
        val playlistLink = playlistUrl.trim()
        if (playlistLink.startsWith("https://open.spotify.com/playlist/")) {
            val id = playlistLink.split("playlist/")[1].split("?")[0]
            loadPlaylistName(playlistId = id, accessTokenProvider.getAccessToken())
        } else {
            viewModelScope.launch {
                sendEvent(R.string.home_invalidLink)
            }
        }
    }

    private fun removePlaylist(playlist: Playlist) {
        val newPlaylists = _viewState.value.playlists.toMutableList()
            .filterNot { (it as PlaylistViewState.Success).playlist == playlist }
        _viewState.update { state ->
            state.copy(playlists = newPlaylists)
        }
    }

    private fun addPlaylists(playlists: List<Playlist>) {
        _viewState.update { state ->
            state.copy(playlists = state.playlists + playlists.map { PlaylistViewState.Success(it) })
        }
    }

    private fun toggleSelectionValue(selectionItem: PlaylistSelectionItem, checked: Boolean) {
        if (_viewState.value.dialogState is SelectionDialog) {
            _viewState.update { state ->
                val dialogState = (state.dialogState as SelectionDialog)
                val newSelectedItems = dialogState.selectionItems.map {
                    if (it == selectionItem) {
                        selectionItem.copy(selected = checked)
                    } else {
                        it
                    }
                }
                state.copy(dialogState = dialogState.copy(
                    selectionItems = newSelectedItems,
                    action = AddPlaylists(
                        newSelectedItems.filter { it.selected }.map { it.playlist }
                    )
                ))
            }
        }
    }

    private fun loadPlaylistName(playlistId: String, accessToken: String?) {
        if (hasPlaylist(playlistId) || accessToken == null) {
            return
        }
        viewModelScope.launch {
            val playlists = _viewState.value.playlists
            _viewState.update {
                it.copy(playlists = playlists + PlaylistViewState.Loading)
            }

            val result = fetchPlaylistName(playlistId, accessToken)
            _viewState.update {
                if (result != null) {
                    val newPlaylist = Playlist(id = playlistId, result)
                    it.copy(
                        playlists = playlists + PlaylistViewState.Success(newPlaylist)
                    )
                } else {
                    it.copy(playlists = playlists + PlaylistViewState.Error)
                }
            }
        }
    }

    private suspend fun fetchPlaylistName(playlistId: String, accessToken: String): String? {
        val url = "https://api.spotify.com/v1/playlists/$playlistId"
        val client = OkHttpClient()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        return withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val json = JsonParser.parseString(responseBody).asJsonObject
                    json.get("name").asString
                } else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun loadUserPlaylists(accessToken: String?) {
        accessToken?.let { token ->
            viewModelScope.launch {
                val playlists = fetchUserPlaylists(token)
                if (playlists != null) {
                    spotifyPlaylists = playlists
                    _viewState.update { state ->
                        state.copy(dialogState = SelectionDialog(
                            text = R.string.home_selectPlaylists.toText(),
                            selectionItems = playlists.map {
                                PlaylistSelectionItem(playlist =  it)
                            },
                            action = AddPlaylists(emptyList())
                        ))
                    }
                }
            }
        } ?: Log.e(LOG_TAG, "No access token found.")
    }

    private suspend fun fetchUserPlaylists(accessToken: String): List<Playlist>? {
        val url = "https://api.spotify.com/v1/me/playlists"
        val client = OkHttpClient()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        return withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val json = JsonParser.parseString(responseBody).asJsonObject
                    val playlists = json.getAsJsonArray("items")

                    playlists.map { item ->
                        val obj = item.asJsonObject
                        val name = obj.get("name").asString
                        val id = obj.get("id").asString
                        Playlist(id = id, name = name)
                    }
                } else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private suspend fun sendEvent(@StringRes messageRes: Int) {
        _event.emit(messageRes)
    }

    private fun hasPlaylist(playlistId: String) =
        _viewState.value.playlists
            .filterIsInstance<PlaylistViewState.Success>()
            .map { it.playlist.id }
            .contains(playlistId)

    companion object {
        private const val LOG_TAG = "HomeViewModel"
    }
}