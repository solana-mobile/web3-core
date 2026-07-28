package com.solana.types.u256

import com.solana.types.U256
import kotlin.test.Test
import kotlin.test.assertEquals

class U256ToFloatTestJvm {

    @Test
    fun `U256 toFloat converts max U256 with expected overflow to Infinity`() {
        // given
        val u256 = U256.MAX_VALUE
        val expected = Float.POSITIVE_INFINITY

        // when
        val result = u256.toFloat()

        // then
        assertEquals(expected, result)
    }
}