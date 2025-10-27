package com.solana.types.u128

import com.solana.types.U128
import kotlin.test.Test
import kotlin.test.assertEquals

class U128ToDoubleTest {

    @Test
    fun `U128 toDouble() converts to equivalent Double for small values`() {
        // given
        val u128 = U128.parse("12345")
        val expected = 12345.0

        // when
        val result = u128.toDouble()

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `U128 toDouble() converts max precise integer within Double mantissa`() {
        // given
        // 2^53 = 9007199254740992 is the largest integer exactly representable in Double
        val value = 9007199254740992UL
        val u128 = U128.parse(value.toString())
        val expected = 9007199254740992.0

        // when
        val result = u128.toDouble()

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `U128 toDouble() converts max U128 value with expected precision loss`() {
        // given
        val u128 = U128.MAX_VALUE
        val expected = 3.402823669209385E38

        // when
        val result = u128.toDouble()

        // then
        assertEquals(expected, result, 1e-9)
    }
}