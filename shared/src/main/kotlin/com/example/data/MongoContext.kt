package com.example.data

import com.example.model.Task
import com.example.utils.UUIDCodec
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import org.bson.UuidRepresentation
import org.bson.codecs.configuration.CodecRegistries
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo


class MongoContext(connectionString: String) {
    private val clientSettings: MongoClientSettings =
        MongoClientSettings.builder()
            .uuidRepresentation(UuidRepresentation.STANDARD)
            .codecRegistry(
                // https://github.com/Litote/kmongo/issues/287
                CodecRegistries.fromRegistries(CodecRegistries.fromCodecs(UUIDCodec()),
                MongoClientSettings.getDefaultCodecRegistry()
            ))
            .applyConnectionString(ConnectionString(connectionString))
            .build()

    private val client = KMongo.createClient(clientSettings).coroutine
    private val database = client.getDatabase("codeExecutionEngine")

    val tasks = database.getCollection<Task>()
}