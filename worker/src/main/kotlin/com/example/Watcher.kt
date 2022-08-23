package com.example

import com.example.data.MongoContext
import com.example.model.Task
import com.example.model.TaskStatus
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.EventsCmd
import com.github.dockerjava.api.model.Event
import com.github.dockerjava.api.model.EventType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import org.litote.kmongo.*
import java.util.*

class Watcher(
    private val dockerClient: DockerClient,
    private val mongoContext: MongoContext
) {
    suspend fun run() {
        dockerClient.eventsCmd()
            .withEventTypeFilter(EventType.CONTAINER)
            .withEventFilter("stop", "die")
            .asFlow()
            .collect { event ->
                val taskIdString = event.actor?.attributes?.get("name") ?: return@collect
                val exitCode = event.actor?.attributes?.get("exitCode")?.toLong() ?: return@collect
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