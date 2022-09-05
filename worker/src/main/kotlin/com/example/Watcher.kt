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
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import org.bson.BsonDocument
import org.bson.conversions.Bson
import org.litote.kmongo.combine
import org.litote.kmongo.eq
import org.litote.kmongo.push
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
            .onCompletion {
                // no way to call https://www.mongodb.com/docs/manual/reference/method/db.collection.updateOne/
                // with aggregation pipeline as second parameter given current KMongo api
                mongoContext.tasks.aggregate<Task>(
                    listOf(
                        BsonDocument.parse(
                            "{ " +
                                "\$match: {" +
                                    "\$and: [" +
                                        "{ _id: '$taskId' }, " +
                                        "{ status: { \$in: ['${TaskStatus.Executing}', '${TaskStatus.Pending}', '${TaskStatus.Created}'] } }" +
                                    "] " +
                                "}" +
                            "}"),
                        BsonDocument.parse(
                            "{" +
                                "    \$set: {" +
                                "      status: {" +
                                "        \$cond: [ { \$eq: [ '\$taskExecutionCompleted', true ] }, '${TaskStatus.Finished}', '\$status']" +
                                "      }," +
                                "      logStreamingCompleted: true" +
                                "    }" +
                                "  }"
                        ),
                        BsonDocument.parse("{ \$merge: {into: 'task'}}")
                    )
                ).toCollection()
            }
            .collect {
                val stdOut = it
                    .filter { frame -> frame.streamType == StreamType.STDOUT }
                    .formatToString()

                val stdError = it
                    .filter { frame -> frame.streamType == StreamType.STDERR }
                    .formatToString()

                val updates = mutableListOf<Bson>()
                if (stdOut.isNotEmpty()) {
                    updates.add(push(Task::stdout, stdOut))
                }

                if (stdError.isNotEmpty()) {
                    updates.add(push(Task::stderr, stdError))
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

        // no way to call https://www.mongodb.com/docs/manual/reference/method/db.collection.updateOne/
        // with aggregation pipeline as second parameter given current KMongo api
        mongoContext.tasks.aggregate<Task>(
            listOf(
                BsonDocument.parse(
                    "{ " +
                            "\$match: {" +
                                "\$and: [" +
                                    "{ _id: '$taskId' }, " +
                                    "{ status: { \$in: ['${TaskStatus.Executing}', '${TaskStatus.Pending}', '${TaskStatus.Created}'] } }" +
                                "] " +
                            "}" +
                    "}"),
                BsonDocument.parse(
                    "{" +
                            "    \$set: {" +
                            "      status: {" +
                            "        \$cond: [ { \$eq: [ '\$logStreamingCompleted', true ] }, '${TaskStatus.Finished}', '\$status']" +
                            "      }," +
                            "      taskExecutionCompleted: true," +
                            "      exitCode: $exitCode" +
                            "    }" +
                            "  }"
                ),
                BsonDocument.parse("{ \$merge: {into: 'task'}}")
            )
        ).toCollection()
    }
}

private fun Iterable<Frame>.formatToString(): String =
    joinToString("\n") { v -> String(v.payload).trim() }