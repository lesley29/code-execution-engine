package com.example

import com.example.model.TaskCreatedEvent
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.apache.kafka.common.serialization.Deserializer
import java.util.UUID

class UUIDKafkaDeserializer : Deserializer<UUID> {
    override fun deserialize(topic: String?, data: ByteArray?): UUID {
        return UUID.fromString(String(data!!))
    }
}

class TaskCreatedEventDeserializer : Deserializer<TaskCreatedEvent> {
    override fun deserialize(topic: String?, data: ByteArray?): TaskCreatedEvent {
        val string = String(data!!)
        return Json.decodeFromString(string)
    }
}