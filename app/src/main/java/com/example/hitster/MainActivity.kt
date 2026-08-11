package com.example.hitster

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
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
import com.example.hitster.data.SpotifyConfig
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

    private val spotifyConfig = SpotifyConfig.Default

    private val loginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> onLoginResult(result) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authorizeIfNeeded()
        // The app is dark regardless of the system setting, so the bar icons have to stay light.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            App()
        }
    }

    @Composable
    private fun App() {
        val navController = rememberNavController()
        HitsterTheme {
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
                            // The activity, not the application context: Spotify has to be able to
                            // put its consent dialog on screen.
                            viewModel.initSpotifyConnection(
                                this@MainActivity,
                                spotifyConfig.clientId,
                                spotifyConfig.redirectUri
                            )
                        }
                        DisposableEffect(backStackEntry) {
                            onDispose { viewModel.closeSpotifyConnection() }
                        }
                        val state by viewModel.uiState.collectAsStateWithLifecycle()
                        GameScreen(
                            state = state,
                            event = viewModel.event,
                            onAction = viewModel::onAction
                        )
                    }
                }
            }
        }
    }

    /**
     * Only asks for a login when the stored session is gone or has run out, so a restart within
     * the hour goes straight into the app.
     */
    private fun authorizeIfNeeded() {
        lifecycleScope.launch {
            if (accessTokenProvider.needsAuthorization()) startLogin()
        }
    }

    /**
     * The implicit grant is what the app to app login supports: the Spotify app receives the
     * request as intent extras, and those carry no room for the PKCE challenge a code exchange
     * would need.
     */
    private fun startLogin() {
        val request = AuthorizationRequest.Builder(
            spotifyConfig.clientId,
            AuthorizationResponse.Type.TOKEN,
            spotifyConfig.redirectUri
        )
            .setScopes(spotifyConfig.scopes.toTypedArray())
            .setShowDialog(true)
            .build()

        loginLauncher.launch(AuthorizationClient.createLoginActivityIntent(this, request))
    }

    private fun onLoginResult(result: ActivityResult) {
        val response = AuthorizationClient.getResponse(result.resultCode, result.data)
        when (response.type) {
            AuthorizationResponse.Type.TOKEN -> lifecycleScope.launch {
                accessTokenProvider.setSession(response.accessToken, response.expiresIn)
            }

            AuthorizationResponse.Type.ERROR ->
                Log.e(LOG_TAG, "Spotify refused the login: ${response.error}")

            else -> Log.d(LOG_TAG, "Login ended without a token (${response.type})")
        }
    }

    private companion object {
        const val LOG_TAG = "MainActivity"
    }
}