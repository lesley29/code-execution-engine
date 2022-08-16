package com.example

import com.example.model.Task
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration
import java.util.*

class Worker(
    private val consumer: KafkaConsumer<UUID, Task>,
    private val launcher: Launcher
) {
    suspend fun run() = coroutineScope {
        consumer.subscribe(listOf("tasks"))

        while (isActive) {
            consumer
                .poll(Duration.ofMillis(300))
                .forEach {
                    println("new task consumed! ${it.key()}")
                }
        }
    }
}