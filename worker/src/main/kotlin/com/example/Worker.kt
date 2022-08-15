package com.example

import com.example.data.MongoContext
import com.example.model.Task
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Duration
import java.util.*

class Worker : KoinComponent {
    private val consumer by inject<KafkaConsumer<UUID, Task>>()
    private val mongoContext by inject<MongoContext>()

    suspend fun run() = coroutineScope {
        consumer.use {
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
}