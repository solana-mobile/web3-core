package com.solana.util

import kotlin.experimental.and
import kotlin.math.ceil

object Varint {

    fun encode(value: Int): ByteArray {
        if (value == 0) return byteArrayOf(0)
        var num = value
        val encodedSize = ceil((Int.SIZE_BITS - value.countLeadingZeroBits()) / 7f).toInt()
        return ByteArray(encodedSize) {
            (num and 0x7F or if (num < 128) 0 else 128).toByte().also {
                num /= 128
            }
        }
    }

    fun encode(value: Long): ByteArray {
        if (value == 0L) return byteArrayOf(0)
        var num = value
        val encodedSize = ceil((Long.SIZE_BITS - value.countLeadingZeroBits()) / 7f).toInt()
        return ByteArray(encodedSize) {
            (num and 0x7F or if (num < 128) 0 else 128).toByte().also {
                num /= 128
            }
        }
    }

    fun decode(bytes: ByteArray): Long =
        bytes.takeWhile { it and 0x80.toByte() < 0 }.run {
            this + bytes[this.size]
        }.foldIndexed(0L) { index, value, byte ->
            ((byte and 0x7f).toLong() shl (7*index)) or value
        }
}

fun Int.asVarint() = Varint.encode(this)
fun Long.asVarint() = Varint.encode(this)