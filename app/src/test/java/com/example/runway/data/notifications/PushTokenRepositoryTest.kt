package com.example.runway.data.notifications

import com.example.runway.data.remote.api.SendReportDto
import com.example.runway.fake.FakeRunwayApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

// SCRUM-163: keeping the API's copy of this phone's FCM token right, including when things fail.
class PushTokenRepositoryTest {

    private val api = FakeRunwayApi()

    private fun repository(signedIn: Boolean = true, token: String = "token-1") = PushTokenRepository(
        api = api,
        isSignedIn = { signedIn },
        fetchToken = { token },
    )

    @Test fun signingInSendsTheTokenToTheApi() = runTest {
        val result = repository().register()
        assertTrue(result.isSuccess)
        assertEquals(listOf("token-1"), api.registeredTokens)
    }

    @Test fun aTokenFromFcmIsSentAsIs() = runTest {
        repository().register(token = "fresh-token")
        assertEquals(listOf("fresh-token"), api.registeredTokens)
    }

    @Test fun nothingIsSentWhileSignedOut() = runTest {
        // FCM can hand over a token before anyone signs in. Sign-in sends it later.
        val result = repository(signedIn = false).register(token = "early-token")
        assertTrue(result.isSuccess)
        assertTrue(api.registeredTokens.isEmpty())
    }

    @Test fun anOfflineRegistrationFailsQuietly() = runTest {
        api.pushError = IOException("offline")
        val result = repository().register()
        assertTrue(result.isFailure)
    }

    @Test fun signingOutRemovesTheToken() = runTest {
        val removed = repository().unregister()
        assertTrue(removed)
        assertEquals(listOf("token-1"), api.removedTokens)
    }

    @Test fun signingOutOfflineStillFinishes() = runTest {
        api.pushError = IOException("offline")
        val removed = repository().unregister()
        assertFalse(removed)
    }

    @Test fun signingOutWithoutAnFcmTokenStillFinishes() = runTest {
        val repo = PushTokenRepository(api = api, isSignedIn = { true }, fetchToken = { error("Play services missing") })
        assertFalse(repo.unregister())
    }

    @Test fun theTestButtonRegistersBeforeSending() = runTest {
        api.testReport = SendReportDto(sent = 1)
        val result = repository().sendTest()
        assertEquals(SendReportDto(sent = 1), result.getOrNull())
        assertEquals(listOf("token-1"), api.registeredTokens)
    }

    @Test fun theTestButtonReportsAFailure() = runTest {
        api.pushError = IOException("offline")
        assertTrue(repository().sendTest().isFailure)
    }
}
