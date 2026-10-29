package com.example

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

// Device registration and test pushes (SCRUM-161, SCRUM-163).
// Built on the Ktor server framework (Ktor, 2026).

fun Route.notificationRoutes(service: NotificationService = NotificationService()) {
    authenticate(FIREBASE_AUTH) {
        route("/api/v1/users/me/device-token") {
            // PUT because registering the same device again changes nothing.
            put {
                val body = call.receive<DeviceTokenRequest>()
                service.registerToken(call.uid(), body)
                call.respond(HttpStatusCode.NoContent)
            }

            // The token goes in the body rather than the URL so it never lands in access logs.
            delete {
                val body = call.receive<DeviceTokenRequest>()
                service.removeToken(call.uid(), body.token)
                call.respond(HttpStatusCode.NoContent)
            }
        }

        // Sends a push to the caller's own devices. Used by the "Send a test" button.
        post("/api/v1/notifications/test") {
            val body = call.receive<TestNotificationRequest>()
            body.validate()
            call.respond(service.sendToUser(call.uid(), body.toPush()))
        }
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Ktor, 2026. Ktor server documentation. [online] Available at: <https://ktor.io/docs/server-create-and-configure.html> [Accessed 11 September 2026].
*/
