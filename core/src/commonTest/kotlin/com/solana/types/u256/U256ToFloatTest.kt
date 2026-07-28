package com.solana.types.u256

import com.solana.types.U256
import kotlin.test.Test
import kotlin.test.assertEquals

class U256ToFloatTest {

    @Test
    fun `U256 toFloat converts small values exactly`() {
        // given
        val u256 = U256.parse("12345")
        val expected = 12345f

        // when
        val result = u256.toFloat()

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `U256 toFloat converts max precise integer within Float mantissa`() {
        // given
        // 2^24 = 16777216 is the largest integer exactly representable in Float
        val value = 16777216UL
        val u256 = U256.parse(value.toString())
        val expected = 16777216f

        // when
        val result = u256.toFloat()

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `U256 toFloat converts imprecise but finite values larger than 2^24`() {
        // given
        val bigButFinite = 100_000_000UL // > 2^24 but << Float.MAX_VALUE
        val u256 = U256.parse(bigButFinite.toString())
        val expected = 100_000_000f

        // when
        val result = u256.toFloat()

        // then
        assertEquals(expected, result, 0.0001f)
    }
}