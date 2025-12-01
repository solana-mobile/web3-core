package com.solana.types.u128

import com.solana.types.U128
import kotlin.test.Test
import kotlin.test.assertEquals

class U128ToFloatTestJvm {

    @Test
    fun `U128 toFloat converts max U128 with expected overflow to Infinity`() {
        // given
        val u128 = U128.MAX_VALUE
        val expected = Float.POSITIVE_INFINITY

        // when
        val result = u128.toFloat()

        // then
        assertEquals(expected, result)
    }
}