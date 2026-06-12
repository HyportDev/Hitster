package com.example.hitster

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.hitster.data.AccessTokenProvider
import com.example.hitster.game.GameViewModel
import com.example.hitster.game.Routes.Game
import com.example.hitster.game.Routes.Home
import com.example.hitster.game.ui.GameScreen
import com.example.hitster.home.HomeViewModel
import com.example.hitster.home.model.PlaylistViewState
import com.example.hitster.home.ui.HomeScreen
import com.example.hitster.ui.HitsterTheme
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

class MainActivity : ComponentActivity() {

    private val accessTokenProvider: AccessTokenProvider by inject()
    private val mainViewModel: MainViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App()
        }
    }

    @Composable
    private fun App() {
        val navController = rememberNavController()
        HitsterTheme {
            LaunchedEffect(Unit) {
                auth()
            }
            Surface(color = MaterialTheme.colorScheme.background) {
                NavHost(
                    navController = navController,
                    startDestination = Home,
                    modifier = Modifier.padding(WindowInsets.safeDrawing.asPaddingValues())
                ) {
                    composable<Home> {
                        val viewModel: HomeViewModel = koinViewModel()
                        val state by viewModel.viewState.collectAsStateWithLifecycle()
                        val spotifyState by mainViewModel.spotifyState.collectAsStateWithLifecycle()
                        HomeScreen(
                            state = state,
                            spotifyState = spotifyState,
                            event = viewModel.event,
                            onAction = viewModel::onAction,
                            onStartGame = {
                                navController.navigate(Game(
                                    playerNames = state.players,
                                    playlists = state.playlists
                                        .filterIsInstance<PlaylistViewState.Success>()
                                        .map { it.playlist.id }
                                ))
                            }
                        )
                    }
                    composable<Game>(
                        enterTransition = {
                            slideIntoContainer(
                                animationSpec = tween(300, easing = EaseIn),
                                towards = AnimatedContentTransitionScope.SlideDirection.Start
                            )
                        }
                    ) { backStackEntry ->
                        val args = backStackEntry.toRoute<Game>()
                        val viewModel: GameViewModel = koinViewModel{
                            parametersOf(args.playerNames, args.playlists)
                        }
                        LaunchedEffect(backStackEntry) {
                            viewModel.initSpotifyConnection(CLIENT_ID, REDIRECT_URI)
                        }
                        DisposableEffect(backStackEntry) {
                            onDispose { viewModel.closeSpotifyConnection() }
                        }
                        val state by viewModel.uiState.collectAsStateWithLifecycle()
                        GameScreen(
                            state = state,
                            onAction = viewModel::onAction
                        )
                    }
                }
            }
        }
    }

    private fun auth() {
        val scopes = arrayOf(
            "streaming",
            "user-read-private",
            "playlist-read-private",
            "playlist-read-collaborative",
            "user-library-read",
            //"user-read-email"
        )

        val builder: AuthorizationRequest.Builder = AuthorizationRequest.Builder(
            CLIENT_ID,
            AuthorizationResponse.Type.TOKEN,
            REDIRECT_URI
        )

        builder.setScopes(scopes)
        builder.setShowDialog(true)
        val request: AuthorizationRequest = builder.build()
        AuthorizationClient.openLoginActivity(this, REQUEST_CODE, request)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE) {
            val response: AuthorizationResponse = AuthorizationClient.getResponse(resultCode, data)

            if (response.type == AuthorizationResponse.Type.TOKEN) {
                accessTokenProvider.setAccessToken(response.accessToken)
                mainViewModel.setSpotifyState(true)
                Log.d("MainActivity", "Access token retrieved!")
            } else if (response.type == AuthorizationResponse.Type.ERROR) {
                mainViewModel.setSpotifyState(false)
                Log.d("MainActivity", "Error during authorization: ${response.error}")
            }
        }
    }

    companion object {
        private const val CLIENT_ID = "41a8741aa1774af5ab5ec8973bcf1a39"
        private const val REDIRECT_URI = "digital-hitster-app://spotify-callback"
        private const val REQUEST_CODE = 1337
    }
}