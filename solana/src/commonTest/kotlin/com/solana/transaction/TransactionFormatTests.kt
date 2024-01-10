package com.solana.transaction

import com.solana.encoders.Base64
import com.solana.publickey.SolanaPublicKey
import com.solana.serialization.TransactionFormat
import com.solana.util.asVarint
import kotlin.experimental.or
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class TransactionFormatTests {

    @Test
    fun testSerializeInstruction() {
        // given
        val programIdIndex = 1.toUByte()
        val accountAddressIndices = byteArrayOf(0)
        val data = "hello world".encodeToByteArray()
        val instruction = Instruction(programIdIndex, accountAddressIndices, data)

        val expectedBytes = byteArrayOf(programIdIndex.toByte()) +
                accountAddressIndices.size.asVarint() + accountAddressIndices +
                data.size.asVarint() + data

        // when
        val serializedTransaction = TransactionFormat.encodeToByteArray(Instruction.serializer(), instruction)

        // then
        assertContentEquals(expectedBytes, serializedTransaction)
    }

    @Test
    fun testSerializeLegacyMessage() {
        // given
        val accounts = listOf(
            SolanaPublicKey(ByteArray(32) {0}),
            SolanaPublicKey(ByteArray(32) {1}),
            SolanaPublicKey(ByteArray(32) {2}),
            SolanaPublicKey(ByteArray(32) {3})
        )

        val blockhash = Blockhash(ByteArray(32) {9})

        val programIdIndex = 1.toUByte()
        val accountAddressIndices = byteArrayOf(0)
        val data = "hello world".encodeToByteArray()
        val instruction = Instruction(programIdIndex, accountAddressIndices, data)

        val message = LegacyMessage(1.toUByte(), 2.toUByte(), 3.toUByte(), accounts, blockhash, listOf(instruction))

        val expectedBytes =
            byteArrayOf(message.signatureCount.toByte(), message.readOnlyAccounts.toByte(), message.readOnlyNonSigners.toByte()) +
                    4.asVarint() + ByteArray(32) {0} + ByteArray(32) {1} + ByteArray(32) {2} + ByteArray(32) {3} +
                    ByteArray(32) {9} +
                    1.asVarint() + programIdIndex.toInt().asVarint() + byteArrayOf(1, 0) + data.size.asVarint() + data

        // when
        val serializedMessage = TransactionFormat.encodeToByteArray(MessageSerializer, message)

        // then
        assertContentEquals(expectedBytes, serializedMessage)
    }

    @Test
    fun testSerializeVersionedMessage() {
        // given
        val accounts = listOf(
            SolanaPublicKey(ByteArray(32) {0}),
            SolanaPublicKey(ByteArray(32) {1}),
            SolanaPublicKey(ByteArray(32) {2}),
            SolanaPublicKey(ByteArray(32) {3})
        )

        val blockhash = Blockhash(ByteArray(32) {9})

        val programIdIndex = 1.toUByte()
        val accountAddressIndices = byteArrayOf(0)
        val data = "hello world".encodeToByteArray()
        val instruction = Instruction(programIdIndex, accountAddressIndices, data)

        val message = VersionedMessage(0, 1.toUByte(), 2.toUByte(), 3.toUByte(), accounts, blockhash, listOf(instruction), listOf())

        val expectedBytes =
            byteArrayOf(0x80.toByte() or message.version, message.signatureCount.toByte(), message.readOnlyAccounts.toByte(), message.readOnlyNonSigners.toByte()) +
                    4.asVarint() + ByteArray(32) {0} + ByteArray(32) {1} + ByteArray(32) {2} + ByteArray(32) {3} +
                    ByteArray(32) {9} +
                    1.asVarint() + programIdIndex.toInt().asVarint() + byteArrayOf(1, 0) + data.size.asVarint() + data + byteArrayOf(0)

        // when
        val serializedMessage = TransactionFormat.encodeToByteArray(MessageSerializer, message)

        // then
        assertContentEquals(expectedBytes, serializedMessage)
    }

    @Test
    fun testSerializeLegacyTransaction() {
        // given
        val account = SolanaPublicKey(Base64.decode("XJy50755nz75BGthIrxe7XIQ9WkcMxgIOCmqEM30qq4"))
        val programId = SolanaPublicKey.from("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr")
        val blockhash = Blockhash(ByteArray(32))
        val signature = ByteArray(64)
        val data = "hello world ".encodeToByteArray()

        val memoTransactionTemplate =
                //region signature
                byteArrayOf(1) + // 1 signature required (fee payer)
                signature +
                //endregion
                //region sign data
                byteArrayOf(
                    0x01.toByte(), // 1 signature required (fee payer)
                    0x00.toByte(), // 0 read-only account signatures
                    0x01.toByte(), // 1 read-only account not requiring a signature
                    0x02.toByte(), // 2 accounts
                ) + account.bytes + programId.bytes + blockhash.bytes +
                //endregion
                //region instructions
                byteArrayOf(
                    0x01.toByte(), // 1 instruction (memo)
                    0x01.toByte(), // program ID (index into list of accounts)
                    0x01.toByte(), // 1 account
                    0x00.toByte(), // account index 0
                    0x0C.toByte(), // 20 byte payload
                ) + data

        val accounts = listOf(account, programId)
        val instructions = listOf(Instruction(1u, byteArrayOf(0), data))
        val memoTxMessage = LegacyMessage(
            1u,
            0u,
            1u,
            accounts,
            blockhash,
            instructions
        )

        val transaction = Transaction(listOf(signature), memoTxMessage)

        // when
        val transactionBytes = TransactionFormat.encodeToByteArray(Transaction.serializer(), transaction)

        val accountsOffset = 1 + 64 + 4
        val blockhashOffset = accountsOffset + 2*SolanaPublicKey.PUBLIC_KEY_LENGTH
        val instructionOffset = blockhashOffset + SolanaPublicKey.PUBLIC_KEY_LENGTH

        // then
        assertEquals(memoTransactionTemplate[0], transactionBytes[0]) // signature count
        assertContentEquals(memoTransactionTemplate.slice(1 .. signature.size), transactionBytes.slice(1 .. signature.size))
        assertEquals(memoTransactionTemplate[signature.size + 1].toUByte(), memoTxMessage.signatureCount)
        assertEquals(memoTransactionTemplate[signature.size + 2].toUByte(), memoTxMessage.readOnlyAccounts)
        assertEquals(memoTransactionTemplate[signature.size + 3].toUByte(), memoTxMessage.readOnlyNonSigners)
        assertEquals(memoTransactionTemplate[signature.size + 4], 2) // number of accounts

        assertContentEquals(
            account.bytes.asList(),
            transactionBytes.slice(accountsOffset until accountsOffset + SolanaPublicKey.PUBLIC_KEY_LENGTH)
        )
        assertContentEquals(
            programId.bytes.asList(),
            transactionBytes.slice(accountsOffset + SolanaPublicKey.PUBLIC_KEY_LENGTH until blockhashOffset)
        )

        assertContentEquals(
            blockhash.bytes.asList(),
            transactionBytes.slice(blockhashOffset until blockhashOffset + SolanaPublicKey.PUBLIC_KEY_LENGTH)
        )

        assertEquals(1, transactionBytes[instructionOffset]) // instruction count
        assertEquals(1, transactionBytes[instructionOffset + 1]) // program id index
        assertEquals(1, transactionBytes[instructionOffset + 2]) // account count
        assertEquals(0, transactionBytes[instructionOffset + 3]) // account index

        assertEquals(12, transactionBytes[instructionOffset + 4]) // data length
        assertContentEquals(data, transactionBytes.sliceArray(instructionOffset + 5 .. instructionOffset + 4 + 12))

        assertContentEquals(memoTransactionTemplate, transactionBytes)
    }

    @Test
    fun testSerializeVersionedTransaction() {
        // given
        val account = SolanaPublicKey(Base64.decode("XJy50755nz75BGthIrxe7XIQ9WkcMxgIOCmqEM30qq4"))
        val programId = SolanaPublicKey.from("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr")
        val blockhash = Blockhash(ByteArray(32))
        val signature = ByteArray(64)
        val data = "hello world ".encodeToByteArray()

        val memoTransactionTemplate =
            //region signature
            byteArrayOf(1) + // 1 signature required (fee payer)
            signature +
            //endregion
            byteArrayOf(0x80.toByte()) + // prefix 0b10000000
            //region sign data
            byteArrayOf(
                0x01.toByte(), // 1 signature required (fee payer)
                0x00.toByte(), // 0 read-only account signatures
                0x01.toByte(), // 1 read-only account not requiring a signature
                0x02.toByte(), // 2 accounts
            ) + account.bytes + programId.bytes + blockhash.bytes +
            //endregion
            //region instructions
            byteArrayOf(
                0x01.toByte(), // 1 instruction (memo)
                0x01.toByte(), // program ID (index into list of accounts)
                0x01.toByte(), // 1 account
                0x00.toByte(), // account index 0
                0x0C.toByte(), // 20 byte payload
            ) + data +
            //endregion
            //region address table lookups
            byteArrayOf(0x00.toByte()) // 0 address table lookups
            //endregion

        val accounts = listOf(account, programId)
        val instructions = listOf(Instruction(1u, byteArrayOf(0), data))
        val memoTxMessage = VersionedMessage(
            0,
            1u,
            0u,
            1u,
            accounts,
            blockhash,
            instructions,
            listOf()
        )

        val transaction = Transaction(listOf(signature), memoTxMessage)

        // when
        val transactionBytes = TransactionFormat.encodeToByteArray(Transaction.serializer(), transaction)

        val accountsOffset = 1 + 64 + 4 + 1
        val blockhashOffset = accountsOffset + 2*SolanaPublicKey.PUBLIC_KEY_LENGTH
        val instructionOffset = blockhashOffset + SolanaPublicKey.PUBLIC_KEY_LENGTH

        // then
        assertEquals(memoTransactionTemplate[0], transactionBytes[0]) // signature count
        assertContentEquals(memoTransactionTemplate.slice(1 .. signature.size), transactionBytes.slice(1 .. signature.size))
        assertEquals(memoTransactionTemplate[signature.size + 2].toUByte(), memoTxMessage.signatureCount)
        assertEquals(memoTransactionTemplate[signature.size + 3].toUByte(), memoTxMessage.readOnlyAccounts)
        assertEquals(memoTransactionTemplate[signature.size + 4].toUByte(), memoTxMessage.readOnlyNonSigners)
        assertEquals(memoTransactionTemplate[signature.size + 5], 2) // number of accounts

        assertContentEquals(
            account.bytes.asList(),
            transactionBytes.slice(accountsOffset until accountsOffset + SolanaPublicKey.PUBLIC_KEY_LENGTH)
        )
        assertContentEquals(
            programId.bytes.asList(),
            transactionBytes.slice(accountsOffset + SolanaPublicKey.PUBLIC_KEY_LENGTH until blockhashOffset)
        )

        assertContentEquals(
            blockhash.bytes.asList(),
            transactionBytes.slice(blockhashOffset until blockhashOffset + SolanaPublicKey.PUBLIC_KEY_LENGTH)
        )

        assertEquals(1, transactionBytes[instructionOffset]) // instruction count
        assertEquals(1, transactionBytes[instructionOffset + 1]) // program id index
        assertEquals(1, transactionBytes[instructionOffset + 2]) // account count
        assertEquals(0, transactionBytes[instructionOffset + 3]) // account index

        assertEquals(12, transactionBytes[instructionOffset + 4]) // data length
        assertContentEquals(data, transactionBytes.sliceArray(instructionOffset + 5 .. instructionOffset + 4 + 12))

        assertContentEquals(memoTransactionTemplate, transactionBytes)
    }

    @Test
    fun testDeserializeLegacyTransaction() {
        // given
        val account = SolanaPublicKey(Base64.decode("XJy50755nz75BGthIrxe7XIQ9WkcMxgIOCmqEM30qq4"))
        val programId = SolanaPublicKey.from("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr")
        val blockhash = Blockhash(ByteArray(32))
        val signature = ByteArray(64)
        val data = "hello world ".encodeToByteArray()

        val memoTransactionBytes =
            //region signature
            byteArrayOf(1) + // 1 signature required (fee payer)
            signature +
            //endregion
            //region sign data
            byteArrayOf(
                0x01.toByte(), // 1 signature required (fee payer)
                0x00.toByte(), // 0 read-only account signatures
                0x01.toByte(), // 1 read-only account not requiring a signature
                0x02.toByte(), // 2 accounts
            ) + account.bytes + programId.bytes + blockhash.bytes +
            //endregion
            //region instructions
            byteArrayOf(
                0x01.toByte(), // 1 instruction (memo)
                0x01.toByte(), // program ID (index into list of accounts)
                0x01.toByte(), // 1 account
                0x00.toByte(), // account index 0
                0x0C.toByte(), // 20 byte payload
            ) + data

        val accounts = listOf(account, programId)
        val instructions = listOf(Instruction(1u, byteArrayOf(0), data))
        val memoTxMessage = LegacyMessage(
            1u,
            0u,
            1u,
            accounts,
            blockhash,
            instructions
        )

        // when
        val transaction = TransactionFormat.decodeFromByteArray(Transaction.serializer(), memoTransactionBytes)
        val transactionBytes = TransactionFormat.encodeToByteArray(Transaction.serializer(), transaction)

        // then
        assertEquals(1, transaction.signatures.size)
        assertContentEquals(signature, transaction.signatures.first())
        assertEquals(memoTxMessage.signatureCount, (transaction.message as LegacyMessage).signatureCount)
        assertEquals(memoTxMessage.readOnlyAccounts, (transaction.message as LegacyMessage).readOnlyAccounts)
        assertEquals(memoTxMessage.readOnlyNonSigners, (transaction.message as LegacyMessage).readOnlyNonSigners)
        assertContentEquals(memoTxMessage.accounts, (transaction.message as LegacyMessage).accounts)
        assertEquals(memoTxMessage.blockhash, (transaction.message as LegacyMessage).blockhash)
        assertContentEquals(memoTxMessage.instructions, (transaction.message as LegacyMessage).instructions)

        assertContentEquals(memoTransactionBytes, transactionBytes)
    }

    @Test
    fun testDeserializeVersionedTransaction() {
        // given
        val account = SolanaPublicKey(Base64.decode("XJy50755nz75BGthIrxe7XIQ9WkcMxgIOCmqEM30qq4"))
        val programId = SolanaPublicKey.from("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr")
        val blockhash = Blockhash(ByteArray(32))
        val signature = ByteArray(64)
        val data = "hello world ".encodeToByteArray()

        val memoTransactionBytes =
            //region signature
            byteArrayOf(1) + // 1 signature required (fee payer)
            signature +
            //endregion
            byteArrayOf(0x80.toByte()) + // prefix 0b10000000
            //region sign data
            byteArrayOf(
                0x01.toByte(), // 1 signature required (fee payer)
                0x00.toByte(), // 0 read-only account signatures
                0x01.toByte(), // 1 read-only account not requiring a signature
                0x02.toByte(), // 2 accounts
            ) + account.bytes + programId.bytes + blockhash.bytes +
            //endregion
            //region instructions
            byteArrayOf(
                0x01.toByte(), // 1 instruction (memo)
                0x01.toByte(), // program ID (index into list of accounts)
                0x01.toByte(), // 1 account
                0x00.toByte(), // account index 0
                0x0C.toByte(), // 20 byte payload
            ) + data +
            //endregion
            //region address table lookups
            byteArrayOf(0x00.toByte()) // 0 address table lookups
            //endregion

        val accounts = listOf(account, programId)
        val instructions = listOf(Instruction(1u, byteArrayOf(0), data))
        val memoTxMessage = VersionedMessage(
            0,
            1u,
            0u,
            1u,
            accounts,
            blockhash,
            instructions,
            listOf()
        )

        // when
        val transaction = TransactionFormat.decodeFromByteArray(Transaction.serializer(), memoTransactionBytes)
        val transactionBytes = TransactionFormat.encodeToByteArray(Transaction.serializer(), transaction)

        // then
        assertEquals(1, transaction.signatures.size)
        assertContentEquals(signature, transaction.signatures.first())
        assertEquals(memoTxMessage.version, (transaction.message as VersionedMessage).version)
        assertEquals(memoTxMessage.signatureCount, (transaction.message as VersionedMessage).signatureCount)
        assertEquals(memoTxMessage.readOnlyAccounts, (transaction.message as VersionedMessage).readOnlyAccounts)
        assertEquals(memoTxMessage.readOnlyNonSigners, (transaction.message as VersionedMessage).readOnlyNonSigners)
        assertContentEquals(memoTxMessage.accounts, (transaction.message as VersionedMessage).accounts)
        assertEquals(memoTxMessage.blockhash, (transaction.message as VersionedMessage).blockhash)
        assertContentEquals(memoTxMessage.instructions, (transaction.message as VersionedMessage).instructions)
        assertContentEquals(memoTxMessage.addressTableLookups, (transaction.message as VersionedMessage).addressTableLookups)

        assertContentEquals(memoTransactionBytes, transactionBytes)
    }
}