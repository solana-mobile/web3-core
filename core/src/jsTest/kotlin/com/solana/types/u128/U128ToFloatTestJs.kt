package com.solana.types.u128

import com.solana.types.U128
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class U128ToFloatTestJs {

    @Test
    fun `U128 toFloat converts max U128 value with expected precision loss`() {
        // given
        val u128 = U128.MAX_VALUE
        val expected = 3.402823669209385E38

        // when
        val result = u128.toFloat()

        // then
        assertTrue(u128.toFloat().isFinite())
        assertEquals(expected, result.toDouble())
    }
}