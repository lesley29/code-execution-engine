package com.example.features.tasks

import com.example.features.tasks.create.createTaskRoute
import com.example.features.tasks.get.getTaskRoute
import io.ktor.server.routing.*

fun Route.taskRouting() {
    route("/tasks") {
        createTaskRoute()
        getTaskRoute()
    }
}