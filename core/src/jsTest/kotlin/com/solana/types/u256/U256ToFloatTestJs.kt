package com.solana.types.u256

import com.solana.types.U256
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class U256ToFloatTestJs {

    @Test
    fun `U256 toFloat converts max U256 value with expected precision loss`() {
        // given
        val u256 = U256.MAX_VALUE
        val expected = 3.402823669209385E38

        // when
        val result = u256.toFloat()

        // then
        assertTrue(u256.toFloat().isFinite())
        assertEquals(expected, result.toDouble())
    }
}