package com.example

import com.example.model.TaskCreatedEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration
import java.util.*

class Consumer(
    private val consumer: KafkaConsumer<UUID, TaskCreatedEvent>,
    private val launcher: Launcher
) {
    suspend fun run() = coroutineScope {
        consumer.subscribe(listOf("tasks"))
        consumer
            .asFlow()
            .collect {
                launcher.startTask(it.value())
            }
    }
}

fun <K, V> KafkaConsumer<K, V>.asFlow() = flow {
    while (true) {
        poll(Duration.ofMillis(300)).forEach {
            emit(it)
        }
        delay(1000)
    }
}.flowOn(Dispatchers.IO)