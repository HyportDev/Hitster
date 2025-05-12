package com.example.hitster.game.usecase

import android.content.Context
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class ConnectToSpotifyUseCase(
    private val context: Context
) {
    suspend operator fun invoke(
        clientId: String,
        redirectUri: String
    ): Result<SpotifyAppRemote> = suspendCancellableCoroutine { cont ->
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
                if (cont.isActive) {
                    cont.resume(Result.failure(throwable))
                }
            }
        })
    }
}