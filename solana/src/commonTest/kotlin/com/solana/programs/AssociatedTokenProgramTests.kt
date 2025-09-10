package com.solana.programs

import com.solana.config.TestConfig
import com.solana.publickey.ProgramDerivedAddress
import com.solana.publickey.SolanaPublicKey
import com.solana.transaction.Message
import com.solana.transaction.Transaction
import com.solana.util.RpcClient
import diglol.crypto.Ed25519
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AssociatedTokenProgramTests {

    @Test
    fun `createAssociatedTokenAccount successfully creates ATA`() = runTest {
        // given
        val mint = Ed25519.generateKeyPair()
        val mintAuthority = Ed25519.generateKeyPair()
        val mintPublicKey = SolanaPublicKey(mint.publicKey)
        val mintAuthorityPublicKey = SolanaPublicKey(mintAuthority.publicKey)
        val owner = Ed25519.generateKeyPair()
        val ownerPublicKey = SolanaPublicKey(owner.publicKey)
        val rpc = RpcClient(TestConfig.RPC_URL)

        val associatedAccount = SolanaPublicKey(ProgramDerivedAddress.find(
            listOf(ownerPublicKey.bytes, TokenProgram.PROGRAM_ID.bytes, mintPublicKey.bytes),
            AssociatedTokenProgram.PROGRAM_ID
        ).getOrThrow().bytes)

        // when
        rpc.requestAirdrop(ownerPublicKey, 0.1f)
        rpc.requestAirdrop(mintAuthorityPublicKey, 0.1f)

        val rentExemptBalanceResponse = rpc.getMinBalanceForRentExemption(82)
        var blockhashResponse = rpc.getLatestBlockhash()
        val createAndInitializeMintTransaction = Message.Builder()
            .setRecentBlockhash(blockhashResponse.result!!.blockhash)
            .addInstruction(SystemProgram.createAccount(
                mintAuthorityPublicKey,
                mintPublicKey,
                rentExemptBalanceResponse.result!!,
                82L,
                TokenProgram.PROGRAM_ID
            ))
            .addInstruction(TokenProgram.initializeMint(
                mintPublicKey,
                10,
                mintAuthorityPublicKey
            ))
            .build().run {
                Transaction(listOf(
                    Ed25519.sign(mintAuthority, serialize()),
                    Ed25519.sign(mint, serialize())
                ), this)
            }

        withContext(Dispatchers.Default.limitedParallelism(1)) {
            rpc.sendAndConfirmTransaction(createAndInitializeMintTransaction)
        }

        blockhashResponse = rpc.getLatestBlockhash()
        val transaction = Message.Builder()
            .setRecentBlockhash(blockhashResponse.result!!.blockhash)
            .addInstruction(AssociatedTokenProgram.createAssociatedTokenAccount(
                mintPublicKey,
                associatedAccount,
                ownerPublicKey,
                ownerPublicKey
            ))
            .build().run {
                Transaction(listOf(
                    Ed25519.sign(owner, serialize())
                ), this)
            }

        withContext(Dispatchers.Default.limitedParallelism(1)) {
            rpc.sendAndConfirmTransaction(transaction).apply {
                assertNull(this.error)
                assertNotNull(this.result)
            }
        }

        val accountInfo = rpc.getAccountInfo(associatedAccount)

        assertNull(accountInfo.error)
        assertNotNull(accountInfo.result)
        assertEquals(TokenProgram.programId, accountInfo.result!!.owner)
        assertEquals(165, accountInfo.result!!.space)
    }
}