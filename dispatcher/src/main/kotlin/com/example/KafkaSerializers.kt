package com.example

import org.apache.kafka.common.serialization.Serializer
import java.util.UUID

class UUIDKafkaSerializer : Serializer<UUID> {
    override fun serialize(topic: String?, data: UUID?): ByteArray {
        return data.toString().toByteArray()
    }
}