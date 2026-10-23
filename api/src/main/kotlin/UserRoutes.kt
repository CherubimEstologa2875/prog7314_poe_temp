package com.example

import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route

// Profile and settings endpoints (IIE, 2026; Rouse, 2020).
// Built on the Ktor server framework (Ktor, 2026).

fun Route.userRoutes(service: UserService = UserService()) {
    authenticate(FIREBASE_AUTH) {
        route("/api/v1/users/me") {
            get {
                val principal = call.principal<RunwayPrincipal>() ?: throw UnauthorizedException()
                call.respond(service.getOrCreate(principal))
            }

            get("/settings") {
                call.respond(service.getSettings(call.uid()))
            }

            put("/settings") {
                val body = call.receive<SettingsDto>()
                call.respond(service.saveSettings(call.uid(), body))
            }
        }
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Ktor, 2026. Ktor server documentation. [online] Available at: <https://ktor.io/docs/server-create-and-configure.html> [Accessed 11 September 2026].
Rouse, M., 2020. RESTful API (REST API). [online] Available at: <https://searchapparchitecture.techtarget.com/definition/RESTful-API> [Accessed 31 July 2023].
*/
