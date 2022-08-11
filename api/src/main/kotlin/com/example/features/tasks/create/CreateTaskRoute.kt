package com.example.features.tasks.create

import com.example.data.MongoContext
import com.example.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
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
            TaskStatus.Pending,
            request.nugetPackages
                ?.map { dto -> NugetPackage(dto.name, dto.version) }
        )

        mongoContext.tasks.insertOne(task)

        call.respond(HttpStatusCode.Created, CreateTaskResponse(task.id, task.status))
    }
}