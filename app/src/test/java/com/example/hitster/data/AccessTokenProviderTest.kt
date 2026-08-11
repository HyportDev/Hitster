package com.example.hitster.data

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@OptIn(ExperimentalCoroutinesApi::class)
class AccessTokenProviderTest {

    private val storage: SpotifyTokenStorage = mockk(relaxed = true)

    private var now = 1_000_000L

    private fun createProvider(dispatcher: TestDispatcher) = AccessTokenProvider(
        storage = storage,
        ioDispatcher = dispatcher,
        now = { now }
    )

    @Test
    fun `a token that is still good is handed out`() = runTest {
        every { storage.load() } returns tokens(accessToken = "stored", expiresInMillis = 600_000)
        val provider = createProvider(StandardTestDispatcher(testScheduler))

        assertEquals("stored", provider.getAccessToken())
        assertFalse(provider.needsAuthorization())
    }

    @Test
    fun `an expired session is dropped instead of handing out a dead token`() = runTest {
        every { storage.load() } returns tokens(accessToken = "old", expiresInMillis = 0)
        val provider = createProvider(StandardTestDispatcher(testScheduler))

        assertThrows<SpotifyAuthException> { provider.getAccessToken() }

        verify { storage.clear() }
        assertEquals(false, provider.isAuthorized.value)
        assertTrue(provider.needsAuthorization())
    }

    @Test
    fun `a token about to run out counts as expired`() = runTest {
        // Inside the safety margin: it would very likely die mid request.
        every { storage.load() } returns tokens(accessToken = "almost", expiresInMillis = 30_000)
        val provider = createProvider(StandardTestDispatcher(testScheduler))

        assertTrue(provider.needsAuthorization())
        assertThrows<SpotifyAuthException> { provider.getAccessToken() }
    }

    @Test
    fun `a session that runs out during the game stops being handed out`() = runTest {
        every { storage.load() } returns tokens(accessToken = "stored", expiresInMillis = 600_000)
        val provider = createProvider(StandardTestDispatcher(testScheduler))
        assertEquals("stored", provider.getAccessToken())

        now += 600_000

        assertThrows<SpotifyAuthException> { provider.getAccessToken() }
    }

    @Test
    fun `without a stored session a login is needed`() = runTest {
        every { storage.load() } returns null
        val provider = createProvider(StandardTestDispatcher(testScheduler))

        assertThrows<SpotifyAuthException> { provider.getAccessToken() }
        assertTrue(provider.needsAuthorization())
    }

    @Test
    fun `a fresh login is stored and usable right away`() = runTest {
        every { storage.load() } returns null
        val provider = createProvider(StandardTestDispatcher(testScheduler))

        provider.setSession(accessToken = "fresh", expiresInSeconds = 3600)

        assertEquals("fresh", provider.getAccessToken())
        assertEquals(true, provider.isAuthorized.value)
        assertFalse(provider.needsAuthorization())
        verify {
            storage.save(
                match { it.accessToken == "fresh" && it.expiresAtMillis == now + 3_600_000 }
            )
        }
    }

    @Test
    fun `the stored session is only read once`() = runTest {
        every { storage.load() } returns tokens(accessToken = "stored", expiresInMillis = 600_000)
        val provider = createProvider(StandardTestDispatcher(testScheduler))

        provider.getAccessToken()
        provider.getAccessToken()
        provider.needsAuthorization()

        verify(exactly = 1) { storage.load() }
    }

    @Test
    fun `signing out clears the stored session`() = runTest {
        every { storage.load() } returns tokens(accessToken = "stored", expiresInMillis = 600_000)
        val provider = createProvider(StandardTestDispatcher(testScheduler))
        provider.getAccessToken()

        provider.signOut()

        verify { storage.clear() }
        assertThrows<SpotifyAuthException> { provider.getAccessToken() }
    }

    private fun tokens(accessToken: String = "token", expiresInMillis: Long) = SpotifyTokens(
        accessToken = accessToken,
        expiresAtMillis = now + expiresInMillis
    )
}
