package com.example.hitster.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Keeps the session across app starts, so a restart within the hour does not mean another login.
 *
 * The file is app private and excluded from backup and device transfer, see backup_rules.xml and
 * data_extraction_rules.xml. It is not encrypted on top of that: the access token replaces itself
 * within the hour, and reading these preferences already requires root.
 *
 * Every method touches the disk and belongs on a background dispatcher.
 */
class SpotifyTokenStorage(context: Context) {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun load(): SpotifyTokens? {
        val accessToken = preferences.getString(KEY_ACCESS_TOKEN, null) ?: return null
        return SpotifyTokens(
            accessToken = accessToken,
            expiresAtMillis = preferences.getLong(KEY_EXPIRES_AT, 0L)
        )
    }

    fun save(tokens: SpotifyTokens) {
        preferences.edit()
            .putString(KEY_ACCESS_TOKEN, tokens.accessToken)
            .putLong(KEY_EXPIRES_AT, tokens.expiresAtMillis)
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val FILE_NAME = "spotify_session"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_EXPIRES_AT = "expires_at"
    }
}
