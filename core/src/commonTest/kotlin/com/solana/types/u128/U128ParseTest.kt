package com.solana.types.u128

import com.solana.types.U128
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class U128ParseTest {

    @Test
    fun `parse Int string successfully parses Int value`() {
        // given
        val intString = "1234"
        val expected = byteArrayOf(
            -46, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        ) // 1234 bytes, little-endian

        // when
        val result = U128.parse(intString)

        // then
        assertContentEquals(expected, result.toByteArray())
    }

    @Test
    fun `parse U128 max value string successfully parses`() {
        // given
        val u128String = U128.MAX_VALUE_DECIMAL
        val expected = U128.MAX_VALUE

        // when
        val result = U128.parse(u128String)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `parse U128 min value string successfully parses`() {
        // given
        val zeroString = "0"
        val expected = U128.MIN_VALUE

        // when
        val result = U128.parse(zeroString)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `parse empty string throws IllegalArgumentException`() {
        // given
        val emptyString = ""

        // then
        assertFailsWith(IllegalArgumentException::class) {
            U128.parse(emptyString)
        }
    }

    @Test
    fun `parse non-numeric string throws IllegalArgumentException`() {
        // given
        val illegalString = "1234abcd"

        // then
        assertFailsWith(IllegalArgumentException::class) {
            U128.parse(illegalString)
        }
    }

    @Test
    fun `parse input string greater than U128 MAX_VALUE throws IllegalArgumentException`() {
        // given
        val u129String = "340282366920938463463374607431768211456" // U128.MAX_VALUE + 1

        // then
        assertFailsWith(IllegalArgumentException::class) {
            U128.parse(u129String)
        }
    }

    @Test
    fun `parse UInt string successfully parses UInt value`() {
        // given
        val intString = "1234u"
        val expected = byteArrayOf(
            -46, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        ) // 1234 bytes, little-endian

        // when
        val result = U128.parse(intString)

        // then
        assertContentEquals(expected, result.toByteArray())
    }

    @Test
    fun `parse underscore separated string successfully parses value`() {
        // given
        val intString = "1_234"
        val expected = byteArrayOf(
            -46, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        ) // 1234 bytes, little-endian

        // when
        val result = U128.parse(intString)

        // then
        assertContentEquals(expected, result.toByteArray())
    }

    @Test
    fun `parse BigInt literal string successfully parses value`() {
        // given
        val intString = "1234n"
        val expected = byteArrayOf(
            -46, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        ) // 1234 bytes, little-endian

        // when
        val result = U128.parse(intString)

        // then
        assertContentEquals(expected, result.toByteArray())
    }

    @Test
    fun `parse underscore separated BigInt literal string successfully parses value`() {
        // given
        val intString = "1_234n"
        val expected = byteArrayOf(
            -46, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        ) // 1234 bytes, little-endian

        // when
        val result = U128.parse(intString)

        // then
        assertContentEquals(expected, result.toByteArray())
    }
}