package com.solana.types.u128

import com.funkatronics.kborsh.Borsh
import com.solana.types.U128
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class U128SerializationTest {

    @Test
    fun `U128 serializes to JSON as string`() {
        // given
        @Serializable
        data class TestClass(val u128: U128)
        val u128 = U128.parse("12345")
        val expectedJson = """
            {"u128":"12345"}
        """.trimIndent()

        // when
        val actualJson = Json.encodeToString<TestClass>(TestClass(u128))

        // then
        assertEquals(expectedJson, actualJson)
    }

    @Test
    fun `U128 serializes small number to Borsh as 16 length byte string`() {
        // given
        @Serializable
        data class TestClass(val u128: U128)
        val u128 = U128.parse("12345")
        val expectedBinary = byteArrayOf(
            57, 48, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        )

        // when
        val actualBinary = Borsh.encodeToByteArray<TestClass>(TestClass(u128))

        // then
        assertContentEquals(expectedBinary, actualBinary)
    }

    @Test
    fun `U128 serializes medium number to Borsh as 16 length byte string`() {
        // given
        @Serializable
        data class TestClass(val u128: U128)
        val u128 = U128.parse("9223372036854775810")
        val expectedBinary = byteArrayOf(
            2, 0, 0, 0, 0, 0, 0, -128, 0, 0, 0, 0, 0, 0, 0, 0
        )
        // when
        val actualBinary = Borsh.encodeToByteArray<TestClass>(TestClass(u128))

        // then
        assertContentEquals(expectedBinary, actualBinary)
    }

    @Test
    fun `U128 serializes large number to Borsh as 16 length byte string`() {
        // given
        @Serializable
        data class TestClass(val u128: U128)
        val u128 = U128.parse("987654321012345678901234567890")
        val expectedBinary = byteArrayOf(
            -46, 10, -65, -41, -116, -128, 101, 80, -103, -127, 72, 119, 12, 0, 0, 0
        )
        // when
        val actualBinary = Borsh.encodeToByteArray<TestClass>(TestClass(u128))

        // then
        assertContentEquals(expectedBinary, actualBinary)
    }

    @Test
    fun `U128 serializes MAX_VALUE to Borsh as ByteArray(16) { -1 }`() {
        // given
        @Serializable
        data class TestClass(val u128: U128)
        val u128 = U128.MAX_VALUE
        val expectedBinary = byteArrayOf(
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
        )

        // when
        val actualBinary = Borsh.encodeToByteArray<TestClass>(TestClass(u128))

        // then
        assertContentEquals(expectedBinary, actualBinary)
    }

    @Test
    fun `Class containing U128 values serializes to Borsh ByteArray`() {
        // given
        @Serializable
        data class TestClass(
            val string: String,
            val u128a: U128,
            val u128b: U128,
            val boolean: Boolean,
            val u128c: U128
        )
        val testInstance = TestClass(
            "Hello World!",
            U128.MAX_VALUE,
            U128.MIN_VALUE,
            false,
            U128.parse("1234567890")
        )
        val expectedBinary = byteArrayOf(
            12, 0, 0, 0, 72, 101, 108, 108, 111, 32, 87, 111, 114, 108, 100, 33, // "Hello World!"
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, // U128.MAX_VALUE
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // U128.MIN_VALUE
            0, // false boolean
            -46, 2, -106, 73, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 // U128(1234567890)
        )

        // when
        val actualBinary = Borsh.encodeToByteArray<TestClass>(testInstance)
        val actualDecoded = Borsh.decodeFromByteArray(TestClass.serializer(), actualBinary)

        // then
        assertContentEquals(expectedBinary, actualBinary)
        assertEquals(testInstance, actualDecoded)
    }
}