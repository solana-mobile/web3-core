package com.solana.types.u256

import com.solana.types.U256
import kotlin.test.Test
import kotlin.test.assertEquals

class U256ToStringTest {

    @Test
    fun `U256 toString returns max value as string`() {
        // given
        val u256 = U256.MAX_VALUE
        val expectedString = "115792089237316195423570985008687907853269984665640564039457584007913129639935"

        // when
        val actualString = u256.toString()

        // then
        assertEquals(expectedString, actualString)
    }

    @Test
    fun `U256 toString for smaller number returns non-zero-padded string`() {
        // given
        val u256 = U256.parse("1234")
        val expectedString = "1234"

        // when
        val actualString = u256.toString()

        // then
        assertEquals(expectedString, actualString)
        assertEquals("0", U256.parse("0").toString())
    }

    @Test
    fun `U256 toString for zero value returns 0 string`() {
        // given
        val u256 = U256.parse("0")
        val expectedString = "0"

        // when
        val actualString = u256.toString()

        // then
        assertEquals(expectedString, actualString)
    }
}