package com.solana.types.u128

import com.solana.types.U128
import kotlin.test.Test
import kotlin.test.assertEquals

class U128ToByteTest {

    @Test
    fun `U128 toByte() converts to equivalent Byte`() {
        // given
        val byteValue: Byte = 123
        val u128 = U128.parse("123")

        // when
        val result = u128.toByte()

        // then
        assertEquals(byteValue, result)
    }

    @Test
    fun `U128 toByte() converts max value Byte`() {
        // given
        val byteValue: Byte = Byte.MAX_VALUE // 127
        val u128 = U128.parse("127")

        // when
        val result = u128.toByte()

        // then
        assertEquals(byteValue, result)
    }

    @Test
    fun `U128 toByte() converts to truncated Byte`() {
        // given
        val intValue = 300 // larger than Byte.MAX_VALUE
        val u128 = U128.parse(intValue.toString())
        val expected = intValue.toByte() // truncates to 44

        // when
        val result = u128.toByte()

        // then
        assertEquals(expected, result)
    }
}