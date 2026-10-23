package com.example

import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

// Wardrobe-wide endpoints, as opposed to the single-item ones in ItemRoutes.
fun Route.wardrobeRoutes(service: ItemService = ItemService()) {
    authenticate(FIREBASE_AUTH) {
        route("/api/v1/wardrobe") {
            // Counts and totals for the home and profile screens.
            get("/summary") {
                call.respond(service.summary(call.uid()))
            }
        }
    }
}
