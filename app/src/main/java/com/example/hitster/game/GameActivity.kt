package com.example.hitster.game

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hitster.game.data.AccessTokenProvider
import com.example.hitster.game.model.GameAction
import com.example.hitster.game.model.GameAction.*
import com.example.hitster.game.view.GameScreen
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote

class GameActivity : AppCompatActivity() {

    private val viewModel: GameViewModel by lazy {
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return GameViewModel(
                    accessTokenProvider = accessTokenProvider
                ) as T
            }
        })[GameViewModel::class.java]
    }

    private var spotifyAppRemote: SpotifyAppRemote? = null
    private val accessTokenProvider = AccessTokenProvider()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent.getStringExtra("accessToken")?.let {
            accessTokenProvider.setAccessToken(it)
        }

        val players = intent.getStringArrayListExtra("players")?.toMutableList() ?: mutableListOf()
        if (players.isEmpty()) {
            players.add("Player 1")
        }
        players.shuffle()
        val playlists = intent.getStringArrayListExtra("playlists")?.toMutableList() ?: mutableListOf()
        if (playlists.isEmpty()) {
            playlists.add("2u0vgWYqU1TWVcDehJnZuN")
        }
        viewModel.init(players, playlists, resources)

        setContent {
            val state = viewModel.viewState.collectAsStateWithLifecycle().value
            MaterialTheme {
                GameScreen(state = state, onAction = ::onAction)
            }
        }
    }

    private fun onAction(action: GameAction) {
        when(action) {
            OnPauseClick -> viewModel.pauseMusic()
            OnPlayClick -> viewModel.playMusic()
            MoveLeft -> viewModel.moveGuessCardLeft()
            MoveRight -> viewModel.moveGuessCardRight()
            Guess -> viewModel.loginGuess()
            is OnSelectPlayer -> viewModel.clickPlayer(action.playerItem)
            NextPlayer -> viewModel.nextPlayer()
        }
    }

    override fun onStart() {
        super.onStart()
        val connectionParams = ConnectionParams.Builder(CLIENT_ID)
            .setRedirectUri(REDIRECT_URI)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(this, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                viewModel.setSpotifyAppRemote(appRemote)
                Log.d("MainActivity", "Connected to Spotify App Remote!")
            }

            override fun onFailure(throwable: Throwable) {
                Log.e("MainActivity", "Failed to connect: ${throwable.message}")
            }
        })
    }

    override fun onStop() {
        super.onStop()
        spotifyAppRemote?.let {
            SpotifyAppRemote.disconnect(it)
        }

    }

    companion object {
        private const val CLIENT_ID = "41a8741aa1774af5ab5ec8973bcf1a39"
        private const val REDIRECT_URI = "digital-hitster-app://spotify-callback"
    }
}