package com.example.hitster.data

/**
 * The implicit grant hands out an access token and nothing else, so there is no way to renew this
 * without sending the user through the login again.
 */
data class SpotifyTokens(
    val accessToken: String,
    val expiresAtMillis: Long
) {
    /**
     * A token that is about to run out counts as expired, so a request never starts on one that
     * dies while it is in flight.
     */
    fun isUsableAt(nowMillis: Long): Boolean = nowMillis + EXPIRY_MARGIN_MILLIS < expiresAtMillis

    private companion object {
        const val EXPIRY_MARGIN_MILLIS = 60_000L
    }
}

/** Raised when there is no usable Spotify session, which only a new login can fix. */
class SpotifyAuthException(message: String, cause: Throwable? = null) : Exception(message, cause)
