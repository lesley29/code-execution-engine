package com.example

import com.example.data.MongoContext
import com.example.model.Task
import com.example.model.TaskStatus
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.EventsCmd
import com.github.dockerjava.api.command.LogContainerCmd
import com.github.dockerjava.api.model.Event
import com.github.dockerjava.api.model.EventType
import com.github.dockerjava.api.model.Frame
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import org.litote.kmongo.*
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

private fun LogContainerCmd.asFlow() = callbackFlow {
    val callback = object : ResultCallback.Adapter<Frame>() {
        override fun onNext(frame: Frame?) {
            frame ?: return
            trySendBlocking(frame)
        }

        override fun onComplete() {
            super.onComplete()
            channel.close()
        }

        override fun onError(throwable: Throwable?) {
            super.onError(throwable)
            cancel(CancellationException("Docker API error", throwable))
        }
    }

    exec(callback)
    awaitClose { callback.close() }
}

private fun EventsCmd.asFlow() = callbackFlow {
    val callback = object : ResultCallback.Adapter<Event>() {
        override fun onNext(event: Event?) {
            event ?: return
            trySendBlocking(event)
        }

        override fun onComplete() {
            super.onComplete()
            channel.close()
        }

        override fun onError(throwable: Throwable?) {
            super.onError(throwable)
            cancel(CancellationException("Docker API error", throwable))
        }
    }

    exec(callback)
    awaitClose { callback.close() }
}