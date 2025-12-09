package com.solana.types.u128

import com.solana.types.U128
import kotlin.test.Test
import kotlin.test.assertEquals

class U128DivideTest {

    @Test
    fun `divide U128 value by long produces integer division result`() {
        // given
        val dividend = U128.parse("2378567835")
        val divisor = 1256L
        val expectedResult = U128.parse("1893764")

        // when
        val product = dividend/divisor

        // then
        assertEquals(expectedResult, product)
    }
}