package com.solana.types.u256

import com.solana.types.U256
import kotlin.test.Test
import kotlin.test.assertEquals

class U256ToLongTest {

    @Test
    fun `U256 toLong converts to equivalent Long`() {
        // given
        val longValue = 1234L
        val u256 = U256.parse("1234")

        // when
        val result = u256.toLong()

        // then
        assertEquals(longValue, result)
    }

    @Test
    fun `U256 toLong converts max value long`() {
        // given
        val longValue = Long.MAX_VALUE
        val u256 = U256.parse(longValue.toString())

        // when
        val result = u256.toLong()

        // then
        assertEquals(longValue, result)
    }

    @Test
    fun `U256 toLong converts truncated Long`() {
        // given
        val u256 = U256.parse("18446744073709551621") // 2^64 + 5
        val expected = 5L

        // when
        val result = u256.toLong()

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `U256 toLong converts 2^64 to zero`() {
        // given
        val u256 = U256.parse("18446744073709551616") // 2^64
        val expected = 0L

        // when
        val result = u256.toLong()

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `U256 toLong converts 2^127 to zero`() {
        // given
        val u256 = U256.parse("170141183460469231731687303715884105728") // 2^127
        val expected = 0L

        // when
        val result = u256.toLong()

        // then
        assertEquals(expected, result)
    }
}