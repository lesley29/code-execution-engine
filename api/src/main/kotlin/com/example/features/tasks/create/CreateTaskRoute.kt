package com.example.features.tasks.create

import com.example.data.MongoContext
import com.example.models.*
import com.mongodb.client.model.InsertOneOptions
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import java.time.Instant
import java.util.*


fun Route.createTaskRoute() {
    val mongoContext by inject<MongoContext>()

    post {
        val request = call.receive<CreateTaskRequest>()

        val task = Task(
            UUID.randomUUID(),
            request.code,
            request.arguments,
            request.targetFrameworkMonikier,
            TaskStatus.Created,
            request.nugetPackages
                ?.map { dto -> NugetPackage(dto.name, dto.version) },
            Instant.now()
        )

        mongoContext.tasks.insertOne(task, InsertOneOptions())

        call.respond(HttpStatusCode.Created, CreateTaskResponse(task.id, task.status))
    }
}