package com.solana.types.u256

import com.solana.types.U256
import kotlin.test.Test
import kotlin.test.assertEquals

class U256ToShortTest {

    @Test
    fun `U256 toShort converts to equivalent Short`() {
        // given
        val shortValue: Short = 1234
        val u256 = U256.parse("1234")

        // when
        val result = u256.toShort()

        // then
        assertEquals(shortValue, result)
    }

    @Test
    fun `U256 toShort converts max value Short`() {
        // given
        val shortValue: Short = Short.MAX_VALUE // 32767
        val u256 = U256.parse("32767")

        // when
        val result = u256.toShort()

        // then
        assertEquals(shortValue, result)
    }

    @Test
    fun `U256 toShort converts to truncated Short`() {
        // given
        val intValue = 40000 // larger than Short.MAX_VALUE
        val u256 = U256.parse(intValue.toString())
        val expected = intValue.toShort() // truncates to -25536

        // when
        val result = u256.toShort()

        // then
        assertEquals(expected, result)
    }

}