package com.example.hitster.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hitster.game.GameActivity
import com.example.hitster.home.model.HomeAction
import com.example.hitster.home.model.HomeAction.AddPlayer
import com.example.hitster.home.model.HomeAction.AddPlaylistByLink
import com.example.hitster.home.model.HomeAction.AddPlaylistFromLibrary
import com.example.hitster.home.model.HomeAction.AddPlaylists
import com.example.hitster.home.model.HomeAction.DismissDialog
import com.example.hitster.home.model.HomeAction.OnDialogItemChecked
import com.example.hitster.home.model.HomeAction.OnPlayerInputChange
import com.example.hitster.home.model.HomeAction.OpenRemovePlayerDialog
import com.example.hitster.home.model.HomeAction.RemovePlayer
import com.example.hitster.home.model.HomeAction.RemovePlaylist
import com.example.hitster.home.model.PlaylistViewState
import com.example.hitster.home.view.HomeScreen
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse

class HomeActivity : AppCompatActivity() {

    private val viewModel: HomeViewModel by viewModels()

    private var accessToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth()
        setContent {
            val state = viewModel.viewState.collectAsStateWithLifecycle().value
            MaterialTheme {
                HomeScreen(
                    state = state,
                    event = viewModel.event,
                    onAction = ::onAction,
                    onStartGame = {
                        startGame(
                            players = state.players,
                            playlists = state.playlists
                                .filterIsInstance<PlaylistViewState.Success>()
                                .map { it.playlist.id }
                        )
                    }
                )
            }
        }
    }

    private fun onAction(action: HomeAction) {
        when (action) {
            is AddPlayer -> viewModel.addPlayer(action.name)
            AddPlaylistByLink -> showEnterPlaylistLinkDialog()
            AddPlaylistFromLibrary -> viewModel.loadUserPlaylists(accessToken, resources)
            DismissDialog -> viewModel.closeDialog()
            is OnPlayerInputChange -> viewModel.changePlayerInput(action.value)
            is OpenRemovePlayerDialog -> viewModel.openRemovePlayerDialog(action.name, resources)
            is RemovePlayer -> {
                viewModel.closeDialog()
                viewModel.removePlayer(action.name)
            }
            is RemovePlaylist -> viewModel.removePlaylist(action.playlist)
            is AddPlaylists -> {
                viewModel.closeDialog()
                viewModel.addPlaylists(action.playlists)
            }
            is OnDialogItemChecked -> viewModel.toggleSelectionValue(action.item, action.checked)
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

    private fun startGame(players: List<String>, playlists: List<String>) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putStringArrayListExtra("players", ArrayList(players))
        intent.putStringArrayListExtra("playlists", ArrayList(playlists))
        intent.putExtra("accessToken", accessToken)
        startActivity(intent)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE) {
            val response: AuthorizationResponse = AuthorizationClient.getResponse(resultCode, data)

            if (response.type == AuthorizationResponse.Type.TOKEN) {
                accessToken = response.accessToken
                Log.d("MainActivity", "Access token retrieved!")
                //connectToSpotifyRemote()
            } else if (response.type == AuthorizationResponse.Type.ERROR) {
                Log.d("MainActivity", "Error during authorization: ${response.error}")
            }
        }
    }

    // 🚀 Shows an input dialog for entering a playlist link
    private fun showEnterPlaylistLinkDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter Playlist Link")

        val input = EditText(this)
        input.hint = "Paste Spotify playlist link"
        builder.setView(input)

        builder.setPositiveButton("Add") { _, _ ->
            val playlistLink = input.text.toString().trim()
            if (playlistLink.startsWith("https://open.spotify.com/playlist/")) {
                val id = playlistLink.split("playlist/")[1].split("?")[0]
                viewModel.addPlaylist(id, accessToken)
            } else {
                Toast.makeText(this, "Invalid playlist link. Try again.", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    companion object {
        private const val CLIENT_ID = "41a8741aa1774af5ab5ec8973bcf1a39"
        private const val REDIRECT_URI = "digital-hitster-app://spotify-callback"
        private const val REQUEST_CODE = 1337
    }
}