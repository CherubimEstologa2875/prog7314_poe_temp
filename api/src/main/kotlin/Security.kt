package com.example

import com.google.firebase.auth.FirebaseAuth
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.bearer
import io.ktor.server.auth.principal
import io.ktor.server.routing.RoutingCall

// Bearer authentication for the Runway REST API (IIE, 2026).
// The token travels in the Authorization header over HTTPS (Seobility, n.d.; Google, 2020).

// The verified identity behind one request.
// Built on the Ktor server framework (Ktor, 2026).
data class RunwayPrincipal(
    val uid: String,
    val email: String?,
)

fun Application.configureSecurity() {
    val logger = log

    install(Authentication) {
        bearer(FIREBASE_AUTH) {
            realm = "Runway API"

            authenticate { credential ->
                try {
                    val decoded = FirebaseAuth.getInstance()
                        .verifyIdTokenAsync(credential.token)
                        .await()

                    RunwayPrincipal(uid = decoded.uid, email = decoded.email)
                } catch (e: Exception) {
                    logger.warn("Rejected an ID token: ${e.message}")
                    null
                }
            }
        }
    }
}

// The uid of the signed-in caller.
fun RoutingCall.uid(): String =
    principal<RunwayPrincipal>()?.uid ?: throw UnauthorizedException()

const val FIREBASE_AUTH = "firebase"

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Google, 2020. Secure your site with HTTPS. [online] Available at: <https://support.google.com/webmasters/answer/6073543?hl=en> [Accessed 31 July 2023].
Ktor, 2026. Ktor server documentation. [online] Available at: <https://ktor.io/docs/server-create-and-configure.html> [Accessed 11 September 2026].
Seobility, n.d.. HTTP headers. [online] Available at: <https://www.seobility.net/en/wiki/HTTP_headers> [Accessed 31 July 2023].
*/
