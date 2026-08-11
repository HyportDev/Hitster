package com.example.hitster.game.usecase

import android.util.Log
import com.example.hitster.data.AccessTokenProvider
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class FetchPlaylistTracksUseCase(
    private val accessTokenProvider: AccessTokenProvider
) {

    /** @throws com.example.hitster.data.SpotifyAuthException when there is no usable session. */
    suspend operator fun invoke(playlistIds: List<String>): List<String> = withContext(Dispatchers.IO) {
        val accessToken = accessTokenProvider.getAccessToken()

        val trackURIs = mutableListOf<String>()

        for (playlistId in playlistIds) {
            var url = "https://api.spotify.com/v1/playlists/$playlistId/tracks"

            do {
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .build()

                try {
                    val response = OkHttpClient().newCall(request).execute()
                    val responseBody = response.body?.string()

                    if (response.isSuccessful && responseBody != null) {
                        val json = JsonParser.parseString(responseBody).asJsonObject
                        val items = json.getAsJsonArray("items")

                        items?.forEach { item ->
                            val track = item.asJsonObject.getAsJsonObject("track")
                            val uri = track?.get("uri")?.asString
                            if (uri != null) {
                                trackURIs.add(uri)
                            }
                        }

                        url = json.get("next")?.asString ?: ""
                    } else {
                        Log.e("FetchPlaylistTracksUseCase", "Error fetching playlist: ${response.code}")
                        Log.e("FetchPlaylistTracksUseCase", "Response body: $responseBody")
                        url = ""
                    }
                } catch (e: Exception) {
                    Log.e("FetchPlaylistTracksUseCase", "Exception: ${e.message}", e)
                    url = ""
                }
            } while (url.isNotEmpty())
        }

        trackURIs
    }
}
