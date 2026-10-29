package com.example

import com.google.firebase.messaging.MessagingErrorCode
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Tests for push notifications (SCRUM-163). FCM and Firestore are replaced with fakes,
// so these check our own handling of each delivery outcome.

class NotificationTest {

    // Same rules as the Firestore store: token -> owner, one owner per token.
    private class MapTokenStore : DeviceTokenStore {
        val owners = linkedMapOf<String, String>()
        override suspend fun tokensFor(uid: String) = owners.filterValues { it == uid }.keys.toList()
        override suspend fun save(uid: String, token: String, platform: String) {
            owners[token] = uid
        }
        override suspend fun delete(uid: String, token: String) {
            if (owners[token] == uid) owners.remove(token)
        }
    }

    // Answers each token with whatever outcome the test set up for it.
    private class ScriptedSender(private val outcomes: Map<String, SendOutcome>) : PushSender {
        val sentTo = mutableListOf<String>()
        override suspend fun send(token: String, notification: PushNotification): SendOutcome {
            sentTo += token
            return outcomes.getValue(token)
        }
    }

    private val push = PushNotification(type = "general", title = "Hi", body = "Test")

    @Test
    fun `a push goes to every device the user registered`() = runBlocking {
        val store = MapTokenStore()
        val sender = ScriptedSender(mapOf("phone" to SendOutcome.DELIVERED, "tablet" to SendOutcome.DELIVERED))
        val service = NotificationService(store, sender)
        service.registerToken("u1", DeviceTokenRequest("phone"))
        service.registerToken("u1", DeviceTokenRequest("tablet"))

        val report = service.sendToUser("u1", push)

        assertEquals(SendReportResponse(sent = 2, removed = 0, failed = 0), report)
        assertEquals(setOf("phone", "tablet"), sender.sentTo.toSet())
    }

    @Test
    fun `a token FCM no longer knows is deleted`() = runBlocking {
        val store = MapTokenStore()
        val sender = ScriptedSender(mapOf("old" to SendOutcome.DEAD_TOKEN, "new" to SendOutcome.DELIVERED))
        val service = NotificationService(store, sender)
        service.registerToken("u1", DeviceTokenRequest("old"))
        service.registerToken("u1", DeviceTokenRequest("new"))

        val report = service.sendToUser("u1", push)

        assertEquals(SendReportResponse(sent = 1, removed = 1, failed = 0), report)
        assertEquals(listOf("new"), store.tokensFor("u1"))
    }

    @Test
    fun `a temporary failure keeps the token for next time`() = runBlocking {
        val store = MapTokenStore()
        val service = NotificationService(store, ScriptedSender(mapOf("phone" to SendOutcome.FAILED)))
        service.registerToken("u1", DeviceTokenRequest("phone"))

        val report = service.sendToUser("u1", push)

        assertEquals(SendReportResponse(sent = 0, removed = 0, failed = 1), report)
        assertEquals(listOf("phone"), store.tokensFor("u1"))
    }

    @Test
    fun `a user with no devices gets an empty report, not an error`() = runBlocking {
        val service = NotificationService(MapTokenStore(), ScriptedSender(emptyMap()))
        assertEquals(SendReportResponse(0, 0, 0), service.sendToUser("nobody", push))
    }

    @Test
    fun `one user's push never reaches another user's device`() = runBlocking {
        val store = MapTokenStore()
        val sender = ScriptedSender(mapOf("mine" to SendOutcome.DELIVERED, "theirs" to SendOutcome.DELIVERED))
        val service = NotificationService(store, sender)
        service.registerToken("me", DeviceTokenRequest("mine"))
        service.registerToken("them", DeviceTokenRequest("theirs"))

        service.sendToUser("me", push)

        assertEquals(listOf("mine"), sender.sentTo)
    }

    @Test
    fun `signing out removes the token`() = runBlocking {
        val store = MapTokenStore()
        val service = NotificationService(store, ScriptedSender(emptyMap()))
        service.registerToken("u1", DeviceTokenRequest("phone"))

        service.removeToken("u1", "phone")

        assertTrue(store.tokensFor("u1").isEmpty())
    }

    @Test
    fun `a phone moves to whoever signed in on it last`() = runBlocking {
        val store = MapTokenStore()
        val sender = ScriptedSender(mapOf("shared-phone" to SendOutcome.DELIVERED))
        val service = NotificationService(store, sender)
        service.registerToken("first", DeviceTokenRequest("shared-phone"))
        service.registerToken("second", DeviceTokenRequest("shared-phone"))

        val report = service.sendToUser("first", push)

        assertEquals(SendReportResponse(0, 0, 0), report)
        assertEquals(listOf("shared-phone"), store.tokensFor("second"))
    }

