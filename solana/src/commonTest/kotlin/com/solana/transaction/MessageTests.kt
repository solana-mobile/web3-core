package com.solana.transaction

import com.funkatronics.encoders.Base64
import com.solana.publickey.SolanaPublicKey
import com.solana.util.asVarint
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MessageTests {

    @Test
    fun testMessageSerialize() {
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
        val serializedMessage = message.serialize()

        // then
        assertContentEquals(expectedBytes, serializedMessage)
    }

    @Test
    fun testBuildMessage() {
        // given
        val account = SolanaPublicKey(Base64.decode("XJy50755nz75BGthIrxe7XIQ9WkcMxgIOCmqEM30qq4"))
        val programId = SolanaPublicKey.from("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr")
        val blockhash = Blockhash(ByteArray(32))
        val data = "hello world ".encodeToByteArray()

        val memoInstruction = TransactionInstruction(
            programId,
            listOf(AccountMeta(account, true, true)),
            data
        )

        val memoInstructionTemplate =
            //region sign data
            byteArrayOf(
                0x01.toByte(), // 1 signature required (fee payer)
                0x00.toByte(), // 0 read-only account signatures
                0x00.toByte(), // 0 read-only account not requiring a signature
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

        // when
        val message = Message.Builder()
            .addInstruction(memoInstruction)
            .setRecentBlockhash(blockhash)
            .build()

        val serializedMessage = message.serialize()

        // then
        assertContentEquals(memoInstructionTemplate, serializedMessage)
    }

    @Test
    fun testMessageToUnsignedTransaction() {
        // given
        val account = SolanaPublicKey(Base64.decode("XJy50755nz75BGthIrxe7XIQ9WkcMxgIOCmqEM30qq4"))
        val programId = SolanaPublicKey.from("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr")
        val blockhash = Blockhash(ByteArray(32))
        val data = "hello world ".encodeToByteArray()
        val expectedSignatureCount = 1
        val expectedSignature = ByteArray(Transaction.SIGNATURE_LENGTH_BYTES)

        val memoInstruction = TransactionInstruction(
            programId,
            listOf(AccountMeta(account, true, true)),
            data
        )

        // when
        val transaction = Message.Builder()
            .addInstruction(memoInstruction)
            .setRecentBlockhash(blockhash)
            .build()
            .toUnsignedTransaction()

        // then
        assertEquals(expectedSignatureCount, transaction.signatures.size)
        assertContentEquals(expectedSignature, transaction.signatures.first())
    }

    @Test
    fun testMessageToUnsignedTransactionMultipleSignatures() {
        // given
        val account1 = SolanaPublicKey(Base64.decode("XJy40744nz74BGthIrxe7XIQ9WkcMxgIOCmqEM30qq4"))
        val account2 = SolanaPublicKey(Base64.decode("YJy50755nz75BGthIrxe7XIQ9WkcMxgIOCmqEM30qq5"))
        val programId = SolanaPublicKey.from("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr")
        val blockhash = Blockhash(ByteArray(32))
        val data = "hello world ".encodeToByteArray()
        val expectedSignatureCount = 2
        val expectedSignature = ByteArray(Transaction.SIGNATURE_LENGTH_BYTES)

        val memoInstruction1 = TransactionInstruction(
            programId,
            listOf(AccountMeta(account1, true, true)),
            data
        )

        val memoInstruction2 = TransactionInstruction(
            programId,
            listOf(AccountMeta(account2, true, true)),
            data
        )

        // when
        val transaction = Message.Builder()
            .addInstruction(memoInstruction1)
            .addInstruction(memoInstruction2)
            .setRecentBlockhash(blockhash)
            .build()
            .toUnsignedTransaction()

        // then
        assertEquals(expectedSignatureCount, transaction.signatures.size)
        assertContentEquals(expectedSignature, transaction.signatures[0])
        assertContentEquals(expectedSignature, transaction.signatures[1])
    }

    @Test
    fun testBuildMessageWithFeePayer() {
        // given
        val account1 = SolanaPublicKey.from("11111111111111111111111111111111")
        val account2 = SolanaPublicKey.from("22222222222222222222222222222222222222222222")
        val account3 = SolanaPublicKey.from("33333333333333333333333333333333333333333333")
        val programId = SolanaPublicKey.from("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr")
        val blockhash = Blockhash(ByteArray(32))
        val expectedMessage = LegacyMessage(
            signatureCount = 1.toUByte(), // 1 signer
            readOnlyAccounts = 0u,
            readOnlyNonSigners = 1u,
            accounts = listOf(
                account3, // payer must go first
                account1,
                account2,
                programId
            ),
            blockhash = blockhash,
            instructions = listOf(
                Instruction(
                    programIdIndex = 3u,
                    accountIndices = byteArrayOf(1, 2, 3),
                    data = byteArrayOf()
                )
            )
        )

        // when
        val message = Message.Builder()
            .addInstruction(TransactionInstruction(
                programId,
                listOf(
                    AccountMeta(account1, isSigner = false, isWritable = true),
                    AccountMeta(account2, isSigner = false, isWritable = true),
                    AccountMeta(programId, isSigner = false, isWritable = false)
                ),
                byteArrayOf()
            ))
            .setRecentBlockhash(blockhash)
            .addFeePayer(account3)
            .build()

        // then
        assertEquals(expectedMessage, message)
    }

    @Test
    fun testBuildMessageWithDuplicateFeePayer() {
        // given
        val account1 = SolanaPublicKey.from("11111111111111111111111111111111")
        val account2 = SolanaPublicKey.from("22222222222222222222222222222222222222222222")
        val account3 = SolanaPublicKey.from("33333333333333333333333333333333333333333333")
        val programId = SolanaPublicKey.from("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr")
        val blockhash = Blockhash(ByteArray(32))
        val expectedMessage = LegacyMessage(
            signatureCount = 1.toUByte(), // 1 signer
            readOnlyAccounts = 0u,
            readOnlyNonSigners = 1u,
            accounts = listOf(
                account3, // payer must go first
                account1,
                account2,
                programId
            ),
            blockhash = blockhash,
            instructions = listOf(
                Instruction(
                    programIdIndex = 3u,
                    accountIndices = byteArrayOf(1, 2, 0, 3),
                    data = byteArrayOf()
                )
            )
        )

        // when
        val message = Message.Builder()
            .addInstruction(TransactionInstruction(
                programId,
                listOf(
                    AccountMeta(account1, isSigner = false, isWritable = true),
                    AccountMeta(account2, isSigner = false, isWritable = true),
                    AccountMeta(account3, isSigner = false, isWritable = false),
                    AccountMeta(programId, isSigner = false, isWritable = false)
                ),
                byteArrayOf()
            ))
            .setRecentBlockhash(blockhash)
            .addFeePayer(account3)
            .build()

        // then
        assertEquals(expectedMessage, message)
    }

    @Test
    fun testBuildMessageWithoutFeePayerThrows() {
        // given
        val account = SolanaPublicKey.from("11111111111111111111111111111111")
        val programId = SolanaPublicKey.from("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr")
        val blockhash = Blockhash(ByteArray(32))

        // when
        val message = Message.Builder()
            .addInstruction(TransactionInstruction(
                programId,
                listOf(
                    AccountMeta(account, isSigner = false, isWritable = true),
                    AccountMeta(programId, isSigner = false, isWritable = false)
                ),
                byteArrayOf()
            ))
            .setRecentBlockhash(blockhash)

        // then
        assertFailsWith<IllegalStateException> {
            message.build()
        }
    }

    @Test
    fun testBuildMessageWithDuplicateAccountMeta() {
        // given
        val account1 = SolanaPublicKey.from("11111111111111111111111111111111")
        val account2 = SolanaPublicKey.from("22222222222222222222222222222222222222222222")
        val account3 = SolanaPublicKey.from("33333333333333333333333333333333333333333333")
        val programId = SolanaPublicKey.from("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr")
        val blockhash = Blockhash(ByteArray(32))
        val expectedMessage = LegacyMessage(
            signatureCount = 1.toUByte(), // 1 signer
            readOnlyAccounts = 0u,
            readOnlyNonSigners = 2u,
            accounts = listOf(
                account3, // payer must go first
                account1,
                account2,
                programId
            ),
            blockhash = blockhash,
            instructions = listOf(
                Instruction(
                    programIdIndex = 3u,
                    accountIndices = byteArrayOf(1, 2, 0, 0, 3),
                    data = byteArrayOf()
                )
            )
        )

        // when
        val message = Message.Builder()
            .addInstruction(TransactionInstruction(
                programId,
                listOf(
                    AccountMeta(account1, isSigner = false, isWritable = true),
                    AccountMeta(account2, isSigner = false, isWritable = false),
                    AccountMeta(account3, isSigner = false, isWritable = false),
                    AccountMeta(account3, isSigner = true, isWritable = true),
                    AccountMeta(programId, isSigner = false, isWritable = false)
                ),
                byteArrayOf()
            ))
            .setRecentBlockhash(blockhash)
            .build()

        // then
        assertEquals(expectedMessage, message)
    }

    @Test
    fun testBuildMessageWithTriplicateAccountMeta() {
        // given
        val account1 = SolanaPublicKey.from("11111111111111111111111111111111")
        val account2 = SolanaPublicKey.from("22222222222222222222222222222222222222222222")
        val account3 = SolanaPublicKey.from("33333333333333333333333333333333333333333333")
        val programId = SolanaPublicKey.from("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr")
        val blockhash = Blockhash(ByteArray(32))
        val expectedMessage = LegacyMessage(
            signatureCount = 1.toUByte(), // 1 signer
            readOnlyAccounts = 0u,
            readOnlyNonSigners = 1u,
            accounts = listOf(
                account3, // payer must go first
                account1,
                account2,
                programId
            ),
            blockhash = blockhash,
            instructions = listOf(
                Instruction(
                    programIdIndex = 3u,
                    accountIndices = byteArrayOf(1, 2, 0, 0, 0, 3),
                    data = byteArrayOf()
                )
            )
        )

        // when
        val message = Message.Builder()
            .addInstruction(TransactionInstruction(
                programId,
                listOf(
                    AccountMeta(account1, isSigner = false, isWritable = true),
                    AccountMeta(account2, isSigner = false, isWritable = true),
                    AccountMeta(account3, isSigner = false, isWritable = false),
                    AccountMeta(account3, isSigner = true, isWritable = true),
                    AccountMeta(account3, isSigner = true, isWritable = false),
                    AccountMeta(programId, isSigner = false, isWritable = false)
                ),
                byteArrayOf()
            ))
            .setRecentBlockhash(blockhash)
            .build()

        // then
        assertEquals(expectedMessage, message)
    }
}