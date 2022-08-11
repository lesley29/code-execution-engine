package com.example.utils

import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import java.util.*

class UUIDCodec : Codec<UUID> {
    override fun encode(writer: BsonWriter, value: UUID, encoderContext: EncoderContext) {
        writer.writeString(value.toString())
    }

    override fun getEncoderClass(): Class<UUID> = UUID::class.java

    override fun decode(reader: BsonReader, decoderContext: DecoderContext): UUID {
        return UUID.fromString(reader.readString())
    }
}