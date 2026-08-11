package com.example.hitster.game

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hitster.R
import com.example.hitster.data.SpotifyAuthException
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
import com.example.hitster.game.model.GameAction.PlaceToken
import com.example.hitster.game.model.GameAction.Reveal
import com.example.hitster.game.model.GameAction.TakeBackToken
import com.example.hitster.game.model.GamePhase
import com.example.hitster.game.model.GameUiState
import com.example.hitster.game.model.MusicButtonItem
import com.example.hitster.game.model.Player
import com.example.hitster.game.model.Song
import com.example.hitster.game.model.TokenBet
import com.example.hitster.game.model.UnknownSong
import com.example.hitster.game.model.getFreeGapIndices
import com.example.hitster.game.model.getGapYearRange
import com.example.hitster.game.model.getGuessCardPosition
import com.example.hitster.game.model.getPlacedSongs
import com.example.hitster.game.model.moveSongItemLeft
import com.example.hitster.game.model.moveSongItemRight
import com.example.hitster.game.usecase.ConnectToSpotifyUseCase
import com.example.hitster.game.usecase.FetchPlaylistTracksUseCase
import com.example.hitster.game.usecase.FetchTrackDetailsUseCase
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.Repeat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    private val _event = MutableSharedFlow<Int>()

    /** String resources for problems the player has to know about, shown as a snackbar. */
    val event = _event.asSharedFlow()

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
        // The first song can only start once the playlists are here, addSongs() takes care of it.
        fetchTracksFromPlaylists(playlists)
        switchToNextPlayer()
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
            Reveal -> reveal()
            is OnPlayerClick -> clickPlayer(action.player)
            NextPlayer -> nextPlayer()
            is AddToken -> addToken(action.player)
            is PlaceToken -> placeToken(action.playerName, action.gapIndex)
            is TakeBackToken -> takeBackToken(action.gapIndex)
        }
    }

    private fun addToken(player: Player) {
        _uiState.update { state ->
            state.copy(
                players = state.players.map {
                    if (it.name == player.name) {
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
        switchToNextPlayer()
        nextSong()
    }

    private fun switchToNextPlayer() {
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
                currentSong = null,
                primaryButton = ButtonState(
                    title = R.string.game_buttonGuess,
                    action = Guess
                ),
                isAddTokenButtonVisible = false,
                phase = GamePhase.GUESSING,
                tokenBets = emptyList(),
                tokenWinnerName = null,
                refundedTokenPlayerNames = emptyList(),
                wasGuessCorrect = null
            ).updatePrimaryButtonVisibility()
        }
    }

    /**
     * While the song details are still loading there is nothing to guess yet, so the button stays
     * away instead of running into an empty guess. Once the song is revealed, moving on has to be
     * possible from every timeline.
     */
    private fun GameUiState.updatePrimaryButtonVisibility(): GameUiState {
        val isVisible = when (phase) {
            GamePhase.GUESSING ->
                currentSong != null && players.any { it.isSelected && it.isCurrentPlayer }

            GamePhase.TOKEN_PLACEMENT, GamePhase.REVEALED -> true
        }
        return copy(primaryButton = primaryButton.copy(isVisible = isVisible))
    }

    private fun clickPlayer(player: Player) {
        _uiState.update { state ->
            // While tokens are placed everybody bets on the current player's timeline, so it stays
            // on screen.
            if (state.phase == GamePhase.TOKEN_PLACEMENT) return@update state
            state.copy(
                players = state.players.map { it.copy(isSelected = it.name == player.name) }
            ).updatePrimaryButtonVisibility()
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
            if (state.phase != GamePhase.GUESSING) return@update state
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
            if (state.phase != GamePhase.GUESSING) return@update state
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

    /**
     * The current player commits to the gap the guess card sits in. The song is not revealed yet:
     * the other players now get the chance to bet a token on one of the remaining gaps. When nobody
     * owns a token there is nothing to bet, so the song is revealed right away.
     */
    private fun loginGuess() {
        val state = _uiState.value
        if (state.phase != GamePhase.GUESSING) return
        val currentPlayer = state.players.find { it.isCurrentPlayer }
        if (currentPlayer == null || currentPlayer.songs.getGuessCardPosition() < 0) {
            Log.e(LOG_TAG, "No guess card found")
            return
        }
        if (state.currentSong == null) {
            Log.e(LOG_TAG, "Song is not loaded yet")
            return
        }
        val canAnybodyBet = state.players.any { !it.isCurrentPlayer && it.tokens > 0 }
        if (!canAnybodyBet) {
            reveal()
            return
        }
        _uiState.update {
            it.copy(
                phase = GamePhase.TOKEN_PLACEMENT,
                tokenBets = emptyList(),
                tokenWinnerName = null,
                primaryButton = ButtonState(
                    title = R.string.game_buttonReveal,
                    action = Reveal
                )
            ).updatePrimaryButtonVisibility()
        }
    }

    /** Another player bets their token on a free gap of the current player's timeline. */
    private fun placeToken(playerName: String, gapIndex: Int) {
        _uiState.update { state ->
            if (state.phase != GamePhase.TOKEN_PLACEMENT) return@update state
            val player = state.players.find { it.name == playerName }
            val freeGaps = state.players.find { it.isCurrentPlayer }
                ?.songs
                ?.getFreeGapIndices(state.tokenBets.map { it.gapIndex })
                ?: return@update state
            val hasAlreadyBet = state.tokenBets.any { it.playerName == playerName }
            if (player == null || player.isCurrentPlayer || player.tokens <= 0 || hasAlreadyBet ||
                gapIndex !in freeGaps
            ) {
                return@update state
            }
            state.copy(
                // The token is spent no matter how the round ends.
                players = state.players.map {
                    if (it.name == playerName) it.copy(tokens = it.tokens - 1) else it
                },
                tokenBets = state.tokenBets + TokenBet(playerName = playerName, gapIndex = gapIndex)
            )
        }
    }

    /** Frees a gap again as long as the song is not revealed, the token goes back to its owner. */
    private fun takeBackToken(gapIndex: Int) {
        _uiState.update { state ->
            if (state.phase != GamePhase.TOKEN_PLACEMENT) return@update state
            val bet = state.tokenBets.find { it.gapIndex == gapIndex } ?: return@update state
            state.copy(
                players = state.players.map {
                    if (it.name == bet.playerName) it.copy(tokens = it.tokens + 1) else it
                },
                tokenBets = state.tokenBets - bet
            )
        }
    }

    /**
     * Reveals the song. The current player keeps the card when their gap was right, otherwise the
     * first player whose token hit the right gap wins the card instead.
     */
    private fun reveal() {
        val state = _uiState.value
        val currentPlayer = state.players.find { it.isCurrentPlayer }
        val song = state.currentSong
        if (currentPlayer == null || song == null) {
            Log.e(LOG_TAG, "Nothing to reveal")
            return
        }
        val guessCardGap = currentPlayer.songs.getGuessCardPosition()
        if (guessCardGap < 0) {
            Log.e(LOG_TAG, "No guess card found")
            return
        }
        val timeline = currentPlayer.songs.getPlacedSongs()
        val guessedCorrectLocation = song.releaseYear in timeline.getGapYearRange(guessCardGap)
        val tokenBets = state.tokenBets.map {
            it.copy(
                isCorrect = song.releaseYear in timeline.getGapYearRange(it.gapIndex),
                // The revealed card takes the place of the guess card, so every gap behind it
                // moves one to the right.
                gapIndex = if (it.gapIndex > guessCardGap) it.gapIndex + 1 else it.gapIndex
            )
        }
        val tokenWinnerName = if (guessedCorrectLocation) {
            null
        } else {
            tokenBets.firstOrNull { it.isCorrect == true }?.playerName
        }
        // Equal release years can make more than one gap correct. Whoever hit a right gap without
        // winning the card keeps their token, only the card decides the round.
        val refundedTokenPlayerNames = tokenBets
            .filter { it.isCorrect == true && it.playerName != tokenWinnerName }
            .map { it.playerName }
        val color = generateColor()

        _uiState.update { current ->
            current.copy(
                players = current.players.map { player ->
                    val updatedPlayer = when {
                        player.isCurrentPlayer -> player.copy(
                            songs = player.songs.map { songItem ->
                                if (songItem is UnknownSong) {
                                    song.copy(
                                        correctLocation = guessedCorrectLocation,
                                        color = color
                                    )
                                } else {
                                    songItem
                                }
                            }
                        )

                        player.name == tokenWinnerName -> player.copy(
                            songs = player.songs.toMutableList().apply {
                                add(
                                    count { it is Song && it.releaseYear <= song.releaseYear },
                                    song.copy(correctLocation = true, color = color)
                                )
                            }
                        )

                        player.name in refundedTokenPlayerNames ->
                            player.copy(tokens = player.tokens + 1)

                        else -> player
                    }
                    // Show the winner their new card right away.
                    if (tokenWinnerName == null) {
                        updatedPlayer
                    } else {
                        updatedPlayer.copy(isSelected = player.name == tokenWinnerName)
                    }
                },
                currentSong = null,
                phase = GamePhase.REVEALED,
                tokenBets = tokenBets,
                tokenWinnerName = tokenWinnerName,
                refundedTokenPlayerNames = refundedTokenPlayerNames,
                wasGuessCorrect = guessedCorrectLocation,
                primaryButton = ButtonState(
                    title = R.string.game_buttonNext,
                    action = NextPlayer
                ),
                isAddTokenButtonVisible = true
            ).updatePrimaryButtonVisibility()
        }
    }

    private fun fetchTracksFromPlaylists(playlistIds: List<String>) {
        viewModelScope.launch {
            try {
                val tracks = fetchPlaylistTracksUseCase(playlistIds)
                addSongs(uris = tracks)
            } catch (e: SpotifyAuthException) {
                reportSpotifySessionLost(e)
            }
        }
    }

    private fun loadTrack(uri: String) {
        viewModelScope.launch {
            try {
                fetchTrackDetailsUseCase(uri)?.let { song ->
                    setCurrentSong(song)
                }
            } catch (e: SpotifyAuthException) {
                // Without a session the round cannot continue, so it must not fail silently.
                reportSpotifySessionLost(e)
            } catch (_: Exception) {
                Log.e(LOG_TAG, "Error fetching track details")
            }
        }
    }

    private suspend fun reportSpotifySessionLost(cause: SpotifyAuthException) {
        Log.e(LOG_TAG, "No usable Spotify session", cause)
        _event.emit(R.string.error_spotifySession)
    }

    private fun setCurrentSong(song: Song) {
        _uiState.update { it.copy(currentSong = song).updatePrimaryButtonVisibility() }
    }

    /**
     * A random but saturated card color. Lightness stays high enough for the black card text and
     * low enough to not wash out against the dark background.
     */
    private fun generateColor() : Color = Color.hsl(
        hue = Random.nextInt(0, 360).toFloat(),
        saturation = 0.62f,
        lightness = 0.72f
    )

    companion object {
        private const val LOG_TAG = "GameViewModel"
    }
}
