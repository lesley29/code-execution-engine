package com.example.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable

@Serializable
data class ProblemDetails(
    val title: String = "",
    val status: Int,
    val detail: String = ""
)

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, _ ->
            call.respond(HttpStatusCode.InternalServerError, ProblemDetails(
                title = "Internal Server Error",
                status = HttpStatusCode.InternalServerError.value
            ))
        }

        exception<RequestValidationException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ProblemDetails(
                title = "One or more validation errors occurred",
                status = HttpStatusCode.BadRequest.value,
                detail = cause.reasons.joinToString()
            ))
        }

        exception<BadRequestException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ProblemDetails(
                title = "One or more validation errors occurred",
                status = HttpStatusCode.BadRequest.value,
                detail = cause.message.orEmpty()
            ))
        }
    }
}