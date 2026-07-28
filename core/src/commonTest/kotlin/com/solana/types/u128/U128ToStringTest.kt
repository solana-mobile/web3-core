package com.solana.types.u128

import com.solana.types.U128
import kotlin.test.Test
import kotlin.test.assertEquals

class U128ToStringTest {

    @Test
    fun `U128 toString returns max value as string`() {
        // given
        val u128 = U128.MAX_VALUE
        val expectedString = "340282366920938463463374607431768211455"

        // when
        val actualString = u128.toString()

        // then
        assertEquals(expectedString, actualString)
    }

    @Test
    fun `U128 toString for smaller number returns non-zero-padded string`() {
        // given
        val u128 = U128.parse("1234")
        val expectedString = "1234"

        // when
        val actualString = u128.toString()

        // then
        assertEquals(expectedString, actualString)
        assertEquals("0", U128.parse("0").toString())
    }

    @Test
    fun `U128 toString for zero value returns 0 string`() {
        // given
        val u128 = U128.parse("0")
        val expectedString = "0"

        // when
        val actualString = u128.toString()

        // then
        assertEquals(expectedString, actualString)
    }
}