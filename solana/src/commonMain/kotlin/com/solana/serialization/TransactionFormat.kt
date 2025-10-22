package com.solana.serialization

import com.solana.util.asVarint
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.EmptySerializersModule

sealed class TransactionFormat : BinaryFormat {

    companion object Default : TransactionFormat()

    override val serializersModule = EmptySerializersModule()

    override fun <T> decodeFromByteArray(deserializer: DeserializationStrategy<T>,
                                         bytes: ByteArray): T =
        TransactionDecoder(bytes).decodeSerializableValue(deserializer)

    override fun <T> encodeToByteArray(serializer: SerializationStrategy<T>, value: T): ByteArray =
        TransactionEncoder().apply { encodeSerializableValue(serializer, value) }.encodedBytes
}

class TransactionEncoder : AbstractEncoder() {
    private val bytes = mutableListOf<Byte>()

    val encodedBytes get() = bytes.toByteArray()

    override val serializersModule = EmptySerializersModule()

    override fun encodeByte(value: Byte) { bytes.add(value) }

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        collectionSize.asVarint().forEach { encodeByte(it) }
        return super.beginCollection(descriptor, collectionSize)
    }
}

class TransactionDecoder(val bytes: ByteArray) : AbstractDecoder() {
    private var position = 0

    override val serializersModule = EmptySerializersModule()

    override fun decodeByte(): Byte {
        if (position >= bytes.size) {
            throw SerializationException(
                "Attempt to read past end of buffer at $position (size=${bytes.size})"
            )
        }
        return bytes[position++]
    }


    // Not called for sequential decoders
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = 0
    override fun decodeSequentially(): Boolean = true

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int {
        var size = 0
        var shift = 0
        do {
            val b = decodeByte().toInt() and 0xFF
            size = ((b and 0x7f) shl shift) or size
            shift += 7
        } while (b and 0x80 != 0)

        return size
    }
}