package com.example

import org.apache.kafka.common.serialization.Deserializer
import java.util.UUID

class UUIDKafkaDeserializer : Deserializer<UUID> {
    override fun deserialize(topic: String?, data: ByteArray?): UUID {
        return UUID.fromString(String(data!!))
    }
}