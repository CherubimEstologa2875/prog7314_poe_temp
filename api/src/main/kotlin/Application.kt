package com.example

import io.ktor.server.application.Application

// Composes the server plugins and routes used by the application module.
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

