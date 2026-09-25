package com.solana.serialization

import com.solana.util.Varint
import com.solana.util.asVarint
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.modules.EmptySerializersModule

@OptIn(ExperimentalSerializationApi::class)
sealed class TransactionFormat : BinaryFormat {

    companion object Default : TransactionFormat()

    override val serializersModule = EmptySerializersModule()

    override fun <T> decodeFromByteArray(deserializer: DeserializationStrategy<T>,
                                         bytes: ByteArray): T =
        TransactionDecoder(bytes).decodeSerializableValue(deserializer)

    override fun <T> encodeToByteArray(serializer: SerializationStrategy<T>, value: T): ByteArray =
        TransactionEncoder().apply { encodeSerializableValue(serializer, value) }.encodedBytes
}

@ExperimentalSerializationApi
internal class TransactionEncoder : AbstractEncoder() {
    private val bytes = mutableListOf<Byte>()

    val encodedBytes get() = bytes.toByteArray()

    override val serializersModule = EmptySerializersModule()

    override fun encodeByte(value: Byte) { bytes.add(value) }

    override fun encodeShort(value: Short) {
        for (i in 0 until Short.SIZE_BYTES) bytes.add(((value.toInt() shr (i*8)) and 0xFF).toByte())
    }

    override fun encodeInt(value: Int) {
        for (i in 0 until Int.SIZE_BYTES) bytes.add(((value shr (i*8)) and 0xFF).toByte())
    }

    override fun encodeLong(value: Long) {
        for (i in 0 until Long.SIZE_BYTES) bytes.add(((value shr (i*8)) and 0xFF).toByte())
    }

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        collectionSize.asVarint().forEach { encodeByte(it) }
        return super.beginCollection(descriptor, collectionSize)
    }
}

@ExperimentalSerializationApi
internal class TransactionDecoder(val bytes: ByteArray) : AbstractDecoder() {
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
    override fun decodeShort(): Short {
        var result = 0
        for (i in 0 until Short.SIZE_BYTES) {
            result = result or ((bytes[position++].toInt() and 0xFF) shl (i*8))
        }
        return result.toShort()
    }
    override fun decodeInt(): Int {
        var result = 0
        for (i in 0 until Int.SIZE_BYTES) {
            result = result or ((bytes[position++].toInt() and 0xFF) shl (i*8))
        }
        return result
    }
    override fun decodeLong(): Long {
        var result = 0L
        for (i in 0 until Long.SIZE_BYTES) {
            result = result or ((bytes[position++].toLong() and 0xFF) shl (i*8))
        }
        return result
    }

    // Not called for sequential decoders
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = 0
    override fun decodeSequentially(): Boolean = true

    override fun decodeCollectionSize(descriptor: SerialDescriptor) = decodeCompactU16(this)

    companion object {
        fun decodeCompactU16(decoder: Decoder, peekedByte: Byte? = null): Int {
            var count = 0
            val bytes = ByteArray(3)
            do {
                if (count >= bytes.size)
                    throw SerializationException("Compact-u16 varint exceeds maximum length of ${bytes.size} bytes")
                val b = if (count == 0 && peekedByte != null) peekedByte else decoder.decodeByte()
                bytes[count++] = b
            } while (b.toInt() and 0x80 != 0)

            return Varint.decode(bytes).toInt()
        }
    }
}