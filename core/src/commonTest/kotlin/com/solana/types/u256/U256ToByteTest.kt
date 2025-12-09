package com.solana.types.u256

import com.solana.types.U256
import kotlin.test.Test
import kotlin.test.assertEquals

class U256ToByteTest {

    @Test
    fun `U256 toByte converts to equivalent Byte`() {
        // given
        val byteValue: Byte = 123
        val u256 = U256.parse("123")

        // when
        val result = u256.toByte()

        // then
        assertEquals(byteValue, result)
    }

    @Test
    fun `U256 toByte converts max value Byte`() {
        // given
        val byteValue: Byte = Byte.MAX_VALUE // 127
        val u256 = U256.parse("127")

        // when
        val result = u256.toByte()

        // then
        assertEquals(byteValue, result)
    }

    @Test
    fun `U256 toByte converts to truncated Byte`() {
        // given
        val intValue = 300 // larger than Byte.MAX_VALUE
        val u256 = U256.parse(intValue.toString())
        val expected = intValue.toByte() // truncates to 44

        // when
        val result = u256.toByte()

        // then
        assertEquals(expected, result)
    }
}