package com.example

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route

// Outfit planner endpoints. Days are addressed as YYYY-MM-DD.
fun Route.planRoutes(service: PlanService = PlanService()) {
    authenticate(FIREBASE_AUTH) {
        route("/api/v1/plans") {
            get {
                val from = parsePlanDate(call.request.queryParameters["from"])
                val to = parsePlanDate(call.request.queryParameters["to"])
                call.respond(service.list(call.uid(), from, to))
            }

            put("/{date}") {
                val body = call.receive<SetPlanRequest>()
                call.respond(service.set(call.uid(), call.planDate(), body))
            }

            delete("/{date}") {
                service.clear(call.uid(), call.planDate())
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private fun RoutingCall.planDate() = parsePlanDate(parameters["date"])
