package com.solana.programs

import com.solana.config.TestConfig
import com.solana.publickey.SolanaPublicKey
import com.solana.transaction.Message
import com.solana.transaction.Transaction
import com.solana.util.RpcClient
import diglol.crypto.Ed25519
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MemoProgramTests {

    @Test
    fun `publishMemo builds valid transaction`() = runTest {
        // given
        val keyPair = Ed25519.generateKeyPair()
        val pubkey = SolanaPublicKey(keyPair.publicKey)
        val rpc = RpcClient(TestConfig.RPC_URL)
        val message = "hello solana!"

        // when
        val airdropResponse = rpc.requestAirdrop(pubkey, 0.1f)
        val blockhashResponse = rpc.getLatestBlockhash()

        val transaction = Message.Builder()
            .setRecentBlockhash(blockhashResponse.result!!.blockhash)
            .addInstruction(MemoProgram.publishMemo(pubkey, message))
            .build().run {
                val sig = Ed25519.sign(keyPair, serialize())
                Transaction(listOf(sig), this)
            }

        val response = rpc.sendTransaction(transaction)

        // then
        assertNull(airdropResponse.error)
        assertNotNull(airdropResponse.result)
        assertNull(response.error)
        assertNotNull(response.result)
    }
}