package com.solana.types.u256

import com.solana.types.U256
import kotlin.test.Test
import kotlin.test.assertEquals

class U256DivideTest {

    @Test
    fun `divide U128 value by long produces integer division result`() {
        // given
        val dividend = U256.parse("2378567835")
        val divisor = 1256L
        val expectedResult = U256.parse("1893764")

        // when
        val product = dividend/divisor

        // then
        assertEquals(expectedResult, product)
    }
}