package com.example

import com.example.model.TaskCreatedEvent
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetCommitCallback
import java.time.Duration
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class Consumer(
    private val consumer: KafkaConsumer<UUID, TaskCreatedEvent>,
    private val launcher: Launcher
) {
    suspend fun run() = coroutineScope {
        consumer.subscribe(listOf("tasks"))
        while (true) {
            val records = consumer.poll(Duration.ofMillis(100))

            if (records.isEmpty) {
                delay(500)
            } else {
                records.forEach {
                    launcher.startTask(it.value())
                }
                consumer.commitSync()
            }
        }
    }
}

// TODO: callback not fired
suspend fun <K, V> KafkaConsumer<K, V>.commit() = suspendCoroutine { cont ->
    val callback = OffsetCommitCallback { _, exception ->
        if (exception != null) {
            cont.resumeWithException(exception)
        } else {
            cont.resume(Unit)
        }
    }

    commitAsync(callback)
}
