package com.example

import com.example.model.TaskCreatedEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.kafka.common.serialization.Serializer
import java.util.UUID

class UUIDKafkaSerializer : Serializer<UUID> {
    override fun serialize(topic: String?, data: UUID?): ByteArray {
        return data.toString().toByteArray()
    }
}

class TaskCreatedEventSerializer : Serializer<TaskCreatedEvent> {
    override fun serialize(topic: String?, data: TaskCreatedEvent?): ByteArray {
        return Json.encodeToString(data).toByteArray()
    }
}