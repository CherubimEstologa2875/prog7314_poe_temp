package com.example

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import org.slf4j.event.Level

// Request logging and cross-origin configuration (IIE, 2026; Rouse, 2019).
// Built on the Ktor server framework (Ktor, 2026).

fun Application.configureMonitoring() {
    install(CallLogging) {
        level = Level.INFO
        format { call ->
            "${call.request.httpMethod.value} ${call.request.path()} -> ${call.response.status()}"
        }
    }

    install(CORS) {
        anyHost()
        allowHeader(io.ktor.http.HttpHeaders.Authorization)
        allowHeader(io.ktor.http.HttpHeaders.ContentType)
        allowMethod(io.ktor.http.HttpMethod.Patch)
        allowMethod(io.ktor.http.HttpMethod.Delete)
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Ktor, 2026. Ktor server documentation. [online] Available at: <https://ktor.io/docs/server-create-and-configure.html> [Accessed 11 September 2026].
Rouse, M., 2019. HTTP (Hypertext Transfer Protocol). [online] Available at: <https://whatis.techtarget.com/definition/HTTP-Hypertext-Transfer-Protocol> [Accessed 31 July 2023].
*/
