package com.example

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.ContentConvertException
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable

// Central error handling for the Runway REST API (IIE, 2026).
// Failures are returned as HTTP status codes with one JSON shape (Rouse, 2019; Rouse, 2020).

// Every failure the API returns uses this one shape, so the Android client can write a single parser instead of one per endpoint.
// Built on the Ktor server framework (Ktor, 2026).
@Serializable
data class ApiError(
    val code: String,
    val message: String,
)

// Thrown when a request is well-formed but the data it carries is not valid.
class ValidationException(message: String) : RuntimeException(message)

// Thrown when a document does not exist OR belongs to another user.
class NotFoundException(message: String) : RuntimeException(message)

// Thrown when a request carries no usable Firebase identity.
class UnauthorizedException(message: String = "Authentication required") : RuntimeException(message)

// Maps thrown exceptions onto HTTP status codes in one place.
fun Application.configureErrorHandling() {
    install(StatusPages) {
        exception<ValidationException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError("validation_failed", cause.message ?: "Invalid request")
            )
        }

        // A body that cannot be parsed at all - a missing required field, a string where a number belongs, truncated JSON.
        exception<ContentConvertException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError("malformed_request", describeParseFailure(cause))
            )
        }

        exception<BadRequestException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError("malformed_request", describeParseFailure(cause))
            )
        }

        exception<SerializationException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError("malformed_request", describeParseFailure(cause))
            )
        }

        exception<UnauthorizedException> { call, cause ->
            call.respond(
                HttpStatusCode.Unauthorized,
                ApiError("unauthorized", cause.message ?: "Authentication required")
            )
        }

        exception<NotFoundException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                ApiError("not_found", cause.message ?: "Not found")
            )
        }

        exception<Throwable> { call, cause ->
            // Log the detail for us, return a generic message to the caller:
            call.application.log.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiError("server_error", "Something went wrong on the server")
            )
        }
    }
}

// Turns a parser exception into something a client developer can act on.
private fun describeParseFailure(cause: Throwable): String {
    val root = generateSequence(cause) { it.cause }.last()

    val cleaned = root.message
        ?.substringBefore('\n')
        ?.substringBefore(" for type with serial name")
        ?.trim()
        .orEmpty()

    return cleaned.ifBlank { "The request body could not be read as JSON" }.take(200)
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Ktor, 2026. Ktor server documentation. [online] Available at: <https://ktor.io/docs/server-create-and-configure.html> [Accessed 11 September 2026].
Rouse, M., 2019. HTTP (Hypertext Transfer Protocol). [online] Available at: <https://whatis.techtarget.com/definition/HTTP-Hypertext-Transfer-Protocol> [Accessed 31 July 2023].
Rouse, M., 2020. RESTful API (REST API). [online] Available at: <https://searchapparchitecture.techtarget.com/definition/RESTful-API> [Accessed 31 July 2023].
*/
