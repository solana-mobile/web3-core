package com.solana.types.u256

import com.solana.types.U256
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class U256ParseTest {

    @Test
    fun `parse Int string successfully parses Int value`() {
        // given
        val intString = "1234"
        val expected = byteArrayOf(
            -46, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        ) // 1234 bytes, little-endian

        // when
        val result = U256.parse(intString)

        // then
        assertContentEquals(expected, result.toByteArray())
    }

    @Test
    fun `parse U256 max value string successfully parses`() {
        // given
        val u256String = U256.MAX_VALUE_DECIMAL
        val expected = U256.MAX_VALUE

        // when
        val result = U256.parse(u256String)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `parse U256 min value string successfully parses`() {
        // given
        val zeroString = "0"
        val expected = U256.MIN_VALUE

        // when
        val result = U256.parse(zeroString)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `parse empty string throws IllegalArgumentException`() {
        // given
        val emptyString = ""

        // then
        assertFailsWith(IllegalArgumentException::class) {
            U256.parse(emptyString)
        }
    }

    @Test
    fun `parse non-numeric string throws IllegalArgumentException`() {
        // given
        val illegalString = "1234abcd"

        // then
        assertFailsWith(IllegalArgumentException::class) {
            U256.parse(illegalString)
        }
    }

    @Test
    fun `parse input string greater than U256 MAX_VALUE throws IllegalArgumentException`() {
        // given
        val u257String = "115792089237316195423570985008687907853269984665640564039457584007913129639936" // U256.MAX_VALUE + 1

        // then
        assertFailsWith(IllegalArgumentException::class) {
            U256.parse(u257String)
        }
    }

    @Test
    fun `parse UInt string successfully parses UInt value`() {
        // given
        val intString = "1234u"
        val expected = byteArrayOf(
            -46, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        ) // 1234 bytes, little-endian

        // when
        val result = U256.parse(intString)

        // then
        assertContentEquals(expected, result.toByteArray())
    }

    @Test
    fun `parse underscore separated string successfully parses value`() {
        // given
        val intString = "1_234"
        val expected = byteArrayOf(
            -46, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        ) // 1234 bytes, little-endian

        // when
        val result = U256.parse(intString)

        // then
        assertContentEquals(expected, result.toByteArray())
    }

    @Test
    fun `parse BigInt literal string successfully parses value`() {
        // given
        val intString = "1234n"
        val expected = byteArrayOf(
            -46, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        ) // 1234 bytes, little-endian

        // when
        val result = U256.parse(intString)

        // then
        assertContentEquals(expected, result.toByteArray())
    }

    @Test
    fun `parse underscore separated BigInt literal string successfully parses value`() {
        // given
        val intString = "1_234n"
        val expected = byteArrayOf(
            -46, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        ) // 1234 bytes, little-endian

        // when
        val result = U256.parse(intString)

        // then
        assertContentEquals(expected, result.toByteArray())
    }
}