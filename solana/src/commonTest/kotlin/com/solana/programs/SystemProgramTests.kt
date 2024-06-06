package com.solana.programs

import com.solana.config.TestConfig
import com.solana.publickey.SolanaPublicKey
import com.solana.transaction.Message
import com.solana.transaction.Transaction
import com.solana.util.RpcClient
import diglol.crypto.Ed25519
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SystemProgramTests {

    @Test
    fun `createAccount successfully creates account`() = runTest {
        // given
        val payerKeyPair = Ed25519.generateKeyPair()
        val newAccountKeyPair = Ed25519.generateKeyPair()
        val payerPubkey = SolanaPublicKey(payerKeyPair.publicKey)
        val newAccountPubkey = SolanaPublicKey(newAccountKeyPair.publicKey)
        val rpc = RpcClient(TestConfig.RPC_URL)

        // when
        val airdropResponse = rpc.requestAirdrop(payerPubkey, 0.1f)

        val rentExemptBalanceResponse = rpc.getMinBalanceForRentExemption(0)

        val blockhashResponse = rpc.getLatestBlockhash()

        val transaction = Message.Builder()
            .setRecentBlockhash(blockhashResponse.result!!.blockhash)
            .addInstruction(SystemProgram.createAccount(payerPubkey,
                newAccountPubkey,
                rentExemptBalanceResponse.result!!,
                0L,
                SystemProgram.PROGRAM_ID
            ))
            .build().run {
                val payerSig = Ed25519.sign(payerKeyPair, serialize())
                val newAccountSig = Ed25519.sign(newAccountKeyPair, serialize())
                Transaction(listOf(payerSig, newAccountSig), this)
            }

        rpc.sendTransaction(transaction)

        // TODO: add transaction confirmation
        runBlocking {
            delay(300)
        }

        val response = rpc.getBalance(newAccountPubkey)

        // then
        assertNull(airdropResponse.error)
        assertNotNull(airdropResponse.result)
        assertNull(response.error)
        assertNotNull(response.result)
        assertEquals(rentExemptBalanceResponse.result!!, response.result)
    }

    @Test
    fun `transfer successfully transfers funds`() = runTest {
        // given
        val payerKeyPair = Ed25519.generateKeyPair()
        val receiverKeyPair = Ed25519.generateKeyPair()
        val payerPubkey = SolanaPublicKey(payerKeyPair.publicKey)
        val receiverPubkey = SolanaPublicKey(receiverKeyPair.publicKey)
        val rpc = RpcClient(TestConfig.RPC_URL)
        val balance = 10000000L // lamports

        // when
        val airdropResponse = rpc.requestAirdrop(payerPubkey, 0.1f)
        val blockhashResponse = rpc.getLatestBlockhash()

        val transaction = Message.Builder()
            .setRecentBlockhash(blockhashResponse.result!!.blockhash)
            .addInstruction(SystemProgram.transfer(payerPubkey, receiverPubkey, balance))
            .build().run {
                val sig = Ed25519.sign(payerKeyPair, serialize())
                Transaction(listOf(sig), this)
            }

        rpc.sendTransaction(transaction)

        // TODO: add transaction confirmation
        runBlocking {
            delay(300)
        }

        val response = rpc.getBalance(receiverPubkey)

        // then
        assertNull(airdropResponse.error)
        assertNotNull(airdropResponse.result)
        assertNull(response.error)
        assertNotNull(response.result)
        assertEquals(balance, response.result!!)
    }
}