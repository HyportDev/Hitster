package com.example.hitster.game

import android.content.res.Resources
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hitster.R
import com.example.hitster.game.data.AccessTokenProvider
import com.example.hitster.game.model.ButtonState
import com.example.hitster.game.model.GameAction
import com.example.hitster.game.model.GameViewState
import com.example.hitster.game.model.MusicButtonItem
import com.example.hitster.game.model.Player
import com.example.hitster.game.model.PlayerItem
import com.example.hitster.game.model.Song
import com.example.hitster.game.model.SongItemColor
import com.example.hitster.game.model.UnknownSong
import com.example.hitster.game.model.getGuessCardPosition
import com.example.hitster.game.model.getGuessedYearRange
import com.example.hitster.game.model.moveSongItemLeft
import com.example.hitster.game.model.moveSongItemRight
import com.example.hitster.game.usecase.FetchPlaylistTracksUseCase
import com.example.hitster.game.usecase.FetchTrackDetailsUseCase
import com.example.hitster.game.usecase.FindEarliestReleaseYearUseCase
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.Repeat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel(accessTokenProvider: AccessTokenProvider) : ViewModel() {

    // Use cases
    private val fetchPlaylistTracksUseCase = FetchPlaylistTracksUseCase(accessTokenProvider)
    private val findEarliestReleaseYearUseCase = FindEarliestReleaseYearUseCase(accessTokenProvider)
    private val fetchTrackDetailsUseCase = FetchTrackDetailsUseCase(
        accessTokenProvider, findEarliestReleaseYearUseCase
    )

    private var spotifyAppRemote: SpotifyAppRemote? = null

    private val players: MutableList<Player> = mutableListOf()
    private val trackURIs = ArrayDeque<String>()

    private var currentSongUri: String? = null

    private val _viewState = MutableStateFlow(GameViewState.initial())
    val viewState = _viewState.asStateFlow()

    fun init(players: List<String>, playlists: List<String>, resources: Resources) {
        this.players.addAll(players.map { Player.initialise(it, resources) })
        fetchTracksFromPlaylists(playlists)
        nextPlayer()
    }

    fun setSpotifyAppRemote(spotifyAppRemote: SpotifyAppRemote) {
        this.spotifyAppRemote = spotifyAppRemote
        spotifyAppRemote.playerApi.setRepeat(Repeat.ONE)
    }

    private fun addSongs(uris: List<String>) {
        trackURIs.addAll(uris)
        trackURIs.shuffle()
        nextSong()
    }

    private fun nextSong() {
        currentSongUri = trackURIs.removeFirstOrNull()
        currentSongUri?.let { uri ->
            spotifyAppRemote?.playerApi?.play(uri)
            _viewState.update { it.copy(musicButton = MusicButtonItem.PAUSE) }
            loadTrack(uri)
        } ?:  Log.e(LOG_TAG, "Playlist has no items left.")
    }

    fun nextPlayer() {
        val nextPlayer = players.removeLastOrNull()
        nextPlayer?.let { player ->
            players.add(0, player)
            _viewState.update { state ->
                state.copy(
                    currentPlayer = player,
                    songItems = player.songs + UnknownSong,
                    playerItems = players.map {
                        PlayerItem(
                            playerName = it.name,
                            isCurrentPlayer = player == it,
                            isSelected = player == it
                        )
                    },
                    primaryButton = ButtonState(
                        isVisible = true,
                        title = R.string.game_buttonGuess,
                        action = GameAction.Guess
                    )
                )
            }
            nextSong()
        }
    }

    fun clickPlayer(playerItem: PlayerItem) {
        _viewState.update { state ->
            val songItems = (players.find { it.name == playerItem.playerName }?.songs ?: emptyList())
            val isCurrentPlayer = state.currentPlayer?.name == playerItem.playerName
            state.copy(
                playerItems = state.playerItems.map { it.copy(isSelected = it == playerItem) },
                songItems = if (isCurrentPlayer) {
                    songItems + UnknownSong
                } else {
                    songItems
                },
                primaryButton = state.primaryButton.copy(isVisible = isCurrentPlayer || state.currentPlayer == null)
            )
        }
    }

    fun playMusic() {
        spotifyAppRemote?.playerApi?.resume()
        _viewState.update { it.copy(musicButton = MusicButtonItem.PAUSE) }
    }

    fun pauseMusic() {
        spotifyAppRemote?.playerApi?.pause()
        _viewState.update { it.copy(musicButton = MusicButtonItem.PLAY) }
    }

    fun moveGuessCardLeft() {
        _viewState.update { state ->
            state.copy(
                songItems = state.songItems.moveSongItemLeft(state.songItems.getGuessCardPosition())
            )
        }
    }

    fun moveGuessCardRight() {
        _viewState.update { state ->
            state.copy(
                songItems = state.songItems.moveSongItemRight(state.songItems.getGuessCardPosition())
            )
        }
    }

    fun loginGuess() {
        _viewState.update { state ->
            val player = state.currentPlayer
            val song = state.currentSong
            val songItems = state.songItems
            if (player != null && song != null) {
                val songPosition = songItems.getGuessCardPosition()
                val guessedCorrectLocation = checkGuess(song, songPosition)
                val color = SongItemColor.entries.random()

                if (guessedCorrectLocation) {
                    // Add song to the player's song list
                    val newPlayerSongs = player.songs.toMutableList()
                    newPlayerSongs.add(songPosition, song.copy(color = color))
                    players[players.indexOf(player)] = player.copy(songs = newPlayerSongs)
                }

                // Add song to temporary shown
                val revealedSongList = songItems.toMutableList()
                revealedSongList[songPosition] = state.currentSong
                    .copy(correctLocation = guessedCorrectLocation, color = color)

                state.copy(
                    currentPlayer = null,
                    currentSong = null,
                    songItems = revealedSongList,
                    primaryButton = ButtonState(
                        isVisible = true,
                        title = R.string.game_buttonNext,
                        action = GameAction.NextPlayer
                    )
                )
            } else {
                state
            }
        }
    }

    private fun checkGuess(song: Song, guessedIndex: Int): Boolean =
        song.releaseYear in _viewState.value.songItems.getGuessedYearRange(guessedIndex)

    private fun fetchTracksFromPlaylists(playlistIds: List<String>) {
        viewModelScope.launch {
            val tracks = fetchPlaylistTracksUseCase(playlistIds)
            addSongs(uris = tracks)
        }
    }

    private fun loadTrack(uri: String) {
        viewModelScope.launch {
            try {
                fetchTrackDetailsUseCase(uri)?.let { song ->
                    setCurrentSong(song)
                }
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Error fetching track details")
            }
        }
    }

    private fun setCurrentSong(song: Song) {
        _viewState.update { it.copy(currentSong = song) }
    }

    companion object {
        private const val LOG_TAG = "GameViewModel"
    }
}
