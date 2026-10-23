package com.example

import io.ktor.server.application.Application
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.routing

// Route registration for the Runway REST API (IIE, 2026; RESTfulAPI.net, n.d.).
// Built on the Ktor server framework (Ktor, 2026).
fun Application.configureRouting() {
    routing {
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")

        healthRoutes()
        itemRoutes()
        wardrobeRoutes()
        outfitRoutes()
        ratingRoutes()
        planRoutes()
        userRoutes()
    }
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Ktor, 2026. Ktor server documentation. [online] Available at: <https://ktor.io/docs/server-create-and-configure.html> [Accessed 11 September 2026].
RESTfulAPI.net, n.d.. What is REST. [online] Available at: <https://restfulapi.net/> [Accessed 31 July 2023].
*/
