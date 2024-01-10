package com.solana.serialization

import com.solana.util.asVarint
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class CompactArraySerializer<T>(val valueSerializer: KSerializer<T>) : KSerializer<Collection<T>> {
    override val descriptor: SerialDescriptor = ListSerializer(valueSerializer).descriptor

    override fun deserialize(decoder: Decoder): Collection<T> {
        var size = 0 // decoder.decodeByte().toInt()
        var shift = 0
        do {
            val b = decoder.decodeByte().toInt() and 0xFF
            size = ((b and 0x7f) shl shift) or size
            shift += 7
        } while (b and 0x80 != 0)

        return List(size) {
            decoder.decodeSerializableValue(valueSerializer)
        }
    }

    override fun serialize(encoder: Encoder, value: Collection<T>) {
        value.size.asVarint().forEach { encoder.encodeByte(it) }
        value.forEach { encoder.encodeSerializableValue(valueSerializer, it) }
    }
}

object CompactByteArraySerializer : KSerializer<ByteArray> {
    override val descriptor: SerialDescriptor = ByteArraySerializer().descriptor

    override fun deserialize(decoder: Decoder): ByteArray {
        var size = 0 // decoder.decodeByte().toInt()
        var shift = 0
        do {
            val b = decoder.decodeByte().toInt() and 0xFF
            size = ((b and 0x7f) shl shift) or size
            shift += 7
        } while (b and 0x80 != 0)

        return ByteArray(size) {
            decoder.decodeByte()
        }
    }

    override fun serialize(encoder: Encoder, value: ByteArray) {
        value.size.asVarint().forEach { encoder.encodeByte(it) }
        value.forEach { encoder.encodeByte(it) }
    }
}

object CompactSignatureArraySerializer : KSerializer<List<ByteArray>> {
    override val descriptor: SerialDescriptor = ByteArraySerializer().descriptor

    override fun deserialize(decoder: Decoder): List<ByteArray> {
        var size = 0 // decoder.decodeByte().toInt()
        var shift = 0
        do {
            val b = decoder.decodeByte().toInt() and 0xFF
            size = ((b and 0x7f) shl shift) or size
            shift += 7
        } while (b and 0x80 != 0)

        return List(size) {
            ByteArray(64) {
                decoder.decodeByte()
            }
        }
    }

    override fun serialize(encoder: Encoder, value: List<ByteArray>) {
        value.size.asVarint().forEach { encoder.encodeByte(it) }
        value.forEach { it.forEach { encoder.encodeByte(it) } }
    }
}