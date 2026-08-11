package com.example.hitster.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The single place that hands out a Spotify access token, and the only one that knows whether it
 * is still good for anything.
 *
 * The login uses the implicit grant, which returns no refresh token: an expired session is dropped
 * and can only be replaced by another login. Everything else about it is kept honest, so callers
 * get a clear failure instead of a token the server will reject.
 */
class AccessTokenProvider(
    private val storage: SpotifyTokenStorage,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val now: () -> Long = { System.currentTimeMillis() }
) {

    private val mutex = Mutex()
    private var tokens: SpotifyTokens? = null
    private var isLoaded = false

    private val _isAuthorized = MutableStateFlow<Boolean?>(null)

    /** Null until the stored session has been read, then whether there is a usable one. */
    val isAuthorized = _isAuthorized.asStateFlow()

    /** True when the user has to go through the login, because nothing usable is stored. */
    suspend fun needsAuthorization(): Boolean = mutex.withLock {
        loadOnce()
        tokens?.isUsableAt(now()) != true
    }

    /**
     * The token for the next request.
     *
     * @throws SpotifyAuthException when the session is missing or has run out.
     */
    suspend fun getAccessToken(): String = mutex.withLock {
        loadOnce()
        val current = tokens
        if (current == null || !current.isUsableAt(now())) {
            // Keeping a dead token around would only produce 401s later on.
            if (current != null) forget()
            throw SpotifyAuthException("No usable Spotify session")
        }
        current.accessToken
    }

    /** Takes the result of a successful login. [expiresInSeconds] comes from Spotify. */
    suspend fun setSession(accessToken: String, expiresInSeconds: Int) = mutex.withLock {
        store(
            SpotifyTokens(
                accessToken = accessToken,
                expiresAtMillis = now() + expiresInSeconds * 1000L
            )
        )
    }

    suspend fun signOut() = mutex.withLock { forget() }

    private suspend fun loadOnce() {
        if (isLoaded) return
        tokens = withContext(ioDispatcher) { storage.load() }
        isLoaded = true
        _isAuthorized.value = tokens?.isUsableAt(now()) == true
    }

    private suspend fun store(newTokens: SpotifyTokens) {
        tokens = newTokens
        isLoaded = true
        withContext(ioDispatcher) { storage.save(newTokens) }
        _isAuthorized.value = true
    }

    private suspend fun forget() {
        tokens = null
        isLoaded = true
        withContext(ioDispatcher) { storage.clear() }
        _isAuthorized.value = false
    }
}
