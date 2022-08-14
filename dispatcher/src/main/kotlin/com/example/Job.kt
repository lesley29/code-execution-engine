package com.example

import com.example.data.MongoContext
import com.example.model.Task
import com.example.model.TaskStatus
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.litote.kmongo.and
import org.litote.kmongo.eq
import org.litote.kmongo.setValue


class Job : KoinComponent {
    private val mongoContext by inject<MongoContext>()

    suspend fun execute() = coroutineScope {
        while (isActive) {
            val task = mongoContext.tasks
                .find(Task::status eq TaskStatus.Created)
                .ascendingSort(Task::createdAt)
                .first()

            if (task == null) {
                println("No work to do, sleeping")
                delay(1000)
            } else {
                println("Sending task ${task.id} to Kafka")

                mongoContext.tasks
                    .findOneAndUpdate(
                        and(Task::id eq task.id, Task::status eq TaskStatus.Created),
                        setValue(Task::status, TaskStatus.Pending)
                    )
            }
        }
    }
}