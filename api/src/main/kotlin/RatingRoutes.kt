package com.example

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

// Confidence rating endpoints, nested under the outfit they belong to.
fun Route.ratingRoutes(service: RatingService = RatingService()) {
    authenticate(FIREBASE_AUTH) {
        route("/api/v1/outfits/{id}/ratings") {
            get {
                call.respond(service.listForOutfit(call.uid(), call.outfitId()))
            }

            post {
                val body = call.receive<CreateRatingRequest>()
                call.respond(HttpStatusCode.Created, service.create(call.uid(), call.outfitId(), body))
            }
        }
    }
}

private fun RoutingCall.outfitId(): String =
    parameters["id"] ?: throw ValidationException("An outfit id is required in the path")