    @Test
    fun `a user cannot remove another user's token`() = runBlocking {
        val store = MapTokenStore()
        val service = NotificationService(store, ScriptedSender(emptyMap()))
        service.registerToken("owner", DeviceTokenRequest("phone"))

        service.removeToken("someone-else", "phone")

        assertEquals(listOf("phone"), store.tokensFor("owner"))
    }

    @Test
    fun `FCM errors that can never succeed drop the token`() {
        assertEquals(SendOutcome.DEAD_TOKEN, FcmPushSender.outcomeFor(MessagingErrorCode.UNREGISTERED))
        assertEquals(SendOutcome.DEAD_TOKEN, FcmPushSender.outcomeFor(MessagingErrorCode.INVALID_ARGUMENT))
        assertEquals(SendOutcome.DEAD_TOKEN, FcmPushSender.outcomeFor(MessagingErrorCode.SENDER_ID_MISMATCH))
    }

    @Test
    fun `FCM errors that may clear up keep the token`() {
        assertEquals(SendOutcome.FAILED, FcmPushSender.outcomeFor(MessagingErrorCode.UNAVAILABLE))
        assertEquals(SendOutcome.FAILED, FcmPushSender.outcomeFor(MessagingErrorCode.INTERNAL))
        assertEquals(SendOutcome.FAILED, FcmPushSender.outcomeFor(MessagingErrorCode.QUOTA_EXCEEDED))
        assertEquals(SendOutcome.FAILED, FcmPushSender.outcomeFor(null))
    }

    @Test
    fun `a blank token is rejected`() {
        assertFailsWith<ValidationException> { DeviceTokenRequest("  ").validate() }
    }

    @Test
    fun `a token far longer than FCM issues is rejected`() {
        assertFailsWith<ValidationException> { DeviceTokenRequest("x".repeat(5000)).validate() }
    }

    @Test
    fun `only android devices can register`() {
        assertFailsWith<ValidationException> { DeviceTokenRequest("abc", platform = "ios").validate() }
    }

    @Test
    fun `a test push must use a known type`() {
        assertFailsWith<ValidationException> { TestNotificationRequest(type = "spam").validate() }
        TestNotificationRequest(type = "wash").validate()
    }

    @Test
    fun `the wash reminder fires on the wear that reaches the limit`() {
        assertTrue(justReachedWashLimit(wearCountBefore = 2, wearLimit = 3))
    }

    @Test
    fun `the wash reminder does not repeat on later wears`() {
        assertFalse(justReachedWashLimit(wearCountBefore = 3, wearLimit = 3))
        assertFalse(justReachedWashLimit(wearCountBefore = 7, wearLimit = 3))
    }

    @Test
    fun `no wash reminder before the limit`() {
        assertFalse(justReachedWashLimit(wearCountBefore = 0, wearLimit = 3))
    }

    @Test
    fun `notification endpoints need a token`() = testApplication {
        application {
            configureSerialization()
            configureErrorHandling()
            configureSecurity()
            configureRouting()
        }

        val register = client.put("/api/v1/users/me/device-token") {
            contentType(ContentType.Application.Json)
            setBody("""{"token":"abc"}""")
        }
        val remove = client.delete("/api/v1/users/me/device-token") {
            contentType(ContentType.Application.Json)
            setBody("""{"token":"abc"}""")
        }
        val test = client.post("/api/v1/notifications/test") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }

        assertEquals(HttpStatusCode.Unauthorized, register.status)
        assertEquals(HttpStatusCode.Unauthorized, remove.status)
        assertEquals(HttpStatusCode.Unauthorized, test.status)
    }

    @Test
    fun `the notification endpoints are in the API docs`() = testApplication {
        application {
            configureSerialization()
            configureErrorHandling()
            configureSecurity()
            configureRouting()
        }

        val definition = client.get("/swagger/documentation.yaml").bodyAsText()
        assertTrue(definition.contains("/api/v1/users/me/device-token:"))
        assertTrue(definition.contains("/api/v1/notifications/test:"))
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Ktor, 2026. Testing in Ktor Server. [online] Available at: <https://ktor.io/docs/server-testing.html> [Accessed 29 October 2026].
*/
