package com.solana.types.u128

import com.solana.types.U128
import kotlin.test.Test
import kotlin.test.assertEquals

class U128ToIntTest {

    @Test
    fun `U128 toInt converts to equivalent Int`() {
        // given
        val intValue = 1234
        val u128 = U128.parse("1234")

        // when
        val result = u128.toInt()

        // then
        assertEquals(intValue, result)
    }

    @Test
    fun `U128 toInt converts max value Int`() {
        // given
        val intValue = Int.MAX_VALUE
        val u128 = U128.parse("2147483647")

        // when
        val result = u128.toInt()

        // then
        assertEquals(intValue, result)
    }

    @Test
    fun `U128 toInt converts to truncated Int`() {
        // given
        val longValue = 2147483652
        val u128 = U128.parse("2147483652")
        val expected = longValue.toInt()

        // when
        val result = u128.toInt()

        // then
        assertEquals(expected, result)
    }
}