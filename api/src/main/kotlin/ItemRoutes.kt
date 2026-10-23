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

// RESTful clothing item endpoints (IIE, 2026; Rouse, 2020).
// Plural nouns with HTTP verbs follow REST conventions (RESTfulAPI.net, n.d.).
// Built on the Ktor server framework (Ktor, 2026).
fun Route.itemRoutes(service: ItemService = ItemService()) {
    authenticate(FIREBASE_AUTH) {
        route("/api/v1/items") {
            get {
                call.respond(
                    service.list(
                        uid = call.uid(),
                        category = call.request.queryParameters["category"],
                        updatedSince = call.request.queryParameters["updatedSince"],
                        limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: DEFAULT_LIMIT,
                        cursor = call.request.queryParameters["cursor"],
                    )
                )
            }

            post {
                val body = call.receive<CreateItemRequest>()
                call.respond(HttpStatusCode.Created, service.create(call.uid(), body))
            }

            get("/{id}") {
                call.respond(service.get(call.uid(), call.itemId()))
            }

            patch("/{id}") {
                val body = call.receive<UpdateItemRequest>()
                call.respond(service.update(call.uid(), call.itemId(), body))
            }

            delete("/{id}") {
                service.softDelete(call.uid(), call.itemId())
                // 204 No Content: the delete succeeded and there is nothing to return.
                call.respond(HttpStatusCode.NoContent)
            }

            post("/{id}/wears") {
                val body = call.receive<LogWearRequest>()
                call.respond(HttpStatusCode.Created, service.logWear(call.uid(), call.itemId(), body))
            }
        }

        // Wardrobe totals. Counted from the same item data, so it lives with the
        // item service rather than in a collection of its own (RESTfulAPI.net, n.d.).
        get("/api/v1/wardrobe/summary") {
            call.respond(service.summary(call.uid()))
        }
    }
}

private fun RoutingCall.itemId(): String =
    parameters["id"] ?: throw ValidationException("An item id is required in the path")

private const val DEFAULT_LIMIT = 50

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Ktor, 2026. Ktor server documentation. [online] Available at: <https://ktor.io/docs/server-create-and-configure.html> [Accessed 11 September 2026].
RESTfulAPI.net, n.d.. What is REST. [online] Available at: <https://restfulapi.net/> [Accessed 11 September 2026].
Rouse, M., 2020. RESTful API (REST API). [online] Available at: <https://searchapparchitecture.techtarget.com/definition/RESTful-API> [Accessed 11 September 2026].
*/
