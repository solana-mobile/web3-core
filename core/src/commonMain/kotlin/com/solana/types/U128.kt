package com.solana.types

import com.funkatronics.multibase.MultiBase
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = U128Serializer::class)
class U128 : Number, Comparable<U128> {
    companion object {
        /**
         * A constant holding the minimum value an instance of U128 can have.
         */
        val MIN_VALUE: U128 = U128(ByteArray(SIZE_BYTES) { 0 })

        /**
         * A constant holding the maximum value an instance of U128 can have.
         */
        val MAX_VALUE: U128 = U128(ByteArray(SIZE_BYTES) { -1 })

        /**
         * A constant holding the maximum decimal value an instance of U128 can have,
         * represented as a String.
         */
        const val MAX_VALUE_DECIMAL = "340282366920938463463374607431768211455"

        /**
         * The number of bytes used to represent an instance of U128 in a binary form.
         */
        const val SIZE_BYTES: Int = 16

        /**
         * The number of bits used to represent an instance of U128 in a binary form.
         */
        const val SIZE_BITS: Int = 128

        fun parse(string: String): U128 {
            require(string.isNotEmpty()) { "Invalid U128 String: Empty string" }
            require(string.all { it in '0'..'9' }) { "Invalid U128 String: Non-digit character" }
            require(string.length <= 39) { "Invalid U128 String: Too many digits for U128" }
            if (string.length == 39) require(string <= MAX_VALUE_DECIMAL) {
                "Invalid U128 String: Value exceeds U128 max"
            }

            // decode the string as a MultiBase Encoded Base10 String
            return fromBigEndian(MultiBase.decode("9$string"))
        }

        fun fromBigEndian(bytes: ByteArray): U128 {
            require(bytes.size <= 16)
            val padded = ByteArray(16)
            // copy into the right end (big‑endian)
            bytes.copyInto(padded, 16 - bytes.size)
            return U128(padded.reversedArray()) // store little‑endian internally
        }

        fun fromLittleEndian(bytes: ByteArray): U128 {
            require(bytes.size <= 16)
            val padded = ByteArray(16)
            // copy into the left end (little‑endian)
            bytes.copyInto(padded, 0)
            return U128(padded) // store little‑endian internally
        }
    }

    private val bytes: ByteArray

    private constructor(bytes: ByteArray) {
        require(bytes.size == 16)
        this.bytes = bytes
    }

    fun toByteArray(bigEndian: Boolean = false) =
        if (bigEndian) bytes.reversedArray() else bytes

    override fun toDouble(): Double {
        // Use high precision path: convert to BigDecimal-like via division
        // but since Double has only 53 bits of precision, we can safely
        // approximate by combining the top 64 bits and scaling.
        val hi = getULong(8) // bytes 8..15
        val lo = getULong(0) // bytes 0..7
        return hi.toDouble() * 1.8446744073709552E19 + lo.toDouble()
        // 2^64 = 18446744073709551616
    }

    override fun toFloat(): Float = toDouble().toFloat()

    override fun toLong(): Long {
        return getULong(0).toLong()
    }

    override fun toInt(): Int {
        return (getULong(0) and 0xFFFFFFFFu).toInt()
    }

    override fun toShort(): Short {
        return (getULong(0) and 0xFFFFu).toShort()
    }

    override fun toByte(): Byte {
        return (getULong(0) and 0xFFu).toByte()
    }

    override fun compareTo(other: U128): Int {
        for (i in 15 downTo 0) {
            val a = bytes[i].toInt() and 0xFF
            val b = other.bytes[i].toInt() and 0xFF
            if (a != b) return a.compareTo(b)
        }
        return 0
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as U128

        return bytes.contentEquals(other.bytes)
    }

    override fun toString(): String {
        return if (bytes.any { it != 0.toByte() }) {
            MultiBase.Base10.encode(bytes.reversedArray())
                .drop(1) // remove the MultiBase identifier
                .dropWhile { it == '0' } // drop leading zeros
        } else {
            "0" // bytes are all zero, just return zero string
        }
    }

    private fun getULong(offset: Int): ULong {
        var v = 0UL
        for (i in 0 until 8) {
            v = v or ((bytes[offset + i].toULong() and 0xFFu) shl (8 * i))
        }
        return v
    }
}

private class U128Serializer : KSerializer<U128> {
    override val descriptor =
        PrimitiveSerialDescriptor("U128", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: U128) {
        when (encoder) {
            is com.funkatronics.kborsh.BorshEncoder -> {
                value.toByteArray().forEach {
                    encoder.encodeByte(it)
                }
            }
            else -> encoder.encodeString(value.toString())
        }
    }

    override fun deserialize(decoder: Decoder): U128 {
        return when (decoder) {
            is com.funkatronics.kborsh.BorshDecoder -> {
                val bytes = ByteArray(U128.SIZE_BYTES) {
                    decoder.decodeByte()
                }
                U128.fromLittleEndian(bytes)
            }
            else -> U128.parse(decoder.decodeString())
        }
    }
}