package com.example

import com.example.data.MongoContext
import com.example.model.Task
import com.example.model.TaskStatus
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import mu.KotlinLogging
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.litote.kmongo.and
import org.litote.kmongo.eq
import org.litote.kmongo.setValue
import java.lang.Exception
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class TaskDispatcher(
    private val mongoContext: MongoContext,
    private val producer: KafkaProducer<UUID, Task>
) {
    private val logger = KotlinLogging.logger {}

    suspend fun run() = coroutineScope {
        while (isActive) {
            val task = mongoContext.tasks
                .find(Task::status eq TaskStatus.Created)
                .ascendingSort(Task::createdAt)
                .first()

            if (task == null) {
                logger.info { "No work to do, sleeping" }
                delay(1000)
            } else {
                logger.info { "Sending task ${task.id} to Kafka" }

                val record = ProducerRecord("tasks", task.id, task)
                producer.produce(record)

                mongoContext.tasks
                    .findOneAndUpdate(
                        and(Task::id eq task.id, Task::status eq TaskStatus.Created),
                        setValue(Task::status, TaskStatus.Pending)
                    )
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