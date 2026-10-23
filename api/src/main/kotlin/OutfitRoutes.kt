package com.example

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route

// RESTful outfit endpoints (IIE, 2026; Rouse, 2020).
// Built on the Ktor server framework (Ktor, 2026).
fun Route.outfitRoutes(service: OutfitService = OutfitService()) {
    authenticate(FIREBASE_AUTH) {
        route("/api/v1/outfits") {
            get {
                call.respond(
                    service.list(
                        uid = call.uid(),
                        limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: DEFAULT_LIMIT,
                        cursor = call.request.queryParameters["cursor"],
                    )
                )
            }

            post {
                val body = call.receive<CreateOutfitRequest>()
                call.respond(HttpStatusCode.Created, service.create(call.uid(), body))
            }

            get("/{id}") {
                call.respond(service.get(call.uid(), call.outfitId()))
            }

            patch("/{id}") {
                val body = call.receive<UpdateOutfitRequest>()
                call.respond(service.update(call.uid(), call.outfitId(), body))
            }

            delete("/{id}") {
                service.softDelete(call.uid(), call.outfitId())
                call.respond(HttpStatusCode.NoContent)
            }

            // Wearing an outfit counts a wear against every garment in it.
            post("/{id}/wears") {
                val body = call.receive<LogOutfitWearRequest>()
                call.respond(HttpStatusCode.Created, service.logWear(call.uid(), call.outfitId(), body))
            }
        }
    }
}

private fun RoutingCall.outfitId(): String =
    parameters["id"] ?: throw ValidationException("An outfit id is required in the path")

private const val DEFAULT_LIMIT = 20

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Ktor, 2026. Ktor server documentation. [online] Available at: <https://ktor.io/docs/server-create-and-configure.html> [Accessed 11 September 2026].
Rouse, M., 2020. RESTful API (REST API). [online] Available at: <https://searchapparchitecture.techtarget.com/definition/RESTful-API> [Accessed 31 July 2023].
*/
