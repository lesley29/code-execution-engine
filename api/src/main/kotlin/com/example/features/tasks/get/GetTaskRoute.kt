package com.example.features.tasks.get

import com.example.data.MongoContext
import com.example.models.Task
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import org.litote.kmongo.eq
import java.util.*

fun Route.getTaskRoute() {
    val mongoContext by inject<MongoContext>()

    get("{id}") {
        val id = call.parameters["id"] ?: return@get call.respondText(
            "Missing id",
            status = HttpStatusCode.BadRequest
        )

        val uuidId = UUID.fromString(id)

        val task = mongoContext.tasks.findOne(Task::id eq uuidId) ?: return@get call.respondText(
            "No task with id $id",
            status = HttpStatusCode.NotFound
        )

        call.respond(task)
    }
}