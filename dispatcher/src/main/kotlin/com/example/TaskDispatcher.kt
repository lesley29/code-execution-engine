package com.example

import com.example.data.MongoContext
import com.example.model.Task
import com.example.model.TaskCreatedEvent
import com.example.model.TaskStatus
import kotlinx.coroutines.delay
import mu.KotlinLogging
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.litote.kmongo.and
import org.litote.kmongo.eq
import org.litote.kmongo.setValue
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class TaskDispatcher(
    private val mongoContext: MongoContext,
    private val producer: KafkaProducer<UUID, TaskCreatedEvent>
) {
    private val logger = KotlinLogging.logger {}

    suspend fun run() {
        while (true) {
            val task = mongoContext.tasks
                .find(Task::status eq TaskStatus.Created)
                .ascendingSort(Task::createdAt)
                .first()

            if (task == null) {
                logger.debug { "No tasks to dispatch, sleeping" }
                delay(1000)
            } else {
                logger.info { "Dispatching task ${task.id} to workers" }

                val record = ProducerRecord("tasks", task.id, TaskCreatedEvent(
                    task.id,
                    task.code,
                    task.arguments,
                    task.targetFrameworkMonikier,
                    task.nugetPackages
                ))

                producer.produce(record)

                mongoContext.tasks
                    .findOneAndUpdate(
                        and(Task::id eq task.id, Task::status eq TaskStatus.Created),
                        setValue(Task::status, TaskStatus.Pending)
                    )

                logger.info { "Task ${task.id} has been dispatched" }
            }
        }
    }
}

suspend fun <K, V> KafkaProducer<K, V>.produce(record: ProducerRecord<K, V>) = suspendCoroutine { cont ->
    send(record) { metadata: RecordMetadata, exception: Exception? ->
        if (exception != null) {
            cont.resumeWithException(exception)
        } else {
            cont.resume(metadata)
        }
    }
}