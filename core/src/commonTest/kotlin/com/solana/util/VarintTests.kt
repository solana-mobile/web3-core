package com.solana.util

import kotlin.test.assertEquals
import kotlin.test.assertContentEquals
import kotlin.test.Test

class VarintTests {

    @Test
    fun testVarintEncode() {
        // given
        val value = 16385
        val expected = byteArrayOf(-127, -128, 1)

        // when
        val result = Varint.encode(value)

        // then
        assertContentEquals(expected, result)
    }

    @Test
    fun testVarintEncode0() {
        // given
        val value = 0
        val expected = byteArrayOf(0)

        // when
        val result = Varint.encode(value)

        // then
        assertContentEquals(expected, result)
    }

    @Test
    fun testVarintDecode() {
        // given
        val bytes = byteArrayOf(-127, -128, 1)
        val expected = 16385L

        // when
        val result = Varint.decode(bytes)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun testVarintDecode0() {
        // given
        val bytes = byteArrayOf(0)
        val expected = 0L

        // when
        val result = Varint.decode(bytes)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun testVarintEncodeDecode() {
        // given
        val value = 375847L

        // when
        val encoded = Varint.encode(value)
        val decoded = Varint.decode(encoded)

        // then
        assertEquals(value, decoded)
    }
}