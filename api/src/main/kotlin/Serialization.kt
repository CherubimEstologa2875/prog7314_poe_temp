package com.example

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json

// JSON content negotiation for the Runway REST API (IIE, 2026; Freeman, 2019).
// DTO classes are marked with serialisation annotations (Kotlin Foundation, 2023).

// Teaches the server to speak JSON.
// Built on the Ktor server framework (Ktor, 2026).
// JSON encoding is provided by kotlinx.serialization (JetBrains, 2026).
fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(
            Json {
                // Readable responses while marking; harmless in production at this scale.
                prettyPrint = true

                ignoreUnknownKeys = true

                encodeDefaults = true
            }
        )
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Freeman, J., 2019. What is JSON? A better format for data exchange. [online] Available at: <https://www.infoworld.com/article/3222851/what-is-json-a-better-format-for-data-exchange.html> [Accessed 31 July 2023].
JetBrains, 2026. kotlinx.serialization guide. [online] Available at: <https://github.com/Kotlin/kotlinx.serialization> [Accessed 11 September 2026].
Kotlin Foundation, 2023. Annotations. [online] Available at: <https://kotlinlang.org/docs/annotations.html> [Accessed 31 July 2023].
Ktor, 2026. Ktor server documentation. [online] Available at: <https://ktor.io/docs/server-create-and-configure.html> [Accessed 11 September 2026].
*/
