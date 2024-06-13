package com.solana.signer.mwa

import android.net.Uri
import androidx.activity.ComponentActivity
import com.funkatronics.encoders.Base64
import com.solana.mobilewalletadapter.clientlib.*
import com.solana.mobilewalletadapter.clientlib.protocol.MobileWalletAdapterClient
import com.solana.mobilewalletadapter.clientlib.protocol.MobileWalletAdapterClient.AuthorizationResult
import com.solana.mobilewalletadapter.clientlib.protocol.MobileWalletAdapterClient.SignMessagesResult.SignedMessage
import com.solana.publickey.SolanaPublicKey
import com.solana.transaction.*
import diglol.crypto.Ed25519
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class MwaSignerTests {

    lateinit var sender: ActivityResultSender
    lateinit var mobileWalletAdapter: MobileWalletAdapter

    @Before
    fun before() = runTest {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).create()

        sender = ActivityResultSender(controller.get())

        val keypair = Ed25519.generateKeyPair()
        val authResult = AuthorizationResult.create("AUTHRESULTTOKEN", keypair.publicKey, "Some Label", Uri.EMPTY)
        val adapterOps: AdapterOperations = mock {
            onBlocking { signMessagesDetached(anyArray(), anyArray()) } doAnswer { invocation ->
                val messages = invocation.arguments[0] as Array<ByteArray>
                val addresses = invocation.arguments[1] as Array<ByteArray>
                MobileWalletAdapterClient.SignMessagesResult(messages.map { msg ->
                    SignedMessage(msg, arrayOf(runBlocking { Ed25519.sign(keypair, msg) }), addresses)
                }.toTypedArray())
            }
            onBlocking { signTransactions(anyArray()) } doAnswer { invocation ->
                val transactions = invocation.arguments[0] as Array<ByteArray>
                MobileWalletAdapterClient.SignPayloadsResult(transactions.map { txn ->
                    runBlocking { Ed25519.sign(keypair, txn) }
                }.toTypedArray())
            }
        }

        mobileWalletAdapter = mock {
            val test2 = argumentCaptor<suspend AdapterOperations.(AuthorizationResult) -> Any>()
            onBlocking { transact(any(), anyOrNull(), test2.capture()) } doAnswer { invocation ->
                val block = invocation.arguments[2] as suspend AdapterOperations.(AuthorizationResult) -> Any
                runBlocking {
                    TransactionResult.Success(block.invoke(adapterOps, authResult))
                }
            }
        }
    }

    @Test
    fun `Mwa Signer rejects sign transaction as off chain message`() = runTest {
        // given
        val signer = MwaSigner(mobileWalletAdapter, sender)
        val programId = SolanaPublicKey.from("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr")
        val blockhash = Blockhash(ByteArray(32))
        val data = "hello world ".encodeToByteArray()

        // when
//        signer.connect()

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
        val result = signer.signOffChainMessage(transaction.serialize())

        // then
        assertTrue { result.isFailure }
        assertTrue { result.exceptionOrNull() is IllegalArgumentException }
    }

    @Test
    fun `Mwa Signer rejects transaction not requiring signer publickey`() = runTest {
        // given
        val signer = MwaSigner(mobileWalletAdapter, sender)
        val account = SolanaPublicKey(Base64.decode("XJy50755nz75BGthIrxe7XIQ9WkcMxgIOCmqEM30qq4"))
        val programId = SolanaPublicKey.from("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr")
        val blockhash = Blockhash(ByteArray(32))
        val data = "hello world ".encodeToByteArray()

        // when
//        signer.connect()

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
        val result = signer.signTransaction(transaction)

        // then
        assertTrue { result.isFailure }
        assertTrue { result.exceptionOrNull() is IllegalArgumentException }
    }

    @Test
    fun `Mwa Signer signs off chain message with correct signature`() = runTest {
        // given
        val signer = MwaSigner(mobileWalletAdapter, sender)
        val message = "hello world ".encodeToByteArray()

        // when
        val result = signer.signOffChainMessage(message)

        // then
        assertTrue { result.isSuccess }
        assertTrue { Ed25519.verify(result.getOrNull()!!, signer.publicKey.bytes, message) }
    }

    @Test
    fun `Mwa Signer signs transaction with correct signature`() = runTest {
        // given
        val signer = MwaSigner(mobileWalletAdapter, sender)
        val programId = SolanaPublicKey.from("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr")
        val blockhash = Blockhash(ByteArray(32))
        val data = "hello world ".encodeToByteArray()

        // when
//        signer.connect()

        println("=== signer = ${signer.publicKey.base58()}")
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

        val result = signer.signTransaction(transaction)

        // then
        assertTrue { result.isSuccess }
        assertTrue {
            Ed25519.verify(result.getOrNull()!!.signatures.first(), signer.publicKey.bytes,
                result.getOrNull()!!.message.serialize())
        }
    }
}