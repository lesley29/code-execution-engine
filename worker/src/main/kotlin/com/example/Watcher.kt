package com.example

import com.example.data.MongoContext
import com.example.model.Task
import com.example.model.TaskStatus
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.Event
import com.github.dockerjava.api.model.EventType
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.litote.kmongo.*
import utils.asFlow
import utils.chunked
import java.time.Duration
import java.util.*

class Watcher(
    private val dockerClient: DockerClient,
    private val mongoContext: MongoContext,
) {
    suspend fun run() = coroutineScope {
        dockerClient.eventsCmd()
            .withEventTypeFilter(EventType.CONTAINER)
            .withEventFilter("start", "stop", "die")
            .asFlow()
            .collect { event ->
                when (event.action) {
                    "start" -> launch { startLogTracking(event) }
                    "stop", "die" -> completeTask(event)
                }
            }
    }

    private suspend fun startLogTracking(event: Event) {
        val taskIdString = event.actor?.attributes?.get("name") ?: return
        val taskId = UUID.fromString(taskIdString)

        dockerClient.logContainerCmd(taskIdString)
            .withStdOut(true)
            .withFollowStream(true)
            .asFlow()
            .chunked(Duration.ofSeconds(1))
            .collect {
                val newLogs = it.joinToString("\n") { v -> String(v.payload).trim() }

                mongoContext.tasks.findOneAndUpdate(
                    Task::id eq taskId,
                    push(Task::stdOut, newLogs)
                )
            }
    }

    private suspend fun completeTask(event: Event) {
        val taskIdString = event.actor?.attributes?.get("name") ?: return
        val exitCode = event.actor?.attributes?.get("exitCode")?.toLong() ?: return
        val taskId = UUID.fromString(taskIdString)

        mongoContext.tasks.findOneAndUpdate(
            and(
                Task::id eq taskId,
                Task::status `in` listOf(TaskStatus.Executing, TaskStatus.Pending, TaskStatus.Created)
            ),
            set(
                Task::status setTo TaskStatus.Finished,
                Task::exitCode setTo exitCode
            )
        )
    }
}