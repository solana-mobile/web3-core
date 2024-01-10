package com.solana.transaction

import com.solana.encoders.Base64
import com.solana.publickey.SolanaPublicKey
import com.solana.serialization.TransactionFormat
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class TransactionTests {

    @Test
    fun testTransaction() {
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
//            byteArrayOf(0x80.toByte()) + // prefix 0b10000000
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
            //region address table lookups
//            byteArrayOf(0x00.toByte()) // 0 address table lookups
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
}