package com.solana.serialization

import com.funkatronics.hash.Sha256
import com.funkatronics.kborsh.Borsh
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToByteArray
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class AnchorDiscriminatorSerializerTests {

    @Test
    fun `discriminator is first 8 bytes of identifier hash`() {
        // given
        val namespace = "test"
        val ixName = "testInstruction"
        val data = "data"
        val expectedDiscriminator = Sha256.hash(
            "$namespace:$ixName".encodeToByteArray()
        ).sliceArray(0..7)

        // when
        val serialized = Borsh.encodeToByteArray(AnchorInstructionSerializer(namespace, ixName), data)

        // then
        assertContentEquals(expectedDiscriminator, serialized.sliceArray(0..7))
    }

    @Test
    fun `data is serialized after 8 byte identifier hash`() {
        // given
        val ixName = "testInstruction"
        val data = "data"
        val expectedEncodedData = Borsh.encodeToByteArray(data)

        // when
        val serialized = Borsh.encodeToByteArray(AnchorInstructionSerializer(ixName), data)

        // then
        assertContentEquals(expectedEncodedData, serialized.sliceArray(8 until serialized.size))
    }

    @Test
    fun `serialize and deserialize data struct`() {
        // given
        @Serializable data class TestData(val name: String, val number: Int, val boolean: Boolean)
        val ixName = "testInstruction"
        val data = TestData("testName", 12345678, true)
        val expectedEncodedData = Borsh.encodeToByteArray(data)

        // when
        val serialized = Borsh.encodeToByteArray(AnchorInstructionSerializer(ixName), data)
        val deserialized: TestData = Borsh.decodeFromByteArray(AnchorInstructionSerializer(ixName), serialized)

        // then
        assertContentEquals(expectedEncodedData, serialized.sliceArray(8 until serialized.size))
        assertEquals(data, deserialized)
    }
}