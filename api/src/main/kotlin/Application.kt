package com.example

import io.ktor.server.application.Application

// Application module wiring for the Runway REST API (IIE, 2026).

// The single application module named in application.yaml.
// Built on the Ktor server framework (Ktor, 2026).
fun Application.module() {
    FirebaseAdmin.init()
    configureSerialization()
    configureMonitoring()
    configureErrorHandling()
    configureSecurity()
    configureRouting()
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Ktor, 2026. Ktor server documentation. [online] Available at: <https://ktor.io/docs/server-create-and-configure.html> [Accessed 11 September 2026].
*/
