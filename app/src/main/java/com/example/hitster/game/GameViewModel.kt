package com.example.hitster.game

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hitster.R
import com.example.hitster.game.model.ButtonState
import com.example.hitster.game.model.GameAction
import com.example.hitster.game.model.GameAction.AddToken
import com.example.hitster.game.model.GameAction.Guess
import com.example.hitster.game.model.GameAction.MoveLeft
import com.example.hitster.game.model.GameAction.MoveRight
import com.example.hitster.game.model.GameAction.NextPlayer
import com.example.hitster.game.model.GameAction.OnPauseClick
import com.example.hitster.game.model.GameAction.OnPlayClick
import com.example.hitster.game.model.GameAction.OnPlayerClick
import com.example.hitster.game.model.GameUiState
import com.example.hitster.game.model.MusicButtonItem
import com.example.hitster.game.model.Player
import com.example.hitster.game.model.Song
import com.example.hitster.game.model.SongItem
import com.example.hitster.game.model.UnknownSong
import com.example.hitster.game.model.getGuessCardPosition
import com.example.hitster.game.model.getGuessedYearRange
import com.example.hitster.game.model.moveSongItemLeft
import com.example.hitster.game.model.moveSongItemRight
import com.example.hitster.game.usecase.ConnectToSpotifyUseCase
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
    private val trackURIs = ArrayDeque<String>()

    private val _uiState = MutableStateFlow(
        GameUiState(
            musicButton = MusicButtonItem.PLAY,
            primaryButton = ButtonState(),
            players = playerNames.shuffled().map { Player.initialize(it) },
            currentSong = null
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
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
            is OnPlayerClick -> clickPlayer(action.player)
            NextPlayer -> nextPlayer()
            is AddToken -> addToken(action.player)
        }
    }

    private fun addToken(player: Player) {
        _uiState.update { state ->
            state.copy(
                players = state.players.map {
                    if (it == player) {
                        it.copy(tokens = it.tokens + 1)
                    } else {
                        it
                    }
                },
                isAddTokenButtonVisible = false
            )
        }
    }

    private fun addSongs(uris: List<String>) {
        trackURIs.addAll(uris)
        trackURIs.shuffle()
        nextSong()
    }

    private fun nextSong() {
        trackURIs.removeFirstOrNull()?.let { uri ->
            spotifyAppRemote?.playerApi?.play(uri)
            _uiState.update { it.copy(musicButton = MusicButtonItem.PAUSE) }
            loadTrack(uri)
        } ?:  Log.e(LOG_TAG, "Playlist has no items left.")
    }

    private fun nextPlayer() {
        _uiState.update { state ->
            val players = state.players
                .map { player ->
                    player.copy(
                        isCurrentPlayer = false,
                        isSelected = false,
                        songs = player.songs.filterIsInstance<Song>()
                            .filterNot { song -> song.correctLocation == false }
                            .map { song -> song.copy(correctLocation = null) }
                    )
                }
                .toMutableList()
            val newCurrentPlayer = players.removeAt(players.lastIndex)
            players.add(
                0,
                newCurrentPlayer.copy(
                    isCurrentPlayer = true,
                    isSelected = true,
                    songs = newCurrentPlayer.songs + UnknownSong()
                )
            )
            state.copy(
                players = players,
                primaryButton = ButtonState(
                    isVisible = true,
                    title = R.string.game_buttonGuess,
                    action = Guess
                ),
                isAddTokenButtonVisible = false
            )
        }
        nextSong()
    }

    private fun clickPlayer(player: Player) {
        _uiState.update { state ->
            state.copy(
                players = state.players.map { it.copy(isSelected = it == player) },
                primaryButton = state.primaryButton.copy(isVisible = player.isCurrentPlayer)
            )
        }
    }

    private fun playMusic() {
        spotifyAppRemote?.playerApi?.resume()
        _uiState.update { it.copy(musicButton = MusicButtonItem.PAUSE) }
    }

    private fun pauseMusic() {
        spotifyAppRemote?.playerApi?.pause()
        _uiState.update { it.copy(musicButton = MusicButtonItem.PLAY) }
    }

    private fun moveGuessCardLeft() {
        _uiState.update { state ->
            val currentPlayer = state.players.find { it.isCurrentPlayer }
            val songs = currentPlayer?.songs?.toMutableList()
            val guessCardPosition = currentPlayer?.songs?.getGuessCardPosition()
            if (songs == null ||guessCardPosition == null || guessCardPosition == -1) {
                state
            } else {
                songs[guessCardPosition] = UnknownSong(
                    canMoveLeft = guessCardPosition > 1,
                    canMoveRight = true
                )
                state.copy(
                    players = state.players.map { player ->
                        if (player.isCurrentPlayer) {
                            player.copy(songs = songs.moveSongItemLeft(guessCardPosition))
                        } else {
                            player
                        }
                    }
                )
            }
        }
    }

    private fun moveGuessCardRight() {
        _uiState.update { state ->
            val currentPlayer = state.players.find { it.isCurrentPlayer }
            val songs = currentPlayer?.songs?.toMutableList()
            val guessCardPosition = currentPlayer?.songs?.getGuessCardPosition()
            if (songs == null ||guessCardPosition == null || guessCardPosition == -1) {
                state
            } else {
                songs[guessCardPosition] = UnknownSong(
                    canMoveLeft = true,
                    canMoveRight = guessCardPosition + 2 < songs.size
                )
                state.copy(
                    players = state.players.map { player ->
                        if (player.isCurrentPlayer) {
                            player.copy(songs = songs.moveSongItemRight(guessCardPosition))
                        } else {
                            player
                        }
                    }
                )
            }
        }
    }

    private fun loginGuess() {
        val songPosition = _uiState.value.players.find { it.isCurrentPlayer }?.songs?.getGuessCardPosition()
        if (songPosition == null || songPosition < 0) {
            Log.e(LOG_TAG, "No guess card found")
            return
        }
        _uiState.update { state ->
            state.copy(
                players = state.players.map { player ->
                    player.copy(
                        songs = player.songs.mapIndexed { index, song ->
                            if (state.currentSong != null && song is UnknownSong) {
                                val guessed = checkGuess(state.currentSong, player.songs, index)
                                state.currentSong.copy(
                                    correctLocation = guessed,
                                    color = generateColor()
                                )
                            } else {
                                song
                            }
                        }
                    )
                },
                currentSong = null,
                primaryButton = ButtonState(
                    isVisible = true,
                    title = R.string.game_buttonNext,
                    action = NextPlayer
                ),
                isAddTokenButtonVisible = true
            )
        }
    }

    private fun checkGuess(song: Song, playerSongs: List<SongItem>, guessedIndex: Int): Boolean =
        song.releaseYear in playerSongs.getGuessedYearRange(guessedIndex)

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
            } catch (_: Exception) {
                Log.e(LOG_TAG, "Error fetching track details")
            }
        }
    }

    private fun setCurrentSong(song: Song) {
        _uiState.update { it.copy(currentSong = song) }
    }

    private fun generateColor() : Color {
        val red = Random.nextInt(200, 256)
        val green = Random.nextInt(200, 256)
        val blue = Random.nextInt(200, 256)
        return Color(red, green, blue)
    }

    companion object {
        private const val LOG_TAG = "GameViewModel"
    }
}
