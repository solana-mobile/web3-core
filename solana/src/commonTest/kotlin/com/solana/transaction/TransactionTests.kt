package com.solana.transaction

import com.funkatronics.encoders.Base64
import com.solana.publickey.SolanaPublicKey
import com.solana.serialization.TransactionFormat
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TransactionTests {

    @Test
    fun testLegacyTransaction() {
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
            ) + data //+
            //endregion

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
        assertEquals(memoTransactionTemplate[signature.size + 4].toInt(), memoTxMessage.accounts.size)

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
    fun testV0Transaction() {
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
        val memoTxMessage = V0Message(
            1u,
            0u,
            1u,
            blockhash,
            accounts,
            instructions,
            listOf()
        )

        val transaction = Transaction(listOf(signature), memoTxMessage)

        // when
        val transactionBytes = TransactionFormat.encodeToByteArray(Transaction.serializer(), transaction)

        val accountsOffset = 1 + 64 + 1 + 4
        val blockhashOffset = accountsOffset + 2*SolanaPublicKey.PUBLIC_KEY_LENGTH
        val instructionOffset = blockhashOffset + SolanaPublicKey.PUBLIC_KEY_LENGTH

        // then
        assertEquals(memoTransactionTemplate[0], transactionBytes[0]) // signature count
        assertContentEquals(memoTransactionTemplate.slice(1 .. signature.size), transactionBytes.slice(1 .. signature.size))
        assertEquals(memoTransactionTemplate[signature.size + 2].toUByte(), memoTxMessage.signatureCount)
        assertEquals(memoTransactionTemplate[signature.size + 3].toUByte(), memoTxMessage.readOnlyAccounts)
        assertEquals(memoTransactionTemplate[signature.size + 4].toUByte(), memoTxMessage.readOnlyNonSigners)
        assertEquals(memoTransactionTemplate[signature.size + 5].toInt(), memoTxMessage.accounts.size)

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
    fun testV1Transaction() {
        // given
        val account = SolanaPublicKey(Base64.decode("XJy50755nz75BGthIrxe7XIQ9WkcMxgIOCmqEM30qq4"))
        val programId = SolanaPublicKey.from("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr")
        val blockhash = Blockhash(ByteArray(32))
        val signature = ByteArray(64)
        val data = "hello world ".encodeToByteArray()

        val memoTransactionTemplate =
            byteArrayOf(0x81.toByte()) + // prefix 0b10000001
            byteArrayOf( // header
                0x01.toByte(), // 1 signature required (fee payer)
                0x00.toByte(), // 0 read-only account signatures
                0x01.toByte(), // 1 read-only account not requiring a signature
            ) +
            byteArrayOf( // config mask
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x00.toByte(),
            ) +
            blockhash.bytes + // blockhash
            1.toByte() + // num instructions
            2.toByte() + // num accounts
            account.bytes + programId.bytes + // accounts
            // config values (none)
            //region instructions
            byteArrayOf( // headers (1 ix -> 4 bytes)
                0x01.toByte(), // program ID (index into list of accounts)
                0x01.toByte(), // 1 account
                0x0C.toByte(), 0x00.toByte(), // 12 byte payload, u16
            ) +
            0x00.toByte() + data + // memo ix: account index 0 + data
            //endregion
            // signature
            signature

        val accounts = listOf(account, programId)
        val instructions = listOf(Instruction(1u, byteArrayOf(0), data))
        val memoTxMessage = V1Message(
            1u,
            0u,
            1u,
            blockhash,
            accounts,
            instructions,
            TransactionConfig()
        )

        val transaction = Transaction(listOf(signature), memoTxMessage)

        // when
        val transactionBytes = TransactionFormat.encodeToByteArray(Transaction.serializer(), transaction)

        val blockhashOffset = 8
        val accountsOffset = 8 + 32 + 2
        val instructionOffset = accountsOffset + 2*SolanaPublicKey.PUBLIC_KEY_LENGTH

        // then
        assertEquals(memoTransactionTemplate[1].toUByte(), memoTxMessage.signatureCount)
        assertEquals(memoTransactionTemplate[2].toUByte(), memoTxMessage.readOnlyAccounts)
        assertEquals(memoTransactionTemplate[3].toUByte(), memoTxMessage.readOnlyNonSigners)

        assertContentEquals(memoTransactionTemplate.slice(4 .. 7), transactionBytes.slice(4 .. 7))

        assertContentEquals(
            blockhash.bytes.asList(),
            transactionBytes.slice(blockhashOffset until blockhashOffset + SolanaPublicKey.PUBLIC_KEY_LENGTH)
        )

        assertEquals(memoTransactionTemplate[8 + 32].toInt(), memoTxMessage.instructions.size) // number of instructions
        assertEquals(memoTransactionTemplate[8 + 32 + 1].toInt(), memoTxMessage.accounts.size) // number of accounts

        assertNull(memoTxMessage.config.priorityFeeLamports)
        assertNull(memoTxMessage.config.computeUnitLimit)
        assertNull(memoTxMessage.config.loadedAccountsDataSizeLimit)
        assertNull(memoTxMessage.config.requestedHeapSize)

        assertContentEquals(
            account.bytes.asList(),
            transactionBytes.slice(accountsOffset until accountsOffset + SolanaPublicKey.PUBLIC_KEY_LENGTH)
        )
        assertContentEquals(
            programId.bytes.asList(),
            transactionBytes.slice(accountsOffset + SolanaPublicKey.PUBLIC_KEY_LENGTH until instructionOffset)
        )

        assertEquals(1, transactionBytes[instructionOffset]) // program id index
        assertEquals(1, transactionBytes[instructionOffset + 1]) // num accounts
        assertEquals(12, transactionBytes[instructionOffset + 2]) // data len b0
        assertEquals(0, transactionBytes[instructionOffset + 3]) // data len b1

        assertEquals(0, transactionBytes[instructionOffset + 4]) // account index
        assertContentEquals(data, transactionBytes.sliceArray(instructionOffset + 5 .. instructionOffset + 4 + 12))

        assertContentEquals(memoTransactionTemplate.takeLast(signature.size), transactionBytes.takeLast(signature.size))

        assertContentEquals(memoTransactionTemplate, transactionBytes)
    }

    @Test
    fun testV1TransactionWithConfigValues() {
        // given
        val account = SolanaPublicKey(Base64.decode("XJy50755nz75BGthIrxe7XIQ9WkcMxgIOCmqEM30qq4"))
        val programId = SolanaPublicKey.from("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr")
        val blockhash = Blockhash(ByteArray(32))
        val signature = ByteArray(64) {7}
        val data = "hello world ".encodeToByteArray()
        val config = TransactionConfig(
            priorityFeeLamports = 5_000uL,
            computeUnitLimit = 200_000u,
            loadedAccountsDataSizeLimit = 65_536u,
            requestedHeapSize = 262_144u,
        )

        val memoTransactionTemplate =
            byteArrayOf(0x81.toByte()) + // prefix 0b10000001
            byteArrayOf(0x01, 0x00, 0x01) + // header
            byteArrayOf(0x1F, 0x00, 0x00, 0x00) + // config mask: priority fee (0x03) | CU limit (0x04) | loaded data size (0x08) | heap size (0x10)
            blockhash.bytes +
            byteArrayOf(0x01, 0x02) + // 1 instruction, 2 accounts
            account.bytes + programId.bytes +
            byteArrayOf(0x88.toByte(), 0x13, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00) + // priority fee 5000 (u64)
            byteArrayOf(0x40, 0x0D, 0x03, 0x00) + // compute unit limit 200000 (u32)
            byteArrayOf(0x00, 0x00, 0x01, 0x00) + // loaded accounts data size limit 65536 (u32)
            byteArrayOf(0x00, 0x00, 0x04, 0x00) + // requested heap size 262144 (u32)
            byteArrayOf(0x01, 0x01, 0x0C, 0x00) + // ix header: program id index 1, 1 account, 12 byte payload (u16)
            byteArrayOf(0x00) + data + // ix payload: account index 0 + data
            signature

        val memoTxMessage = V1Message(
            1u,
            0u,
            1u,
            blockhash,
            listOf(account, programId),
            listOf(Instruction(1u, byteArrayOf(0), data)),
            config
        )
        val transaction = Transaction(listOf(signature), memoTxMessage)

        // when
        val transactionBytes = TransactionFormat.encodeToByteArray(Transaction.serializer(), transaction)
        val decodedTransaction = TransactionFormat.decodeFromByteArray(Transaction.serializer(), memoTransactionTemplate)

        // then
        assertContentEquals(memoTransactionTemplate, transactionBytes)

        assertEquals(1, decodedTransaction.signatures.size)
        assertContentEquals(signature, decodedTransaction.signatures.first())
        assertEquals(config, (decodedTransaction.message as V1Message).config)
        assertContentEquals(memoTxMessage.accounts, decodedTransaction.message.accounts)
        assertContentEquals(memoTxMessage.instructions, decodedTransaction.message.instructions)
    }

    @Test
    fun testV1TransactionWithMultipleInstructionsAndSignatures() {
        // given
        val signer1 = SolanaPublicKey(ByteArray(32) {1})
        val signer2 = SolanaPublicKey(ByteArray(32) {2})
        val programId = SolanaPublicKey.from("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr")
        val blockhash = Blockhash(ByteArray(32))
        val signature1 = ByteArray(64) {1}
        val signature2 = ByteArray(64) {2}
        val data1 = "hello".encodeToByteArray()
        val data2 = "world!".encodeToByteArray()

        val memoTransactionTemplate =
            byteArrayOf(0x81.toByte()) + // prefix 0b10000001
            byteArrayOf(0x02, 0x00, 0x01) + // header: 2 signatures, 0 read-only signed, 1 read-only unsigned
            byteArrayOf(0x00, 0x00, 0x00, 0x00) + // config mask
            blockhash.bytes +
            byteArrayOf(0x02, 0x03) + // 2 instructions, 3 accounts
            signer1.bytes + signer2.bytes + programId.bytes +
            byteArrayOf(0x02, 0x01, 0x05, 0x00) + // ix 1 header: program id index 2, 1 account, 5 byte payload (u16)
            byteArrayOf(0x02, 0x01, 0x06, 0x00) + // ix 2 header: program id index 2, 1 account, 6 byte payload (u16)
            byteArrayOf(0x00) + data1 + // ix 1 payload: account index 0 + data
            byteArrayOf(0x01) + data2 + // ix 2 payload: account index 1 + data
            signature1 + signature2

        val memoTxMessage = V1Message(
            2u,
            0u,
            1u,
            blockhash,
            listOf(signer1, signer2, programId),
            listOf(
                Instruction(2u, byteArrayOf(0), data1),
                Instruction(2u, byteArrayOf(1), data2)
            ),
            TransactionConfig()
        )
        val transaction = Transaction(listOf(signature1, signature2), memoTxMessage)

        // when
        val transactionBytes = TransactionFormat.encodeToByteArray(Transaction.serializer(), transaction)
        val decodedTransaction = TransactionFormat.decodeFromByteArray(Transaction.serializer(), memoTransactionTemplate)

        // then
        assertContentEquals(memoTransactionTemplate, transactionBytes)

        assertEquals(2, decodedTransaction.signatures.size)
        assertContentEquals(signature1, decodedTransaction.signatures[0])
        assertContentEquals(signature2, decodedTransaction.signatures[1])
        assertEquals(memoTxMessage.signatureCount, decodedTransaction.message.signatureCount)
        assertContentEquals(memoTxMessage.accounts, decodedTransaction.message.accounts)
        assertContentEquals(memoTxMessage.instructions, decodedTransaction.message.instructions)
    }
}