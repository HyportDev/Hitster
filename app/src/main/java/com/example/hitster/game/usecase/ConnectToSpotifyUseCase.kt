package com.example.hitster.game.usecase

import android.content.Context
import android.util.Log
import com.example.hitster.BuildConfig
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class ConnectToSpotifyUseCase {

    /**
     * @param context must be an Activity. The first connection for a client id makes Spotify ask
     * the user for permission, and that dialog cannot be shown from an application context - the
     * connection then never calls back at all.
     */
    suspend operator fun invoke(
        context: Context,
        clientId: String,
        redirectUri: String
    ): Result<SpotifyAppRemote> = suspendCancellableCoroutine { cont ->
        // The SDK keeps its own trace of the handshake, which is the only view into what happens
        // between asking to connect and getting a callback.
        SpotifyAppRemote.setDebugMode(BuildConfig.DEBUG)

        val connectionParams = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(context, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                if (cont.isActive) {
                    cont.resume(Result.success(appRemote))
                }
            }

            override fun onFailure(throwable: Throwable) {
                Log.e(LOG_TAG, "Spotify connection failed", throwable)
                if (cont.isActive) {
                    cont.resume(Result.failure(throwable))
                }
            }
        })
    }

    private companion object {
        const val LOG_TAG = "ConnectToSpotify"
    }
}