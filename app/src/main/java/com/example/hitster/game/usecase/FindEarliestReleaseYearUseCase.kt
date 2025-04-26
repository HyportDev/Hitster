package com.example.hitster.game.usecase

import com.example.hitster.game.data.AccessTokenProvider
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

class FindEarliestReleaseYearUseCase(
    private val accessTokenProvider: AccessTokenProvider
) {

    suspend operator fun invoke(songName: String, artistName: String): Int? = withContext(Dispatchers.IO) {
        val accessToken = accessTokenProvider.getAccessToken() ?: return@withContext null

        val query = buildString {
            append(URLEncoder.encode(songName, "UTF-8"))
            if (artistName.isNotBlank()) {
                append("+artist:${URLEncoder.encode(artistName, "UTF-8")}")
            }
        }

        val url = "https://api.spotify.com/v1/search?q=$query&type=track&limit=10"

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val json = JsonParser.parseString(body).asJsonObject
            val tracks = json.getAsJsonObject("tracks").getAsJsonArray("items")

            tracks.mapNotNull { track ->
                track.asJsonObject
                    .getAsJsonObject("album")
                    .get("release_date")
                    .asString
                    .take(4)
                    .toIntOrNull()
            }.minOrNull()

        } catch (e: Exception) {
            null
        }
    }
}
