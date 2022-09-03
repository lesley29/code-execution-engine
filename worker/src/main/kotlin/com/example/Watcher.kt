package com.example

import com.example.data.MongoContext
import com.example.model.Task
import com.example.model.TaskStatus
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.Event
import com.github.dockerjava.api.model.EventType
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.StreamType
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.bson.conversions.Bson
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
        val taskId: UUID
        try {
            taskId = UUID.fromString(taskIdString)
        } catch (_ : IllegalArgumentException) {
            return
        }

        dockerClient.logContainerCmd(taskIdString)
            .withStdOut(true)
            .withStdErr(true)
            .withFollowStream(true)
            .asFlow()
            .chunked(Duration.ofSeconds(1))
            .collect {
                val stdOut = it
                    .filter { frame -> frame.streamType == StreamType.STDOUT }
                    .formatToString()

                val stdError = it
                    .filter { frame -> frame.streamType == StreamType.STDERR }
                    .formatToString()

                val updates = mutableListOf<Bson>()
                if (stdOut.isNotEmpty()) {
                    updates.add(push(Task::stdOut, stdOut))
                }

                if (stdError.isNotEmpty()) {
                    updates.add(push(Task::stdError, stdError))
                }

                mongoContext.tasks.findOneAndUpdate(
                    Task::id eq taskId,
                    combine(updates)
                )
            }
    }

    private suspend fun completeTask(event: Event) {
        val taskIdString = event.actor?.attributes?.get("name") ?: return
        val exitCode = event.actor?.attributes?.get("exitCode")?.toLong() ?: return
        val taskId: UUID
        try {
            taskId = UUID.fromString(taskIdString)
        } catch (_ : IllegalArgumentException) {
            return
        }

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

private fun Iterable<Frame>.formatToString(): String =
    joinToString("\n") { v -> String(v.payload).trim() }