package com.example.hitster.game.usecase

import com.example.hitster.data.AccessTokenProvider
import com.example.hitster.game.model.Song
import com.example.hitster.res.toText
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class FetchTrackDetailsUseCase(
    private val accessTokenProvider: AccessTokenProvider,
    private val findYearUseCase: FindEarliestReleaseYearUseCase
) {

    suspend operator fun invoke(trackUri: String): Song? = withContext(Dispatchers.IO) {
        val accessToken = accessTokenProvider.getAccessToken() ?: return@withContext null

        val trackId = trackUri.split(":").last()
        val url = "https://api.spotify.com/v1/tracks/$trackId"

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val responseBody = response.body?.string() ?: return@withContext null
            val json = JsonParser.parseString(responseBody).asJsonObject

            val title = json["name"].asString
            val artist = json["artists"].asJsonArray[0].asJsonObject["name"].asString
            val releaseYear = json["album"].asJsonObject["release_date"].asString.take(4).toInt()

            val songName = cleanSongTitle(title)
            val actualYear = findYearUseCase(songName, artist)

            return@withContext Song(songName.toText(), artist.toText(), actualYear ?: releaseYear)

        } catch (e: Exception) {
            return@withContext null
        }
    }

    private fun cleanSongTitle(title: String): String {
        // Split by the first occurrence of a dash and take the part before it
        val cleanPart = title.split("-")[0]

        // Remove any remaster, year, or extra details within the clean part
        val cleanedTitle = cleanPart.replace(Regex("(?i)\\s*(\\bremaster(?:ed)?\\b|\\b\\d{4}\\b)"), "").trim()

        // Remove extra spaces caused by replacements
        return cleanedTitle.replace(Regex("\\s+"), " ")
    }
}
