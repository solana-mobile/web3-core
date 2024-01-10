package com.solana.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

open class ByteStringSerializer(val length: Int) : KSerializer<ByteArray> {
    override val descriptor: SerialDescriptor = ByteArraySerializer().descriptor

    override fun deserialize(decoder: Decoder) =
        ByteArray(length) {
            decoder.decodeByte()
        }

    override fun serialize(encoder: Encoder, value: ByteArray) {
        if (value.isEmpty()) ByteArray(length).forEach { encoder.encodeByte(it) }
        check(value.size == length) { "Cannot serialize, provided byte array has incorrect size { ${value.size} }" }
        value.forEach { encoder.encodeByte(it) }
    }
}