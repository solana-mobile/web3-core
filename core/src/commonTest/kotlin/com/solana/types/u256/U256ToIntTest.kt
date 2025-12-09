package com.solana.types.u256

import com.solana.types.U256
import kotlin.test.Test
import kotlin.test.assertEquals

class U256ToIntTest {

    @Test
    fun `U256 toInt converts to equivalent Int`() {
        // given
        val intValue = 1234
        val u256 = U256.parse("1234")

        // when
        val result = u256.toInt()

        // then
        assertEquals(intValue, result)
    }

    @Test
    fun `U256 toInt converts max value Int`() {
        // given
        val intValue = Int.MAX_VALUE
        val u256 = U256.parse("2147483647")

        // when
        val result = u256.toInt()

        // then
        assertEquals(intValue, result)
    }

    @Test
    fun `U256 toInt converts to truncated Int`() {
        // given
        val longValue = 2147483652
        val u256 = U256.parse("2147483652")
        val expected = longValue.toInt()

        // when
        val result = u256.toInt()

        // then
        assertEquals(expected, result)
    }
}