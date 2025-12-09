package com.solana.types.u256

import com.solana.types.U256
import kotlin.test.Test
import kotlin.test.assertEquals

class U256ToDoubleTest {

    @Test
    fun `U256 toDouble converts to equivalent Double for small values`() {
        // given
        val u256 = U256.parse("12345")
        val expected = 12345.0

        // when
        val result = u256.toDouble()

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `U256 toDouble converts max precise integer within Double mantissa`() {
        // given
        // 2^53 = 9007199254740992 is the largest integer exactly representable in Double
        val value = 9007199254740992UL
        val u256 = U256.parse(value.toString())
        val expected = 9007199254740992.0

        // when
        val result = u256.toDouble()

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `U256 toDouble converts max U256 value with expected precision loss`() {
        // given
        val u256 = U256.MAX_VALUE
        val expected = 3.402823669209385E38

        // when
        val result = u256.toDouble()

        // then
        assertEquals(expected, result, 1e-9)
    }
}