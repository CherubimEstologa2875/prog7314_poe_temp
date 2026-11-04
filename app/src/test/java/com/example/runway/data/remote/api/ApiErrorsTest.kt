package com.example.runway.data.remote.api

import com.example.runway.R
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

// SCRUM-172: every failure maps to a string resource, so the error is shown in the app's chosen language.

class ApiErrorsTest {

    // Response.error creates a synthetic error response, so no real server is needed (Square, Inc., n.d.).
    private fun http(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody()))

    @Test
    fun `a timeout gets the timeout message`() {
        assertEquals(R.string.rw_error_timeout, SocketTimeoutException().toUserMessage())
    }

    @Test
    fun `a timeout wins even for calls that queue offline`() {
        assertEquals(R.string.rw_error_timeout, SocketTimeoutException().toUserMessage(queuesOffline = true))
    }

    @Test
    fun `no connection says the user is offline`() {
        assertEquals(R.string.rw_error_offline, IOException().toUserMessage())
    }

    @Test
    fun `wardrobe saves promise a later sync when offline`() {
        assertEquals(R.string.rw_error_offline_queued, IOException().toUserMessage(queuesOffline = true))
    }

    // What each status code means (Umbraco, n.d.).
    @Test
    fun `each HTTP status gets its own message`() {
        assertEquals(R.string.rw_error_bad_request, http(400).toUserMessage())
        assertEquals(R.string.rw_error_session_expired, http(401).toUserMessage())
        assertEquals(R.string.rw_error_not_found, http(404).toUserMessage())
        assertEquals(R.string.rw_error_server, http(500).toUserMessage())
        assertEquals(R.string.rw_error_server, http(503).toUserMessage())
    }

    @Test
    fun `an unexpected status falls back to the generic message`() {
        assertEquals(R.string.rw_error_generic, http(418).toUserMessage())
    }

    @Test
    fun `any other exception falls back to the generic message`() {
        assertEquals(R.string.rw_error_generic, IllegalStateException().toUserMessage())
    }
}

/* Reference List
Android Developers, 2026. Build local unit tests. [online] Available at: <https://developer.android.com/training/testing/local-tests> [Accessed 4 November 2026].
JUnit, 2025. JUnit 4: About. [online] Available at: <https://junit.org/junit4/> [Accessed 4 November 2026].
Square, Inc., n.d. Retrofit 2.x API: Response. [online] Available at: <https://square.github.io/retrofit/2.x/retrofit/retrofit2/Response.html> [Accessed 4 November 2026].
Umbraco, n.d. HTTP Status Codes: Full List of All 63 Explained. [online] Available at: <https://umbraco.com/knowledge-base/http-status-codes/> [Accessed 4 November 2026].
*/
