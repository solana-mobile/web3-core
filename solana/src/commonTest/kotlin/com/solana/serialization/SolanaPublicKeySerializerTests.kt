package com.solana.serialization

import com.funkatronics.kborsh.Borsh
import com.solana.publickey.SolanaPublicKey
import com.solana.publickey.SolanaPublicKeySerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class SolanaPublicKeySerializerTests {

    @Test
    fun `Json serializes as base58 string`() {
        // given
        val publicKeyBase58 = "11111111111111111111111111111111"
        val publicKey = SolanaPublicKey.from(publicKeyBase58)

        // when
        val serialized = Json.encodeToString(SolanaPublicKeySerializer, publicKey)

        // then
        assertEquals("\"$publicKeyBase58\"", serialized)
    }

    @Test
    fun `Json deserializes from base58 string`() {
        // given
        @Serializable
        data class TestStruct(val owner: SolanaPublicKey)

        val publicKeyBase58 = "11111111111111111111111111111111"
        val publicKey = SolanaPublicKey.from(publicKeyBase58)
        val json = """
            {
                "owner": "$publicKeyBase58"
            }
        """.trimIndent()

        // when
        val deserialized = Json.decodeFromString<TestStruct>(json)

        // then
        assertEquals(publicKey, deserialized.owner)
    }

    @Test
    fun `Borsh serializes as base58 string`() {
        // given
        val publicKeyBase58 = "11111111111111111111111111111111"
        val publicKey = SolanaPublicKey.from(publicKeyBase58)

        // when
        val serialized = Borsh.encodeToByteArray(SolanaPublicKeySerializer, publicKey)

        // then
        assertContentEquals(publicKey.bytes, serialized)
    }

    @Test
    fun `Borsh deserializes from base58 string`() {
        // given
        val publicKeyBase58 = "11111111111111111111111111111111"
        val publicKey = SolanaPublicKey.from(publicKeyBase58)

        // when
        val deserialized = Borsh.decodeFromByteArray(SolanaPublicKeySerializer, publicKey.bytes)

        // then
        assertEquals(publicKey, deserialized)
    }

    @Test
    fun `Transaction encoder encodes public key bytes`() {
        // given
        val publicKeyBase58 = "11111111111111111111111111111111"
        val publicKey = SolanaPublicKey.from(publicKeyBase58)

        // when
        val serialized = TransactionFormat.encodeToByteArray(SolanaPublicKeySerializer, publicKey)

        // then
        assertContentEquals(publicKey.bytes, serialized)
    }

    @Test
    fun `Transaction encoder decodes public key bytes`() {
        // given
        val publicKeyBase58 = "11111111111111111111111111111111"
        val publicKey = SolanaPublicKey.from(publicKeyBase58)

        // when
        val deserialized = TransactionFormat.decodeFromByteArray(SolanaPublicKeySerializer, publicKey.bytes)

        // then
        assertEquals(publicKey, deserialized)
    }
}