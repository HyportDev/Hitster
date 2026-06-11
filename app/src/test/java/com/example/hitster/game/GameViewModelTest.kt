package com.example.hitster.game

import app.cash.turbine.test
import com.example.hitster.game.model.GameAction
import com.example.hitster.game.model.MusicButtonItem
import com.example.hitster.game.model.Song
import com.example.hitster.game.usecase.ConnectToSpotifyUseCase
import com.example.hitster.game.usecase.FetchPlaylistTracksUseCase
import com.example.hitster.game.usecase.FetchTrackDetailsUseCase
import com.example.hitster.res.toText
import com.spotify.android.appremote.api.PlayerApi
import com.spotify.android.appremote.api.SpotifyAppRemote
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Spotify Mocks vorbereiten
        every { spotifyAppRemote.playerApi } returns playerApi
        coEvery { connectToSpotifyUseCase(any(), any()) } returns Result.success(spotifyAppRemote)

        // Standard-Verhalten für UseCases
        coEvery { fetchPlaylistTracksUseCase(any()) } returns listOf("spotify:track:1", "spotify:track:2")
        coEvery { fetchTrackDetailsUseCase(any()) } returns Song("Title".toText(), "Artist".toText(), 2000)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
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
            verify { fetchPlaylistTracksUseCase(any()) }
        }
    }

    @Test
    fun `initSpotifyConnection updates remote and sets repeat mode`() = runTest {
        val viewModel = createViewModel(listOf("Alice"))

        viewModel.initSpotifyConnection("client", "uri")
        advanceUntilIdle()

        verify { connectToSpotifyUseCase("client", "uri") }
        verify { playerApi.setRepeat(any()) }
    }

    @Test
    fun `onPauseClick calls spotify pause and updates state`() = runTest {
        val viewModel = createViewModel(listOf("Alice"))
        viewModel.initSpotifyConnection("client", "uri")
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
    fun `loginGuess updates player songs and moves to next state`() = runTest {
        val viewModel = createViewModel(listOf("Alice"))
        advanceUntilIdle()

        // Simuliere einen aktuellen Song
        val testSong = Song("Test".toText(), "Artist".toText(), 1990)
        coEvery { fetchTrackDetailsUseCase(any()) } returns testSong

        // Wir brauchen einen Song im State, um zu raten
        viewModel.onAction(GameAction.NextPlayer)
        advanceUntilIdle()

        viewModel.onAction(GameAction.Guess)

        viewModel.uiState.test {
            val state = awaitItem()
            // Nach Guess sollte der Button auf "Next Player" wechseln
            assertEquals(com.example.hitster.R.string.game_buttonNext, state.primaryButton.title)
        }
    }

    private fun createViewModel(playerNames: List<String>): GameViewModel {
        return GameViewModel(
            playerNames = playerNames,
            playlists = listOf("playlist_id"),
            fetchTrackDetailsUseCase = fetchTrackDetailsUseCase,
            fetchPlaylistTracksUseCase = fetchPlaylistTracksUseCase,
            connectToSpotifyUseCase = connectToSpotifyUseCase
        )
    }
}