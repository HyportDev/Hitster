package com.example.hitster.game

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hitster.game.usecase.ConnectToSpotifyUseCase
import com.example.hitster.R
import com.example.hitster.game.model.ButtonState
import com.example.hitster.game.model.GameAction
import com.example.hitster.game.model.GameAction.Guess
import com.example.hitster.game.model.GameAction.MoveLeft
import com.example.hitster.game.model.GameAction.MoveRight
import com.example.hitster.game.model.GameAction.NextPlayer
import com.example.hitster.game.model.GameAction.OnPauseClick
import com.example.hitster.game.model.GameAction.OnPlayClick
import com.example.hitster.game.model.GameAction.OnSelectPlayer
import com.example.hitster.game.model.GameViewState
import com.example.hitster.game.model.MusicButtonItem
import com.example.hitster.game.model.Player
import com.example.hitster.game.model.PlayerItem
import com.example.hitster.game.model.Song
import com.example.hitster.game.model.UnknownSong
import com.example.hitster.game.model.getGuessCardPosition
import com.example.hitster.game.model.getGuessedYearRange
import com.example.hitster.game.model.moveSongItemLeft
import com.example.hitster.game.model.moveSongItemRight
import com.example.hitster.game.usecase.FetchPlaylistTracksUseCase
import com.example.hitster.game.usecase.FetchTrackDetailsUseCase
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.Repeat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class GameViewModel(
    playerNames: List<String>,
    playlists: List<String>,
    private val fetchTrackDetailsUseCase: FetchTrackDetailsUseCase,
    private val fetchPlaylistTracksUseCase: FetchPlaylistTracksUseCase,
    private val connectToSpotifyUseCase: ConnectToSpotifyUseCase,
) : ViewModel() {

    private var spotifyAppRemote: SpotifyAppRemote? = null

    private val players: MutableList<Player> = mutableListOf()
    private val trackURIs = ArrayDeque<String>()

    private var currentSongUri: String? = null

    private val _viewState = MutableStateFlow(GameViewState.initial())
    val viewState = _viewState.asStateFlow()

    init {
        playerNames.let { players.addAll(playerNames.shuffled().map { Player.initialise(it) }) }
        fetchTracksFromPlaylists(playlists)
        nextPlayer()
    }

    fun initSpotifyConnection(clientId: String, redirectUri: String) {
        viewModelScope.launch {
            val result = connectToSpotifyUseCase(clientId, redirectUri)
            if (result.isSuccess){
                val appRemote = result.getOrThrow()
                setSpotifyAppRemote(appRemote)
            } else {
                Log.e("Spotify", "Failed to connect", result.exceptionOrNull())
            }
        }
    }

    fun closeSpotifyConnection() {
        spotifyAppRemote?.let {
            SpotifyAppRemote.disconnect(it)
        }
    }

    private fun setSpotifyAppRemote(spotifyAppRemote: SpotifyAppRemote) {
        this.spotifyAppRemote = spotifyAppRemote
        spotifyAppRemote.playerApi.setRepeat(Repeat.ONE)
    }

    fun onAction(action: GameAction) {
        when(action) {
            OnPauseClick -> pauseMusic()
            OnPlayClick -> playMusic()
            MoveLeft -> moveGuessCardLeft()
            MoveRight -> moveGuessCardRight()
            Guess -> loginGuess()
            is OnSelectPlayer -> clickPlayer(action.playerItem)
            NextPlayer -> nextPlayer()
        }
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

    private fun nextPlayer() {
        val nextPlayer = players.removeLastOrNull()
        nextPlayer?.let { player ->
            players.add(0, player)
            _viewState.update { state ->
                state.copy(
                    currentPlayer = player,
                    songItems = player.songs + UnknownSong(),
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
                        action = Guess
                    )
                )
            }
            nextSong()
        }
    }

    private fun clickPlayer(playerItem: PlayerItem) {
        _viewState.update { state ->
            val songItems = (players.find { it.name == playerItem.playerName }?.songs ?: emptyList())
            val isCurrentPlayer = state.currentPlayer?.name == playerItem.playerName
            state.copy(
                playerItems = state.playerItems.map { it.copy(isSelected = it == playerItem) },
                songItems = if (isCurrentPlayer) {
                    songItems + UnknownSong()
                } else {
                    songItems
                },
                primaryButton = state.primaryButton.copy(isVisible = isCurrentPlayer || state.currentPlayer == null)
            )
        }
    }

    private fun playMusic() {
        spotifyAppRemote?.playerApi?.resume()
        _viewState.update { it.copy(musicButton = MusicButtonItem.PAUSE) }
    }

    private fun pauseMusic() {
        spotifyAppRemote?.playerApi?.pause()
        _viewState.update { it.copy(musicButton = MusicButtonItem.PLAY) }
    }

    private fun moveGuessCardLeft() {
        _viewState.update { state ->
            val position = state.songItems.getGuessCardPosition()
            val songItems = state.songItems.toMutableList()
            songItems[position] = UnknownSong(
                canMoveLeft = position > 1,
                canMoveRight = true
            )
            state.copy(
                songItems = songItems.moveSongItemLeft(position)
            )
        }
    }

    private fun moveGuessCardRight() {
        _viewState.update { state ->
            val songItems = state.songItems.toMutableList()
            val position = songItems.getGuessCardPosition()
            songItems[position] = UnknownSong(
                canMoveLeft = true,
                canMoveRight = position + 2 < songItems.size
            )
            state.copy(
                songItems = songItems.moveSongItemRight(position)
            )
        }
    }

    private fun loginGuess() {
        _viewState.update { state ->
            val player = state.currentPlayer
            val song = state.currentSong
            val songItems = state.songItems
            if (player != null && song != null) {
                val songPosition = songItems.getGuessCardPosition()
                val guessedCorrectLocation = checkGuess(song, songPosition)
                val color = Color(Random.nextLong()).copy(alpha = 1f)

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
                        action = NextPlayer
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
