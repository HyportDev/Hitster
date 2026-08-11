package com.example.hitster.game

import android.content.Context
import android.util.Log
import app.cash.turbine.test
import com.example.hitster.game.model.GameAction
import com.example.hitster.game.model.GamePhase
import com.example.hitster.game.model.MusicButtonItem
import com.example.hitster.game.model.Player
import com.example.hitster.game.model.Song
import com.example.hitster.game.model.TokenBet
import com.example.hitster.game.model.UnknownSong
import com.example.hitster.game.model.getPlacedSongs
import com.example.hitster.game.usecase.ConnectToSpotifyUseCase
import com.example.hitster.game.usecase.FetchPlaylistTracksUseCase
import com.example.hitster.game.usecase.FetchTrackDetailsUseCase
import com.example.hitster.res.toText
import com.spotify.android.appremote.api.PlayerApi
import com.spotify.android.appremote.api.SpotifyAppRemote
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val fetchPlaylistTracksUseCase: FetchPlaylistTracksUseCase = mockk()
    private val fetchTrackDetailsUseCase: FetchTrackDetailsUseCase = mockk()
    private val connectToSpotifyUseCase: ConnectToSpotifyUseCase = mockk()

    private val spotifyAppRemote: SpotifyAppRemote = mockk(relaxed = true)
    private val playerApi: PlayerApi = mockk(relaxed = true)

    /** Stands in for the activity the connection needs, see ConnectToSpotifyUseCase. */
    private val context: Context = mockk(relaxed = true)

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0

        // Spotify Mocks vorbereiten
        every { spotifyAppRemote.playerApi } returns playerApi
        coEvery { connectToSpotifyUseCase(any(), any(), any()) } returns Result.success(spotifyAppRemote)

        // Standard-Verhalten für UseCases
        coEvery { fetchPlaylistTracksUseCase(any()) } returns listOf("spotify:track:1", "spotify:track:2")
        coEvery { fetchTrackDetailsUseCase(any()) } returns Song("Title".toText(), "Artist".toText(), 2000)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
    }

    @Test
    fun `initialization fetches tracks and sets up first player`() = runTest {
        val viewModel = createViewModel(listOf("Alice", "Bob"))
        advanceUntilIdle() // Warte auf init { fetchTracksFromPlaylists }

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.players.size)
            assertTrue(state.players.any { it.isCurrentPlayer })

            // Verifiziere, dass Tracks geladen wurden
            coVerify { fetchPlaylistTracksUseCase(any()) }
        }
    }

    @Test
    fun `initSpotifyConnection updates remote and sets repeat mode`() = runTest {
        val viewModel = createViewModel(listOf("Alice"))

        viewModel.initSpotifyConnection(context, "client", "uri")
        advanceUntilIdle()

        coVerify { connectToSpotifyUseCase(context, "client", "uri") }
        verify { playerApi.setRepeat(any()) }
    }

    @Test
    fun `onPauseClick calls spotify pause and updates state`() = runTest {
        val viewModel = createViewModel(listOf("Alice"))
        viewModel.initSpotifyConnection(context, "client", "uri")
        advanceUntilIdle()

        viewModel.onAction(GameAction.OnPauseClick)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(MusicButtonItem.PLAY, state.musicButton)
            verify { playerApi.pause() }
        }
    }

    @Test
    fun `addToken increases player token count`() = runTest {
        val viewModel = createViewModel(listOf("Alice"))
        advanceUntilIdle()

        val player = viewModel.uiState.value.players.first()
        val initialTokens = player.tokens

        viewModel.onAction(GameAction.AddToken(player))

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(initialTokens + 1, state.players.first().tokens)
        }
    }

    @Test
    fun `a track that arrived before the connection starts once it is there`() = runTest {
        // The playlist wins the race against the app remote, which is what happens now that a
        // stored session skips the login on start up.
        val viewModel = createViewModel(listOf("Alice"))
        advanceUntilIdle()
        verify(exactly = 0) { playerApi.play(any<String>()) }
        // Nothing is playing, so the button must not claim otherwise.
        assertEquals(MusicButtonItem.PLAY, viewModel.uiState.value.musicButton)

        viewModel.initSpotifyConnection(context, "client", "uri")
        advanceUntilIdle()

        // The playlist is shuffled, so only the fact that exactly one track started matters.
        verify(exactly = 1) { playerApi.play(any<String>()) }
        assertEquals(MusicButtonItem.PAUSE, viewModel.uiState.value.musicButton)
    }

    @Test
    fun `a track is not started twice when the connection was already there`() = runTest {
        val viewModel = createViewModel(listOf("Alice"))
        viewModel.initSpotifyConnection(context, "client", "uri")
        advanceUntilIdle()

        verify(exactly = 1) { playerApi.play(any<String>()) }
    }

    @Test
    fun `the play button stays honest while nothing is connected`() = runTest {
        val viewModel = createViewModel(listOf("Alice"))
        advanceUntilIdle()

        viewModel.onAction(GameAction.OnPlayClick)

        assertEquals(MusicButtonItem.PLAY, viewModel.uiState.value.musicButton)
        verify(exactly = 0) { playerApi.resume() }
    }

    @Test
    fun `no song is requested before the playlists arrived`() = runTest {
        createViewModel(listOf("Alice", "Bob"))
        advanceUntilIdle()

        verify(exactly = 0) { Log.e("GameViewModel", "Playlist has no items left.") }
        coVerify(exactly = 1) { fetchTrackDetailsUseCase(any()) }
    }

    @Test
    fun `the guess button waits for the song details`() = runTest {
        val viewModel = createViewModel(listOf("Alice", "Bob"))

        // The playlists and the song details are still on their way.
        assertFalse(viewModel.uiState.value.primaryButton.isVisible)

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.primaryButton.isVisible)
    }

    @Test
    fun `guessing before the song is loaded does nothing`() = runTest {
        val viewModel = createViewModel(listOf("Alice", "Bob"))

        viewModel.onAction(GameAction.Guess)

        val state = viewModel.uiState.value
        assertEquals(GamePhase.GUESSING, state.phase)
        assertFalse(state.isAddTokenButtonVisible)
        assertTrue(state.players.first { it.isCurrentPlayer }.songs.any { it is UnknownSong })
    }

    @Test
    fun `guess reveals immediately when nobody else can bet a token`() = runTest {
        val viewModel = createViewModel(listOf("Alice"))
        advanceUntilIdle()

        viewModel.onAction(GameAction.Guess)

        val state = viewModel.uiState.value
        assertEquals(GamePhase.REVEALED, state.phase)
        assertEquals(com.example.hitster.R.string.game_buttonNext, state.primaryButton.title)
    }

    @Test
    fun `guess opens the token placement phase when another player owns tokens`() = runTest {
        val viewModel = createViewModel(listOf("Alice", "Bob"))
        advanceUntilIdle()

        viewModel.onAction(GameAction.Guess)

        val state = viewModel.uiState.value
        assertEquals(GamePhase.TOKEN_PLACEMENT, state.phase)
        assertEquals(com.example.hitster.R.string.game_buttonReveal, state.primaryButton.title)
        assertTrue(state.tokenBets.isEmpty())
        // Nothing is revealed yet.
        assertTrue(state.currentSong != null)
    }

    @Test
    fun `placing a token spends it and occupies the gap`() = runTest {
        val viewModel = createViewModel(listOf("Alice", "Bob", "Carol"))
        advanceUntilIdle()
        viewModel.onAction(GameAction.Guess)

        val (bob, carol) = viewModel.uiState.value.players.filterNot { it.isCurrentPlayer }
        viewModel.onAction(GameAction.PlaceToken(bob.name, gapIndex = 0))
        // Carol aims at the very same gap, which is taken now.
        viewModel.onAction(GameAction.PlaceToken(carol.name, gapIndex = 0))

        val state = viewModel.uiState.value
        assertEquals(listOf(TokenBet(playerName = bob.name, gapIndex = 0)), state.tokenBets)
        assertEquals(bob.tokens - 1, state.players.first { it.name == bob.name }.tokens)
        assertEquals(carol.tokens, state.players.first { it.name == carol.name }.tokens)
    }

    @Test
    fun `the current player cannot bet a token on their own timeline`() = runTest {
        val viewModel = createViewModel(listOf("Alice", "Bob"))
        advanceUntilIdle()
        viewModel.onAction(GameAction.Guess)

        val currentPlayer = viewModel.uiState.value.players.first { it.isCurrentPlayer }
        viewModel.onAction(GameAction.PlaceToken(currentPlayer.name, gapIndex = 0))

        val state = viewModel.uiState.value
        assertTrue(state.tokenBets.isEmpty())
        assertEquals(currentPlayer.tokens, state.players.first { it.isCurrentPlayer }.tokens)
    }

    @Test
    fun `a token on the correct gap wins the card from the current player`() = runTest {
        // The song belongs in front of the starting card, the guess card sits behind it.
        val viewModel = createViewModel(listOf("Alice", "Bob"), songYearOffset = -5)
        advanceUntilIdle()
        viewModel.onAction(GameAction.Guess)

        val song = viewModel.uiState.value.currentSong!!
        val bob = viewModel.uiState.value.players.first { !it.isCurrentPlayer }
        viewModel.onAction(GameAction.PlaceToken(bob.name, gapIndex = 0))
        viewModel.onAction(GameAction.Reveal)

        val state = viewModel.uiState.value
        assertEquals(bob.name, state.tokenWinnerName)
        assertEquals(GamePhase.REVEALED, state.phase)
        assertEquals(listOf(TokenBet(bob.name, 0, isCorrect = true)), state.tokenBets)

        // Bob owns the card now, sorted into his own timeline.
        val bobSongs = state.players.first { it.name == bob.name }.songs.getPlacedSongs()
        assertEquals(2, bobSongs.size)
        assertEquals(
            bobSongs.map { it.releaseYear }.sorted(),
            bobSongs.map { it.releaseYear }
        )
        assertEquals(
            listOf(song.releaseYear),
            bobSongs.filter { it.correctLocation == true }.map { it.releaseYear }
        )

        // The current player placed it wrong and loses it on the next turn.
        val currentSongs = state.players.first { it.isCurrentPlayer }.songs.getPlacedSongs()
        assertTrue(currentSongs.any { it.releaseYear == song.releaseYear && it.correctLocation == false })
        viewModel.onAction(GameAction.NextPlayer)
        advanceUntilIdle()
        assertTrue(
            viewModel.uiState.value.players
                .first { it.name != bob.name }
                .songs.getPlacedSongs()
                .none { it.releaseYear == song.releaseYear }
        )
    }

    @Test
    fun `a correct guess keeps the card even when a token was placed`() = runTest {
        // The song belongs behind the starting card, which is where the guess card sits.
        val viewModel = createViewModel(listOf("Alice", "Bob"), songYearOffset = 5)
        advanceUntilIdle()
        viewModel.onAction(GameAction.Guess)

        val song = viewModel.uiState.value.currentSong!!
        val bob = viewModel.uiState.value.players.first { !it.isCurrentPlayer }
        viewModel.onAction(GameAction.PlaceToken(bob.name, gapIndex = 0))
        viewModel.onAction(GameAction.Reveal)

        val state = viewModel.uiState.value
        assertNull(state.tokenWinnerName)
        assertEquals(listOf(TokenBet(bob.name, 0, isCorrect = false)), state.tokenBets)

        val currentSongs = state.players.first { it.isCurrentPlayer }.songs.getPlacedSongs()
        assertTrue(currentSongs.any { it.releaseYear == song.releaseYear && it.correctLocation == true })
        // Bob only lost his token.
        assertEquals(1, state.players.first { it.name == bob.name }.songs.getPlacedSongs().size)
        assertEquals(bob.tokens - 1, state.players.first { it.name == bob.name }.tokens)
    }

    @Test
    fun `a token can be taken back as long as the song is not revealed`() = runTest {
        val viewModel = createViewModel(listOf("Alice", "Bob"))
        advanceUntilIdle()
        viewModel.onAction(GameAction.Guess)

        val bob = viewModel.uiState.value.players.first { !it.isCurrentPlayer }
        viewModel.onAction(GameAction.PlaceToken(bob.name, gapIndex = 0))
        viewModel.onAction(GameAction.TakeBackToken(gapIndex = 0))

        val state = viewModel.uiState.value
        assertTrue(state.tokenBets.isEmpty())
        assertEquals(bob.tokens, state.players.first { it.name == bob.name }.tokens)
    }

    @Test
    fun `a token cannot be taken back after the reveal`() = runTest {
        val viewModel = createViewModel(listOf("Alice", "Bob"), songYearOffset = 5)
        advanceUntilIdle()
        viewModel.onAction(GameAction.Guess)

        val bob = viewModel.uiState.value.players.first { !it.isCurrentPlayer }
        viewModel.onAction(GameAction.PlaceToken(bob.name, gapIndex = 0))
        viewModel.onAction(GameAction.Reveal)
        viewModel.onAction(GameAction.TakeBackToken(gapIndex = 0))

        val state = viewModel.uiState.value
        assertEquals(1, state.tokenBets.size)
        assertEquals(bob.tokens - 1, state.players.first { it.name == bob.name }.tokens)
    }

    @Test
    fun `a token in a second correct gap is handed back to its owner`() = runTest {
        // The song shares its year with the starting card, so the gap in front of it and the gap
        // behind it are both correct.
        val viewModel = createViewModel(listOf("Alice", "Bob"), songYearOffset = 0)
        advanceUntilIdle()
        viewModel.onAction(GameAction.Guess)

        val bob = viewModel.uiState.value.players.first { !it.isCurrentPlayer }
        viewModel.onAction(GameAction.PlaceToken(bob.name, gapIndex = 0))
        viewModel.onAction(GameAction.Reveal)

        val state = viewModel.uiState.value
        // The current player was right as well and keeps the card.
        assertNull(state.tokenWinnerName)
        assertEquals(2, state.players.first { it.isCurrentPlayer }.songs.getPlacedSongs().size)
        // Bob was right too, so his token comes back.
        assertEquals(listOf(bob.name), state.refundedTokenPlayerNames)
        assertEquals(bob.tokens, state.players.first { it.name == bob.name }.tokens)
    }

    @Test
    fun `the winner's timeline is shown right after the reveal`() = runTest {
        val viewModel = createViewModel(listOf("Alice", "Bob"), songYearOffset = -5)
        advanceUntilIdle()
        viewModel.onAction(GameAction.Guess)

        val bob = viewModel.uiState.value.players.first { !it.isCurrentPlayer }
        viewModel.onAction(GameAction.PlaceToken(bob.name, gapIndex = 0))
        viewModel.onAction(GameAction.Reveal)

        val state = viewModel.uiState.value
        assertEquals(bob.name, state.tokenWinnerName)
        assertEquals(listOf(bob.name), state.players.filter { it.isSelected }.map { it.name })
        // Moving on stays possible although another timeline is on screen.
        assertTrue(state.primaryButton.isVisible)
        assertTrue(state.isAddTokenButtonVisible)
    }

    @Test
    fun `a bet behind the guess card follows the revealed card`() = runTest {
        val viewModel = createViewModel(listOf("Alice", "Bob"), songYearOffset = -5)
        advanceUntilIdle()
        // Move the guess card in front of the starting card, so gap 1 is behind it.
        viewModel.onAction(GameAction.MoveLeft)
        viewModel.onAction(GameAction.Guess)

        val bob = viewModel.uiState.value.players.first { !it.isCurrentPlayer }
        viewModel.onAction(GameAction.PlaceToken(bob.name, gapIndex = 1))
        viewModel.onAction(GameAction.Reveal)

        val state = viewModel.uiState.value
        // The current player was right, so the token was wrong and the timeline grew by one card.
        assertNull(state.tokenWinnerName)
        assertEquals(2, state.players.first { it.isCurrentPlayer }.songs.getPlacedSongs().size)
        // Gap 1 sat behind the guess card and is gap 2 of the revealed timeline.
        assertEquals(listOf(TokenBet(bob.name, gapIndex = 2, isCorrect = false)), state.tokenBets)
    }

    @Test
    fun `the next turn resets the token round`() = runTest {
        val viewModel = createViewModel(listOf("Alice", "Bob"), songYearOffset = -5)
        advanceUntilIdle()
        viewModel.onAction(GameAction.Guess)
        val bob = viewModel.uiState.value.players.first { !it.isCurrentPlayer }
        viewModel.onAction(GameAction.PlaceToken(bob.name, gapIndex = 0))
        viewModel.onAction(GameAction.Reveal)

        viewModel.onAction(GameAction.NextPlayer)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(GamePhase.GUESSING, state.phase)
        assertTrue(state.tokenBets.isEmpty())
        assertNull(state.tokenWinnerName)
        assertEquals(com.example.hitster.R.string.game_buttonGuess, state.primaryButton.title)
    }

    /**
     * @param songYearOffset the year of every loaded song, relative to the starting card of the
     * player whose turn it is. A negative offset means the song belongs in front of the starting
     * card (gap 0), a positive one behind it (gap 1), which is where the guess card starts out.
     */
    private fun createViewModel(
        playerNames: List<String>,
        songYearOffset: Int? = null
    ): GameViewModel {
        lateinit var viewModel: GameViewModel
        if (songYearOffset != null) {
            coEvery { fetchTrackDetailsUseCase(any()) } coAnswers {
                val startYear = viewModel.uiState.value.players
                    .first { it.isCurrentPlayer }
                    .songs.getPlacedSongs()
                    .first()
                    .releaseYear
                Song("Test".toText(), "Artist".toText(), startYear + songYearOffset)
            }
        }
        viewModel = GameViewModel(
            playerNames = playerNames,
            playlists = listOf("playlist_id"),
            fetchTrackDetailsUseCase = fetchTrackDetailsUseCase,
            fetchPlaylistTracksUseCase = fetchPlaylistTracksUseCase,
            connectToSpotifyUseCase = connectToSpotifyUseCase
        )
        return viewModel
    }
}

