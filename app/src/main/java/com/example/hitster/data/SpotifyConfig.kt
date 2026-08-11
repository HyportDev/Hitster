package com.example.hitster.data

import com.example.hitster.BuildConfig

/**
 * The client id is public by design: it ships inside the APK and shows up in every authorization
 * URL. What actually guards the app is the package name and signing fingerprint Spotify verifies
 * on top of it. There is no client secret anywhere.
 *
 * It still comes from local.properties rather than from here, because a Spotify app is tied to one
 * package name and fingerprint, so every developer needs their own. See app/build.gradle.kts.
 */
data class SpotifyConfig(
    val clientId: String,
    val redirectUri: String,
    val scopes: List<String>
) {
    companion object {
        val Default = SpotifyConfig(
            clientId = BuildConfig.SPOTIFY_CLIENT_ID,
            redirectUri = "digital-hitster-app://spotify-callback",
            scopes = listOf(
                // Required by the App Remote SDK. Without it the service binds, but Spotify never
                // answers the handshake and the connection hangs without an error.
                "app-remote-control",
                "streaming",
                "user-read-private",
                "playlist-read-private",
                "playlist-read-collaborative",
                "user-library-read"
            )
        )
    }
}
