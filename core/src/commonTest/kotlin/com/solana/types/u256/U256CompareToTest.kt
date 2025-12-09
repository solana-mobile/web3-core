package com.solana.types.u256

import com.solana.types.U256
import kotlin.test.Test
import kotlin.test.assertEquals

class U256CompareToTest {

    @Test
    fun `U256 compareTo returns 0 for equivalent U256 values`() {
        // given
        val firstU256 = U256.parse("1234567890")
        val otherU256 = U256.parse("1234567890")
        val expectedComparison = 0

        // when
        val actualComparison = firstU256.compareTo(otherU256)

        // then
        assertEquals(expectedComparison, actualComparison)
    }

    @Test
    fun `U256 compareTo returns -1 for comparison to larger U256 value`() {
        // given
        val firstU256 = U256.parse("1234567890")
        val otherU256 = U256.parse("1234567891")
        val expectedComparison = -1

        // when
        val actualComparison = firstU256.compareTo(otherU256)

        // then
        assertEquals(expectedComparison, actualComparison)
    }

    @Test
    fun `U256 compareTo returns -1 for equivalent U256 values`() {
        // given
        val firstU256 = U256.parse("1234567891")
        val otherU256 = U256.parse("1234567890")
        val expectedComparison = 1

        // when
        val actualComparison = firstU256.compareTo(otherU256)

        // then
        assertEquals(expectedComparison, actualComparison)
    }
}