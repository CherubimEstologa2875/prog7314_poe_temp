package com.example

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

// Unauthenticated liveness endpoint for deployment health checks.

fun Route.healthRoutes() {
    get("/health") {
        call.respond(HealthResponse(status = "ok", version = API_VERSION))
    }
}

// Displayed by /health so a deployed client can identify the API version.
const val API_VERSION = "1.0.1"

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Ktor, 2026. Ktor server documentation. [online] Available at: <https://ktor.io/docs/server-create-and-configure.html> [Accessed 11 September 2026].
Rouse, M., 2019. HTTP (Hypertext Transfer Protocol). [online] Available at: <https://whatis.techtarget.com/definition/HTTP-Hypertext-Transfer-Protocol> [Accessed 31 July 2023].
*/
