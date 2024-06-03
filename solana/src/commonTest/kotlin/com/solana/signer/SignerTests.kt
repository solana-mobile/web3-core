package com.solana.signer

import com.funkatronics.encoders.Base64
import com.solana.publickey.SolanaPublicKey
import com.solana.transaction.*
import diglol.crypto.Ed25519
import diglol.crypto.KeyPair
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

class SignerTests {

    class TestSigner(val keyPair: KeyPair) : SolanaSigner() {
        override val publicKey: SolanaPublicKey = SolanaPublicKey(keyPair.publicKey)

        override suspend fun signAndSendTransaction(transaction: Transaction): Result<String> {
            TODO("Not yet implemented")
        }

        override suspend fun signPayload(payload: ByteArray): Result<ByteArray> =
            Result.success(Ed25519.sign(keyPair, payload))
    }

    @Test
    fun `Signer rejects signing transaction as off chain message`() = runTest {
        // given
        val signer = TestSigner(Ed25519.generateKeyPair())
        val programId = SolanaPublicKey.from("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr")
        val blockhash = Blockhash(ByteArray(32))
        val data = "hello world ".encodeToByteArray()

        val memoInstruction = TransactionInstruction(
            programId,
            listOf(AccountMeta(signer.publicKey, true, true)),
            data
        )

        val transactionMessage = Message.Builder()
            .addInstruction(memoInstruction)
            .setRecentBlockhash(blockhash)
            .build()

        // when
        val result = signer.signOffChainMessage(transactionMessage.serialize())

        // then
        assertTrue { result.isFailure }
        assertTrue { result.exceptionOrNull() is IllegalArgumentException }
    }

    @Test
    fun `Signer rejects transaction not requiring signer publickey`() = runTest {
        // given
        val signer = TestSigner(Ed25519.generateKeyPair())
        val account = SolanaPublicKey(Base64.decode("XJy50755nz75BGthIrxe7XIQ9WkcMxgIOCmqEM30qq4"))
        val programId = SolanaPublicKey.from("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr")
        val blockhash = Blockhash(ByteArray(32))
        val data = "hello world ".encodeToByteArray()

        val memoInstruction = TransactionInstruction(
            programId,
            listOf(AccountMeta(account, true, true)),
            data
        )

        val message = Message.Builder()
            .addInstruction(memoInstruction)
            .setRecentBlockhash(blockhash)
            .build()

        val transaction = Transaction(message)

        // when
        val result = signer.signTransaction(transaction)

        // then
        assertTrue { result.isFailure }
        assertTrue { result.exceptionOrNull() is IllegalArgumentException }
    }

    @Test
    fun `Signer signs payload with correct signature`() = runTest {
        // given
        val signer = TestSigner(Ed25519.generateKeyPair())
        val payload = Random.nextBytes(100)

        // when
        val result = signer.signOffChainMessage(payload)

        // then
        assertTrue { result.isSuccess }
        assertTrue { Ed25519.verify(result.getOrNull()!!, signer.publicKey.bytes, payload) }
    }

    @Test
    fun `Signer signs message with correct signature`() = runTest {
        // given
        val signer = TestSigner(Ed25519.generateKeyPair())
        val message = "hello world ".encodeToByteArray()

        // when
        val result = signer.signOffChainMessage(message)

        // then
        assertTrue { result.isSuccess }
        assertTrue { Ed25519.verify(result.getOrNull()!!, signer.publicKey.bytes, message) }
    }

    @Test
    fun `Signer signs transaction with correct signature`() = runTest {
        // given
        val signer = TestSigner(Ed25519.generateKeyPair())
        val programId = SolanaPublicKey.from("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr")
        val blockhash = Blockhash(ByteArray(32))
        val data = "hello world ".encodeToByteArray()

        val memoInstruction = TransactionInstruction(
            programId,
            listOf(AccountMeta(signer.publicKey, true, true)),
            data
        )

        val message = Message.Builder()
            .addInstruction(memoInstruction)
            .setRecentBlockhash(blockhash)
            .build()

        val transaction = Transaction(message)

        // when
        val result = signer.signTransaction(transaction)

        // then
        assertTrue { result.isSuccess }
        assertTrue { Ed25519.verify(result.getOrNull()!!.signatures.first(), signer.publicKey.bytes, transaction.message.serialize()) }
    }
}