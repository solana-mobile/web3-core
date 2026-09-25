package com.solana.transaction

import com.funkatronics.encoders.Base64
import com.solana.publickey.SolanaPublicKey
import com.solana.serialization.TransactionFormat
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class V1MessageTests {

    @Test
    fun testSerializeV1Message() {
        // given
        val account = SolanaPublicKey(Base64.decode("XJy50755nz75BGthIrxe7XIQ9WkcMxgIOCmqEM30qq4"))
        val programId = SolanaPublicKey.from("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr")
        val blockhash = Blockhash(ByteArray(32) {9})
        val data = "hello world ".encodeToByteArray()

        val message = V1Message(
            signatureCount = 1u,
            readOnlyAccounts = 0u,
            readOnlyNonSigners = 1u,
            blockhash = blockhash,
            accounts = listOf(account, programId),
            instructions = listOf(Instruction(1u, byteArrayOf(0), data)),
            config = TransactionConfig()
        )

        val expectedBytes =
            byteArrayOf(0x81.toByte()) + // prefix 0b10000001
            byteArrayOf(0x01, 0x00, 0x01) + // header
            byteArrayOf(0x00, 0x00, 0x00, 0x00) + // config mask
            blockhash.bytes +
            byteArrayOf(0x01, 0x02) + // 1 instruction, 2 accounts
            account.bytes + programId.bytes +
            byteArrayOf(0x01, 0x01, 0x0C, 0x00) + // ix header: program id index 1, 1 account, 12 byte payload (u16)
            byteArrayOf(0x00) + data // ix payload: account index 0 + data

        // when
        val serializedMessage = TransactionFormat.encodeToByteArray(MessageSerializer, message)

        // then
        assertContentEquals(expectedBytes, serializedMessage)
    }

    @Test
    fun testDeserializeV1Message() {
        // given
        val account = SolanaPublicKey(Base64.decode("XJy50755nz75BGthIrxe7XIQ9WkcMxgIOCmqEM30qq4"))
        val programId = SolanaPublicKey.from("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr")
        val blockhash = Blockhash(ByteArray(32) {9})
        val data = "hello world ".encodeToByteArray()

        val messageBytes =
            byteArrayOf(0x81.toByte()) + // prefix 0b10000001
            byteArrayOf(0x01, 0x00, 0x01) + // header
            byteArrayOf(0x00, 0x00, 0x00, 0x00) + // config mask
            blockhash.bytes +
            byteArrayOf(0x01, 0x02) + // 1 instruction, 2 accounts
            account.bytes + programId.bytes +
            byteArrayOf(0x01, 0x01, 0x0C, 0x00) + // ix header: program id index 1, 1 account, 12 byte payload (u16)
            byteArrayOf(0x00) + data // ix payload: account index 0 + data

        val expectedMessage = V1Message(
            signatureCount = 1u,
            readOnlyAccounts = 0u,
            readOnlyNonSigners = 1u,
            blockhash = blockhash,
            accounts = listOf(account, programId),
            instructions = listOf(Instruction(1u, byteArrayOf(0), data)),
            config = TransactionConfig()
        )

        // when
        val message = Message.from(messageBytes)

        // then
        assertEquals(expectedMessage, message)
        assertContentEquals(messageBytes, message.serialize())
    }

    @Test
    fun testDeserializeV1MessageRejectsUnknownConfigMaskBits() {
        // given:
        val messageBytes =
            byteArrayOf(0x81.toByte()) + // V1_PREFIX
            byteArrayOf(1, 0, 0) + // header
            byteArrayOf(0x20, 0, 0, 0) + // config mask: bit 5, unknown (u32 LE)
            ByteArray(32) { 0xAB.toByte() } + // lifetime specifier (blockhash)
            byteArrayOf(1) + // num instructions
            byteArrayOf(2) + // num addresses
            ByteArray(32) { 1 } + // fee payer
            ByteArray(32) { 2 } + // program
            byteArrayOf(1, 1) + // ix header: program id index, num accounts
            byteArrayOf(0, 0) + // ix header: data len (u16 LE)
            byteArrayOf(0) // ix payload: account index 0

        // when / then
        assertFailsWith<SerializationException> {
            Message.from(messageBytes)
        }
    }

    @Test
    fun testDeserializeV1MessageRejectsPartialPriorityFeeBits() {
        // given:
        val messageBytes0 =
            byteArrayOf(0x81.toByte()) + // V1_PREFIX
            byteArrayOf(1, 0, 0) + // header
            byteArrayOf(0x01, 0, 0, 0) + // config mask: bit 0 only (u32 LE)
            ByteArray(32) { 0xAB.toByte() } + // lifetime specifier (blockhash)
            byteArrayOf(1) + // num instructions
            byteArrayOf(2) + // num addresses
            ByteArray(32) { 1 } + // fee payer
            ByteArray(32) { 2 } + // program
            byteArrayOf(1, 1) + // ix header: program id index, num accounts
            byteArrayOf(0, 0) + // ix header: data len (u16 LE)
            byteArrayOf(0) // ix payload: account index 0

        val messageBytes1 =
            byteArrayOf(0x81.toByte()) + // V1_PREFIX
            byteArrayOf(1, 0, 0) + // header
            byteArrayOf(0x02, 0, 0, 0) + // config mask: bit 1 only (u32 LE)
            ByteArray(32) { 0xAB.toByte() } + // lifetime specifier (blockhash)
            byteArrayOf(1) + // num instructions
            byteArrayOf(2) + // num addresses
            ByteArray(32) { 1 } + // fee payer
            ByteArray(32) { 2 } + // program
            byteArrayOf(1, 1) + // ix header: program id index, num accounts
            byteArrayOf(0, 0) + // ix header: data len (u16 LE)
            byteArrayOf(0) // ix payload: account index 0

        // when / then
        assertFailsWith<SerializationException> {
            Message.from(messageBytes0)
        }
        assertFailsWith<SerializationException> {
            Message.from(messageBytes1)
        }
    }
}