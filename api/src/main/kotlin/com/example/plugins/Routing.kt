package com.example.plugins

import com.example.features.tasks.taskRouting
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.response.*

fun Application.configureRouting() {

    routing {
        taskRouting()

        get("/") {
            call.respondText("Hello World!")
        }
    }
}
