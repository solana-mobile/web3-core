package com.solana.types.u128

import com.solana.types.U128
import com.solana.types.U256
import kotlin.test.Test
import kotlin.test.assertEquals

class U128MultiplyTest {

    @Test
    fun `multiplying U128 small values produces product`() {
        // given
        val value1 = U128.parse("76")
        val value2 = U128.parse("28568")
        val expectedResult = U256.parse("2171168")

        // when
        val product = value1*value2

        // then
        assertEquals(expectedResult, product)
    }

    @Test
    fun `multiplying U128 medium values produces product`() {
        // given
        val value1 = U128.parse("4032822279924")
        val value2 = U128.parse("1361845238")
        val expectedResult = U256.parse("5492079817614802401912")

        // when
        val product = value1*value2

        // then
        assertEquals(expectedResult, product)
    }

    @Test
    fun `multiplying U128 large values produces product`() {
        // given
        val value1 = U128.MAX_VALUE
        val value2 = U128.MAX_VALUE
        val expectedResult =
            U256.parse("115792089237316195423570985008687907852589419931798687112530834793049593217025")

        // when
        val product = value1*value2

        // then
        assertEquals(expectedResult, product)
    }
}