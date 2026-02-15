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
import kotlinx.coroutines.delay
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
    private var currentPlayerIndex = -1
    private var originalPlayerDuringSteal: Player? = null

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
            GameAction.SkipSong -> skipSong()
            is GameAction.UseToken -> useToken(action.playerItem)
            GameAction.AddToken -> addToken()
        }
    }

    private fun addToken() {
        val targetPlayerIndex = if (originalPlayerDuringSteal != null) {
            players.indexOfFirst { it.name == originalPlayerDuringSteal?.name }
        } else {
            currentPlayerIndex
        }

        if (targetPlayerIndex != -1) {
            val player = players[targetPlayerIndex]
            if (player.tokens < MAX_TOKENS) {
                val updatedPlayer = player.copy(tokens = player.tokens + 1)
                players[targetPlayerIndex] = updatedPlayer

                _viewState.update { state ->
                    state.copy(
                        addTokenButton = state.addTokenButton.copy(isVisible = false),
                        playerItems = players.mapIndexed { index, it ->
                            PlayerItem(
                                playerName = it.name,
                                isCurrentPlayer = index == currentPlayerIndex && originalPlayerDuringSteal == null,
                                isSelected = state.playerItems[index].isSelected,
                                tokens = it.tokens
                            )
                        }
                    )
                }
            } else {
                _viewState.update { state ->
                    state.copy(addTokenButton = state.addTokenButton.copy(isVisible = false))
                }
            }
        }
    }

    private fun skipSong() {
        _viewState.value.currentPlayer?.let { currentPlayer ->
            if (currentPlayer.tokens > 0) {
                val updatedPlayer = currentPlayer.copy(tokens = currentPlayer.tokens - 1)
                val index = players.indexOfFirst { it.name == currentPlayer.name }
                if (index != -1) {
                    players[index] = updatedPlayer
                    _viewState.update { state ->
                        state.copy(
                            currentPlayer = updatedPlayer,
                            playerItems = players.map {
                                PlayerItem(
                                    playerName = it.name,
                                    isCurrentPlayer = updatedPlayer.name == it.name,
                                    isSelected = updatedPlayer.name == it.name,
                                    tokens = it.tokens
                                )
                            },
                            skipButton = state.skipButton.copy(isVisible = updatedPlayer.tokens > 0)
                        )
                    }
                    originalPlayerDuringSteal = null
                    nextSong()
                }
            }
        }
    }

    private fun useToken(playerItem: PlayerItem) {
        val stealer = players.find { it.name == playerItem.playerName }
        val originalPlayer = players.getOrNull(currentPlayerIndex)
        val currentSong = _viewState.value.currentSong
        
        if (stealer != null && stealer.tokens > 0 && originalPlayer != null && stealer.name != originalPlayer.name && currentSong != null) {
            val updatedStealer = stealer.copy(tokens = stealer.tokens - 1)
            val stealerIndex = players.indexOfFirst { it.name == stealer.name }
            players[stealerIndex] = updatedStealer

            val guessPosition = _viewState.value.songItems.getGuessCardPosition()
            val isOriginalPlayerCorrect = checkGuess(currentSong, guessPosition)

            if (isOriginalPlayerCorrect) {
                val color = generateColor()
                val updatedOriginalSongs = originalPlayer.songs.toMutableList()
                updatedOriginalSongs.add(guessPosition, currentSong.copy(color = color))
                players[currentPlayerIndex] = originalPlayer.copy(songs = updatedOriginalSongs)

                val revealedSongList = _viewState.value.songItems.toMutableList()
                revealedSongList[guessPosition] = currentSong.copy(correctLocation = true, color = color)

                _viewState.update { state ->
                    state.copy(
                        currentPlayer = null,
                        currentSong = null,
                        songItems = revealedSongList,
                        isStealInProgress = false,
                        skipButton = state.skipButton.copy(isVisible = false),
                        addTokenButton = state.addTokenButton.copy(isVisible = originalPlayer.tokens < MAX_TOKENS),
                        message = "The guess was already correct",
                        playerItems = players.map {
                            PlayerItem(
                                playerName = it.name,
                                isCurrentPlayer = false,
                                isSelected = it.name == originalPlayer.name,
                                tokens = it.tokens
                            )
                        },
                        primaryButton = ButtonState(
                            isVisible = true,
                            title = R.string.game_buttonNext,
                            action = NextPlayer
                        )
                    )
                }
                viewModelScope.launch {
                    delay(2000)
                    _viewState.update { it.copy(message = null) }
                }
            } else {
                originalPlayerDuringSteal = originalPlayer
                _viewState.update { state ->
                    state.copy(
                        currentPlayer = updatedStealer,
                        songItems = originalPlayer.songs + UnknownSong(),
                        isStealInProgress = true,
                        skipButton = state.skipButton.copy(isVisible = false),
                        playerItems = players.map {
                            PlayerItem(
                                playerName = it.name,
                                isCurrentPlayer = it.name == updatedStealer.name,
                                isSelected = it.name == originalPlayer.name,
                                tokens = it.tokens
                            )
                        },
                        primaryButton = ButtonState(
                            isVisible = true,
                            title = R.string.game_buttonGuess,
                            action = Guess
                        )
                    )
                }
            }
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
        if (players.isEmpty()) return
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size
        val player = players[currentPlayerIndex]
        originalPlayerDuringSteal = null
        
        _viewState.update { state ->
            state.copy(
                currentPlayer = player,
                songItems = player.songs + UnknownSong(),
                isStealInProgress = false,
                skipButton = state.skipButton.copy(isVisible = player.tokens > 0),
                addTokenButton = state.addTokenButton.copy(isVisible = false),
                playerItems = players.mapIndexed { index, p ->
                    PlayerItem(
                        playerName = p.name,
                        isCurrentPlayer = index == currentPlayerIndex,
                        isSelected = index == currentPlayerIndex,
                        tokens = p.tokens
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

    private fun clickPlayer(playerItem: PlayerItem) {
        _viewState.update { state ->
            val clickedPlayer = players.find { it.name == playerItem.playerName }
            val songItems = (clickedPlayer?.songs ?: emptyList())
            val activePlayer = state.currentPlayer
            
            val shouldShowGuessCard = if (originalPlayerDuringSteal != null) {
                clickedPlayer?.name == originalPlayerDuringSteal?.name
            } else {
                clickedPlayer?.name == activePlayer?.name
            }

            state.copy(
                playerItems = state.playerItems.map { it.copy(isSelected = it.playerName == playerItem.playerName) },
                songItems = if (shouldShowGuessCard) {
                    songItems + UnknownSong()
                } else {
                    songItems
                },
                skipButton = state.skipButton.copy(
                    isVisible = shouldShowGuessCard && originalPlayerDuringSteal == null && (activePlayer?.tokens ?: 0) > 0
                ),
                primaryButton = state.primaryButton.copy(isVisible = (activePlayer != null) && shouldShowGuessCard)
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
        var message: String? = null
        _viewState.update { state ->
            val activePlayer = state.currentPlayer
            val song = state.currentSong
            val displayedSongItems = state.songItems
            if (activePlayer != null && song != null) {
                val songPosition = displayedSongItems.getGuessCardPosition()
                val guessedCorrectLocation = checkGuess(song, songPosition)
                val color = generateColor()

                if (guessedCorrectLocation) {
                    if (originalPlayerDuringSteal != null) {
                        val stealerSongs = activePlayer.songs.toMutableList()
                        val insertIndex = stealerSongs.findInsertIndex(song.releaseYear)
                        stealerSongs.add(insertIndex, song.copy(color = color))
                        
                        val playerIndex = players.indexOfFirst { it.name == activePlayer.name }
                        if (playerIndex != -1) {
                            players[playerIndex] = activePlayer.copy(songs = stealerSongs)
                        }
                        message = "${activePlayer.name} successfully stole the song!"
                    } else {
                        val newPlayerSongs = activePlayer.songs.toMutableList()
                        newPlayerSongs.add(songPosition, song.copy(color = color))
                        val playerIndex = players.indexOfFirst { it.name == activePlayer.name }
                        if (playerIndex != -1) {
                            players[playerIndex] = activePlayer.copy(songs = newPlayerSongs)
                        }
                    }
                }

                val revealedSongList = displayedSongItems.toMutableList()
                revealedSongList[songPosition] = state.currentSong
                    .copy(correctLocation = guessedCorrectLocation, color = color)

                val originalPlayer = players[currentPlayerIndex]
                val showAddToken = guessedCorrectLocation && originalPlayerDuringSteal == null && originalPlayer.tokens < MAX_TOKENS

                state.copy(
                    currentPlayer = null,
                    currentSong = null,
                    songItems = revealedSongList,
                    isStealInProgress = false,
                    skipButton = state.skipButton.copy(isVisible = false),
                    addTokenButton = state.addTokenButton.copy(isVisible = showAddToken),
                    message = message,
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
        if (message != null) {
            viewModelScope.launch {
                delay(2000)
                _viewState.update { it.copy(message = null) }
            }
        }
    }

    private fun List<Song>.findInsertIndex(year: Int): Int {
        var index = 0
        while (index < size && this[index].releaseYear < year) {
            index++
        }
        return index
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

    private fun generateColor() : Color {
        val red = Random.nextInt(200, 256)
        val green = Random.nextInt(200, 256)
        val blue = Random.nextInt(200, 256)
        return Color(red, green, blue)
    }

    companion object {
        private const val LOG_TAG = "GameViewModel"
        private const val MAX_TOKENS = 5
    }
}
