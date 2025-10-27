package com.solana.types.u128

import com.solana.types.U128
import kotlin.test.Test
import kotlin.test.assertEquals

class U128ToShortTest {

    @Test
    fun `U128 toShort() converts to equivalent Short`() {
        // given
        val shortValue: Short = 1234
        val u128 = U128.parse("1234")

        // when
        val result = u128.toShort()

        // then
        assertEquals(shortValue, result)
    }

    @Test
    fun `U128 toShort() converts max value Short`() {
        // given
        val shortValue: Short = Short.MAX_VALUE // 32767
        val u128 = U128.parse("32767")

        // when
        val result = u128.toShort()

        // then
        assertEquals(shortValue, result)
    }

    @Test
    fun `U128 toShort() converts to truncated Short`() {
        // given
        val intValue = 40000 // larger than Short.MAX_VALUE
        val u128 = U128.parse(intValue.toString())
        val expected = intValue.toShort() // truncates to -25536

        // when
        val result = u128.toShort()

        // then
        assertEquals(expected, result)
    }

}