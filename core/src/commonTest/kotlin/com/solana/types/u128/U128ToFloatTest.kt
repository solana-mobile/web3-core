package com.solana.types.u128

import com.solana.types.U128
import kotlin.test.Test
import kotlin.test.assertEquals

class U128ToFloatTest {

    @Test
    fun `U128 toFloat() converts small values exactly`() {
        // given
        val u128 = U128.parse("12345")
        val expected = 12345f

        // when
        val result = u128.toFloat()

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `U128 toFloat() converts max precise integer within Float mantissa`() {
        // given
        // 2^24 = 16777216 is the largest integer exactly representable in Float
        val value = 16777216UL
        val u128 = U128.parse(value.toString())
        val expected = 16777216f

        // when
        val result = u128.toFloat()

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `U128 toFloat() converts imprecise but finite values larger than 2^24`() {
        // given
        val bigButFinite = 100_000_000UL // > 2^24 but << Float.MAX_VALUE
        val u128 = U128.parse(bigButFinite.toString())
        val expected = 100_000_000f

        // when
        val result = u128.toFloat()

        // then
        assertEquals(expected, result, 0.0001f)
    }

    @Test
    fun `U128 toFloat() converts very large U128 with expected overflow to Infinity`() {
        // given
        val u128 = U128.MAX_VALUE
        val expected = Float.POSITIVE_INFINITY

        // when
        val result = u128.toFloat()

        // then
        assertEquals(expected, result)
    }
}