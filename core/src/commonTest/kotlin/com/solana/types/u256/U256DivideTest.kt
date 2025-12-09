package com.solana.types.u256

import com.solana.types.U256
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class U256DivideTest {

    @Test
    fun `divide U256 value by long produces integer division result`() {
        // given
        val dividend = U256.parse("2378567835")
        val divisor = 1256L
        val expectedResult = U256.parse("1893764")

        // when
        val result = dividend/divisor

        // then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `divide U256 0 value by long produces 0`() {
        // given
        val dividend = U256.parse("0")
        val divisor = 1256L
        val expectedResult = U256.parse("0")

        // when
        val result = dividend/divisor

        // then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `divide U256 value by 0 produces Error`() {
        // given
        val dividend = U256.parse("324862882325")
        val divisor = 0L

        // when
        fun result() = dividend/divisor

        // then
        assertFailsWith(ArithmeticException::class) {
            result()
        }
    }
}