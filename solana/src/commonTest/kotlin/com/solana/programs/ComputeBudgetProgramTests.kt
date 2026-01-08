package com.solana.programs

import com.solana.config.TestConfig
import com.solana.networking.KtorNetworkDriver
import com.solana.publickey.SolanaPublicKey
import com.solana.rpc.Commitment
import com.solana.rpc.SolanaRpcClient
import com.solana.rpc.TransactionOptions
import com.solana.transaction.Message
import com.solana.transaction.Transaction
import diglol.crypto.Ed25519
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ComputeBudgetProgramTests {

    @Test
    fun `set Compute Limit and Price builds valid transaction`() = runTest {
        // given
        val keyPair = Ed25519.generateKeyPair()
        val pubkey = SolanaPublicKey(keyPair.publicKey)
        val rpc = SolanaRpcClient(TestConfig.RPC_URL, KtorNetworkDriver())
        val message = "hello solana!"

        // when
        val airdropResponse = rpc.requestAirdrop(pubkey, 0.1f)
        val blockhashResponse = rpc.getLatestBlockhash()

        val transaction = Message.Builder()
            .setRecentBlockhash(blockhashResponse.result!!.blockhash)
            .addInstruction(MemoProgram.publishMemo(pubkey, message))
            .addInstruction(ComputeBudgetProgram.setComputeUnitLimit(25000u))
            .addInstruction(ComputeBudgetProgram.setComputeUnitPrice(10000u))
            .build().run {
                val sig = Ed25519.sign(keyPair, serialize())
                Transaction(listOf(sig), this)
            }

        val response = withContext(Dispatchers.Default.limitedParallelism(1)) {
            rpc.sendAndConfirmTransaction(
                transaction, TransactionOptions(
                    commitment = Commitment.CONFIRMED,
                    skipPreflight = true
                )
            )
        }

        // then
        assertNull(airdropResponse.error)
        assertNotNull(airdropResponse.result)
        assertNull(response.error)
        assertNotNull(response.result)
    }

    @Test
    fun `request heap frame builds valid transaction`() = runTest {
        // given
        val keyPair = Ed25519.generateKeyPair()
        val pubkey = SolanaPublicKey(keyPair.publicKey)
        val rpc = SolanaRpcClient(TestConfig.RPC_URL, KtorNetworkDriver())
        val message = "hello solana!"

        // when
        val airdropResponse = rpc.requestAirdrop(pubkey, 0.1f)
        val blockhashResponse = rpc.getLatestBlockhash()

        val transaction = Message.Builder()
            .setRecentBlockhash(blockhashResponse.result!!.blockhash)
            .addInstruction(MemoProgram.publishMemo(pubkey, message))
            .addInstruction(ComputeBudgetProgram.requestHeapFrame(40u*1024u))
            .build().run {
                val sig = Ed25519.sign(keyPair, serialize())
                Transaction(listOf(sig), this)
            }

        val response = withContext(Dispatchers.Default.limitedParallelism(1)) {
            rpc.sendAndConfirmTransaction(
                transaction, TransactionOptions(
                    commitment = Commitment.CONFIRMED,
                    skipPreflight = true
                )
            )
        }

        // then
        assertNull(airdropResponse.error)
        assertNotNull(airdropResponse.result)
        assertNull(response.error)
        assertNotNull(response.result)
    }

    @Test
    fun `set Loaded Accounts data size limit builds valid transaction`() = runTest {
        // given
        val keyPair = Ed25519.generateKeyPair()
        val pubkey = SolanaPublicKey(keyPair.publicKey)
        val rpc = SolanaRpcClient(TestConfig.RPC_URL, KtorNetworkDriver())
        val message = "hello solana!"

        // when
        val airdropResponse = rpc.requestAirdrop(pubkey, 0.1f)
        val blockhashResponse = rpc.getLatestBlockhash()

        val transaction = Message.Builder()
            .setRecentBlockhash(blockhashResponse.result!!.blockhash)
            .addInstruction(MemoProgram.publishMemo(pubkey, message))
            .addInstruction(ComputeBudgetProgram.setLoadedAccountsDataSizeLimit(300000u))
            .build().run {
                val sig = Ed25519.sign(keyPair, serialize())
                Transaction(listOf(sig), this)
            }

        val response = withContext(Dispatchers.Default.limitedParallelism(1)) {
            rpc.sendAndConfirmTransaction(
                transaction, TransactionOptions(
                    commitment = Commitment.CONFIRMED,
                    skipPreflight = true
                )
            )
        }

        // then
        assertNull(airdropResponse.error)
        assertNotNull(airdropResponse.result)
        assertNull(response.error)
        assertNotNull(response.result)
    }
}