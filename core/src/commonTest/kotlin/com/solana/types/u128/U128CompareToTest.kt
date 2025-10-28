package com.solana.types.u128

import com.solana.types.U128
import kotlin.test.Test
import kotlin.test.assertEquals

class U128CompareToTest {

    @Test
    fun `U128 compareTo returns 0 for equivalent U128 values`() {
        // given
        val firstU128 = U128.parse("1234567890")
        val otherU128 = U128.parse("1234567890")
        val expectedComparison = 0

        // when
        val actualComparison = firstU128.compareTo(otherU128)

        // then
        assertEquals(expectedComparison, actualComparison)
    }

    @Test
    fun `U128 compareTo returns -1 for comparison to larger U128 value`() {
        // given
        val firstU128 = U128.parse("1234567890")
        val otherU128 = U128.parse("1234567891")
        val expectedComparison = -1

        // when
        val actualComparison = firstU128.compareTo(otherU128)

        // then
        assertEquals(expectedComparison, actualComparison)
    }

    @Test
    fun `U128 compareTo returns -1 for equivalent U128 values`() {
        // given
        val firstU128 = U128.parse("1234567891")
        val otherU128 = U128.parse("1234567890")
        val expectedComparison = 1

        // when
        val actualComparison = firstU128.compareTo(otherU128)

        // then
        assertEquals(expectedComparison, actualComparison)
    }
}