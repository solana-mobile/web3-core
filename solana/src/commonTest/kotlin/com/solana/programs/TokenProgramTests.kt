package com.solana.programs

import com.solana.config.TestConfig
import com.solana.networking.KtorNetworkDriver
import com.solana.publickey.SolanaPublicKey
import com.solana.rpc.Commitment
import com.solana.rpc.SolanaRpcClient
import com.solana.rpc.TransactionOptions
import com.solana.rpc.getAccountInfo
import com.solana.transaction.Message
import com.solana.transaction.Transaction
import diglol.crypto.Ed25519
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlin.test.*

class TokenProgramTests {

    @Test
    fun `initializeMint successfully initializes a token mint account`() = runTest {
        // given
        val mint = Ed25519.generateKeyPair()
        val mintAuthority = Ed25519.generateKeyPair()
        val mintPublicKey = SolanaPublicKey(mint.publicKey)
        val mintAuthorityPublicKey = SolanaPublicKey(mintAuthority.publicKey)
        val rpc = SolanaRpcClient(TestConfig.RPC_URL, KtorNetworkDriver())

        // when
        rpc.requestAirdrop(mintAuthorityPublicKey, 0.1f)

        val rentExemptBalanceResponse = rpc.getMinBalanceForRentExemption(82)
        val blockhashResponse = rpc.getLatestBlockhash()
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
            rpc.sendAndConfirmTransaction(createAndInitializeMintTransaction, TransactionOptions(
                commitment = Commitment.CONFIRMED,
                skipPreflight = true
            )).apply {
                assertNull(this.error)
                assertNotNull(this.result)
            }
        }

        val accountInfoResponse = rpc.getAccountInfo(mintPublicKey, commitment = Commitment.CONFIRMED)

        assertNull(accountInfoResponse.error)
        assertNotNull(accountInfoResponse.result)
        assertEquals(TokenProgram.PROGRAM_ID, accountInfoResponse.result!!.owner)
        assertEquals(82, accountInfoResponse.result!!.space!!.toInt())
    }

    @Test
    fun `initializeAccount successfully initializes a token account`() = runTest {
        // given
        val mint = Ed25519.generateKeyPair()
        val mintAuthority = Ed25519.generateKeyPair()
        val mintPublicKey = SolanaPublicKey(mint.publicKey)
        val mintAuthorityPublicKey = SolanaPublicKey(mintAuthority.publicKey)
        val tokenAccount = Ed25519.generateKeyPair()
        val tokenOwner = Ed25519.generateKeyPair()
        val tokenAccountPublicKey = SolanaPublicKey(tokenAccount.publicKey)
        val tokenOwnerPublicKey = SolanaPublicKey(tokenOwner.publicKey)
        val rpc = SolanaRpcClient(TestConfig.RPC_URL, KtorNetworkDriver())

        // when
        rpc.requestAirdrop(mintAuthorityPublicKey, 0.1f)

        //  Create Token Mint (Precondition)
        var rentExemptBalanceResponse = rpc.getMinBalanceForRentExemption(82)
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
            rpc.sendAndConfirmTransaction(createAndInitializeMintTransaction, TransactionOptions(
                commitment = Commitment.CONFIRMED,
                skipPreflight = true
            )).apply {
                assertNull(this.error)
                assertNotNull(this.result)
            }
        }

        rpc.requestAirdrop(tokenOwnerPublicKey, 0.1f)

        rentExemptBalanceResponse = rpc.getMinBalanceForRentExemption(165)
        blockhashResponse = rpc.getLatestBlockhash()
        val createAndInitializeAccountTransaction = Message.Builder()
            .setRecentBlockhash(blockhashResponse.result!!.blockhash)
            .addInstruction(SystemProgram.createAccount(
                tokenOwnerPublicKey,
                tokenAccountPublicKey,
                rentExemptBalanceResponse.result!!,
                165L,
                TokenProgram.PROGRAM_ID
            ))
            .addInstruction(TokenProgram.initializeAccount(
                tokenAccountPublicKey,
                mintPublicKey,
                tokenOwnerPublicKey
            ))
            .build().run {
                Transaction(listOf(
                    Ed25519.sign(tokenOwner, serialize()),
                    Ed25519.sign(tokenAccount, serialize())
                ), this)
            }

        withContext(Dispatchers.Default.limitedParallelism(1)) {
            rpc.sendAndConfirmTransaction(createAndInitializeAccountTransaction, TransactionOptions(
                commitment = Commitment.CONFIRMED,
                skipPreflight = true
            )).apply {
                assertNull(this.error)
                assertNotNull(this.result)
            }
        }

        val accountInfoResponse = rpc.getAccountInfo(tokenAccountPublicKey, commitment = Commitment.CONFIRMED)

        assertNull(accountInfoResponse.error)
        assertNotNull(accountInfoResponse.result)
        assertEquals(TokenProgram.PROGRAM_ID, accountInfoResponse.result!!.owner)
        assertEquals(165, accountInfoResponse.result!!.space!!.toInt())
    }

    @Test
    fun `mintTo successfully mints SPL token`() = runTest {
        // given
        val mint = Ed25519.generateKeyPair()
        val mintAuthority = Ed25519.generateKeyPair()
        val mintPublicKey = SolanaPublicKey(mint.publicKey)
        val mintAuthorityPublicKey = SolanaPublicKey(mintAuthority.publicKey)
        val owner = Ed25519.generateKeyPair()
        val ownerTokenAccount = Ed25519.generateKeyPair()
        val ownerPublicKey = SolanaPublicKey(owner.publicKey)
        val ownerTokenAccountPublicKey = SolanaPublicKey(ownerTokenAccount.publicKey)
        val rpc = SolanaRpcClient(TestConfig.RPC_URL, KtorNetworkDriver())

        // when
        rpc.requestAirdrop(mintAuthorityPublicKey, 0.1f)

        //  Create Token Mint (Precondition)
        var rentExemptBalanceResponse = rpc.getMinBalanceForRentExemption(82)
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
                0,
                mintAuthorityPublicKey
            ))
            .build().run {
                Transaction(listOf(
                    Ed25519.sign(mintAuthority, serialize()),
                    Ed25519.sign(mint, serialize())
                ), this)
            }

        withContext(Dispatchers.Default.limitedParallelism(1)) {
            rpc.sendAndConfirmTransaction(createAndInitializeMintTransaction, TransactionOptions(
                commitment = Commitment.CONFIRMED,
                skipPreflight = true
            )).apply {
                assertNull(this.error)
                assertNotNull(this.result)
            }
        }

        // Initialize Owner Account (Precondition)
        rpc.requestAirdrop(ownerPublicKey, 0.1f)

        rentExemptBalanceResponse = rpc.getMinBalanceForRentExemption(165)
        blockhashResponse = rpc.getLatestBlockhash()
        val createAndInitializeSenderAccountTransaction = Message.Builder()
            .setRecentBlockhash(blockhashResponse.result!!.blockhash)
            .addInstruction(SystemProgram.createAccount(
                ownerPublicKey,
                ownerTokenAccountPublicKey,
                rentExemptBalanceResponse.result!!,
                165L,
                TokenProgram.PROGRAM_ID
            ))
            .addInstruction(TokenProgram.initializeAccount(
                ownerTokenAccountPublicKey,
                mintPublicKey,
                ownerPublicKey
            ))
            .build().run {
                Transaction(listOf(
                    Ed25519.sign(owner, serialize()),
                    Ed25519.sign(ownerTokenAccount, serialize())
                ), this)
            }

        withContext(Dispatchers.Default.limitedParallelism(1)) {
            rpc.sendAndConfirmTransaction(createAndInitializeSenderAccountTransaction, TransactionOptions(
                commitment = Commitment.CONFIRMED,
                skipPreflight = true
            )).apply {
                assertNull(this.error)
                assertNotNull(this.result)
            }
        }

        // Mint to Owner Account
        blockhashResponse = rpc.getLatestBlockhash()
        val mintToOwnerAccountTransaction = Message.Builder()
            .setRecentBlockhash(blockhashResponse.result!!.blockhash)
            .addInstruction(TokenProgram.mintTo(
                mintPublicKey,
                ownerTokenAccountPublicKey,
                mintAuthorityPublicKey,
                100
            ))
            .build().run {
                Transaction(listOf(
                    Ed25519.sign(mintAuthority, serialize())
                ), this)
            }

        withContext(Dispatchers.Default.limitedParallelism(1)) {
            rpc.sendAndConfirmTransaction(mintToOwnerAccountTransaction, TransactionOptions(
                commitment = Commitment.CONFIRMED,
                skipPreflight = true
            )).apply {
                assertNull(this.error)
                assertNotNull(this.result)
            }
        }

        // then
        rpc.getAccountInfo(MintAccountInfo.serializer(),
            mintPublicKey,
            commitment = Commitment.CONFIRMED
        ).apply {
            assertNull(error)
            assertNotNull(result)
            result!!.data!!.let { mintAccountInfo ->
                assertEquals(100, mintAccountInfo.supply)
                assertEquals(mintAuthorityPublicKey, mintAccountInfo.mintAuthority)
            }
        }

        rpc.getAccountInfo(TokenAccountInfo.serializer(),
            ownerTokenAccountPublicKey,
            commitment = Commitment.CONFIRMED
        ).apply {
            assertNull(error)
            assertNotNull(result)
            result!!.data!!.let { tokenAccountInfo ->
                assertEquals(100, tokenAccountInfo.amount)
                assertEquals(mintPublicKey, tokenAccountInfo.mint)
            }
        }
    }

    @Test
    fun `mintTo with decimals successfully mints SPL token`() = runTest {
        // given
        val mint = Ed25519.generateKeyPair()
        val mintAuthority = Ed25519.generateKeyPair()
        val mintPublicKey = SolanaPublicKey(mint.publicKey)
        val mintAuthorityPublicKey = SolanaPublicKey(mintAuthority.publicKey)
        val owner = Ed25519.generateKeyPair()
        val ownerTokenAccount = Ed25519.generateKeyPair()
        val ownerPublicKey = SolanaPublicKey(owner.publicKey)
        val ownerTokenAccountPublicKey = SolanaPublicKey(ownerTokenAccount.publicKey)
        val rpc = SolanaRpcClient(TestConfig.RPC_URL, KtorNetworkDriver())

        // when
        rpc.requestAirdrop(mintAuthorityPublicKey, 0.1f)

        //  Create Token Mint (Precondition)
        var rentExemptBalanceResponse = rpc.getMinBalanceForRentExemption(82)
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
                9,
                mintAuthorityPublicKey
            ))
            .build().run {
                Transaction(listOf(
                    Ed25519.sign(mintAuthority, serialize()),
                    Ed25519.sign(mint, serialize())
                ), this)
            }

        withContext(Dispatchers.Default.limitedParallelism(1)) {
            rpc.sendAndConfirmTransaction(createAndInitializeMintTransaction, TransactionOptions(
                commitment = Commitment.CONFIRMED,
                skipPreflight = true
            )).apply {
                assertNull(this.error)
                assertNotNull(this.result)
            }
        }

        // Initialize Owner Account (Precondition)
        rpc.requestAirdrop(ownerPublicKey, 0.1f)

        rentExemptBalanceResponse = rpc.getMinBalanceForRentExemption(165)
        blockhashResponse = rpc.getLatestBlockhash()
        val createAndInitializeSenderAccountTransaction = Message.Builder()
            .setRecentBlockhash(blockhashResponse.result!!.blockhash)
            .addInstruction(SystemProgram.createAccount(
                ownerPublicKey,
                ownerTokenAccountPublicKey,
                rentExemptBalanceResponse.result!!,
                165L,
                TokenProgram.PROGRAM_ID
            ))
            .addInstruction(TokenProgram.initializeAccount(
                ownerTokenAccountPublicKey,
                mintPublicKey,
                ownerPublicKey
            ))
            .build().run {
                Transaction(listOf(
                    Ed25519.sign(owner, serialize()),
                    Ed25519.sign(ownerTokenAccount, serialize())
                ), this)
            }

        withContext(Dispatchers.Default.limitedParallelism(1)) {
            rpc.sendAndConfirmTransaction(createAndInitializeSenderAccountTransaction, TransactionOptions(
                commitment = Commitment.CONFIRMED,
                skipPreflight = true
            )).apply {
                assertNull(this.error)
                assertNotNull(this.result)
            }
        }

        // Mint to Owner Account
        blockhashResponse = rpc.getLatestBlockhash()
        val mintToOwnerAccountTransaction = Message.Builder()
            .setRecentBlockhash(blockhashResponse.result!!.blockhash)
            .addInstruction(TokenProgram.mintTo(
                mintPublicKey,
                ownerTokenAccountPublicKey,
                mintAuthorityPublicKey,
                10_000_000_000
            ))
            .build().run {
                Transaction(listOf(
                    Ed25519.sign(mintAuthority, serialize())
                ), this)
            }

        withContext(Dispatchers.Default.limitedParallelism(1)) {
            rpc.sendAndConfirmTransaction(mintToOwnerAccountTransaction, TransactionOptions(
                commitment = Commitment.CONFIRMED,
                skipPreflight = true
            )).apply {
                assertNull(this.error)
                assertNotNull(this.result)
            }
        }

        // then
        rpc.getAccountInfo(MintAccountInfo.serializer(),
            mintPublicKey,
            commitment = Commitment.CONFIRMED
        ).apply{
            assertNull(error)
            assertNotNull(result)
            result!!.data!!.let { mintAccountInfo ->
                assertEquals(9, mintAccountInfo.decimals)
                assertEquals(10_000_000_000, mintAccountInfo.supply)
                assertEquals(mintAuthorityPublicKey, mintAccountInfo.mintAuthority)
            }
        }

        rpc.getAccountInfo(TokenAccountInfo.serializer(),
            ownerTokenAccountPublicKey,
            commitment = Commitment.CONFIRMED
        ).apply {
            assertNull(error)
            assertNotNull(result)
            result!!.data!!.let { tokenAccountInfo ->
                assertEquals(10_000_000_000, tokenAccountInfo.amount)
                assertEquals(mintPublicKey, tokenAccountInfo.mint)
            }
        }
    }

    @Test
    fun `transfer successfully transfers SPL token`() = runTest {
        // given
        val mint = Ed25519.generateKeyPair()
        val mintAuthority = Ed25519.generateKeyPair()
        val mintPublicKey = SolanaPublicKey(mint.publicKey)
        val mintAuthorityPublicKey = SolanaPublicKey(mintAuthority.publicKey)
        val sender = Ed25519.generateKeyPair()
        val receiver = Ed25519.generateKeyPair()
        val senderTokenAccount = Ed25519.generateKeyPair()
        val receiverTokenAccount = Ed25519.generateKeyPair()
        val senderPublicKey = SolanaPublicKey(sender.publicKey)
        val senderTokenAccountPublicKey = SolanaPublicKey(senderTokenAccount.publicKey)
        val receiverPublicKey = SolanaPublicKey(receiver.publicKey)
        val receiverTokenAccountPublicKey = SolanaPublicKey(receiverTokenAccount.publicKey)
        val rpc = SolanaRpcClient(TestConfig.RPC_URL, KtorNetworkDriver())

        // when
        rpc.requestAirdrop(mintAuthorityPublicKey, 0.1f)

        //  Create Token Mint (Precondition)
        var rentExemptBalanceResponse = rpc.getMinBalanceForRentExemption(82)
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
                0,
                mintAuthorityPublicKey
            ))
            .build().run {
                Transaction(listOf(
                    Ed25519.sign(mintAuthority, serialize()),
                    Ed25519.sign(mint, serialize())
                ), this)
            }

        withContext(Dispatchers.Default.limitedParallelism(1)) {
            rpc.sendAndConfirmTransaction(createAndInitializeMintTransaction, TransactionOptions(
                commitment = Commitment.CONFIRMED,
                skipPreflight = true
            )).apply {
                assertNull(this.error)
                assertNotNull(this.result)
            }
        }

        // Initialize Sender Account (Precondition)
        rpc.requestAirdrop(senderPublicKey, 0.1f)

        rentExemptBalanceResponse = rpc.getMinBalanceForRentExemption(165)
        blockhashResponse = rpc.getLatestBlockhash()
        val createAndInitializeSenderAccountTransaction = Message.Builder()
            .setRecentBlockhash(blockhashResponse.result!!.blockhash)
            .addInstruction(SystemProgram.createAccount(
                senderPublicKey,
                senderTokenAccountPublicKey,
                rentExemptBalanceResponse.result!!,
                165L,
                TokenProgram.PROGRAM_ID
            ))
            .addInstruction(TokenProgram.initializeAccount(
                senderTokenAccountPublicKey,
                mintPublicKey,
                senderPublicKey
            ))
            .build().run {
                Transaction(listOf(
                    Ed25519.sign(sender, serialize()),
                    Ed25519.sign(senderTokenAccount, serialize())
                ), this)
            }

        withContext(Dispatchers.Default.limitedParallelism(1)) {
            rpc.sendAndConfirmTransaction(createAndInitializeSenderAccountTransaction, TransactionOptions(
                commitment = Commitment.CONFIRMED,
                skipPreflight = true
            )).apply {
                assertNull(this.error)
                assertNotNull(this.result)
            }
        }

        // Initialize Receiver Account (Precondition)
        rpc.requestAirdrop(receiverPublicKey, 0.1f)

        rentExemptBalanceResponse = rpc.getMinBalanceForRentExemption(165)
        blockhashResponse = rpc.getLatestBlockhash()
        val createAndInitializeReceiverAccountTransaction = Message.Builder()
            .setRecentBlockhash(blockhashResponse.result!!.blockhash)
            .addInstruction(SystemProgram.createAccount(
                receiverPublicKey,
                receiverTokenAccountPublicKey,
                rentExemptBalanceResponse.result!!,
                165L,
                TokenProgram.PROGRAM_ID
            ))
            .addInstruction(TokenProgram.initializeAccount(
                receiverTokenAccountPublicKey,
                mintPublicKey,
                receiverPublicKey
            ))
            .build().run {
                Transaction(listOf(
                    Ed25519.sign(receiver, serialize()),
                    Ed25519.sign(receiverTokenAccount, serialize())
                ), this)
            }

        withContext(Dispatchers.Default.limitedParallelism(1)) {
            rpc.sendAndConfirmTransaction(createAndInitializeReceiverAccountTransaction, TransactionOptions(
                commitment = Commitment.CONFIRMED,
                skipPreflight = true
            )).apply {
                assertNull(this.error)
                assertNotNull(this.result)
            }
        }

        // Mint to Sender Account (Precondition)
        blockhashResponse = rpc.getLatestBlockhash()
        val mintToSenderAccountTransaction = Message.Builder()
            .setRecentBlockhash(blockhashResponse.result!!.blockhash)
            .addInstruction(TokenProgram.mintTo(
                mintPublicKey,
                senderTokenAccountPublicKey,
                mintAuthorityPublicKey,
                100
            ))
            .build().run {
                Transaction(listOf(
                    Ed25519.sign(mintAuthority, serialize())
                ), this)
            }

        withContext(Dispatchers.Default.limitedParallelism(1)) {
            rpc.sendAndConfirmTransaction(mintToSenderAccountTransaction, TransactionOptions(
                commitment = Commitment.CONFIRMED,
                skipPreflight = true
            )).apply {
                assertNull(this.error)
                assertNotNull(this.result)
            }
        }

        // Transfer
        blockhashResponse = rpc.getLatestBlockhash()
        val transferTransaction = Message.Builder()
            .setRecentBlockhash(blockhashResponse.result!!.blockhash)
            .addInstruction(TokenProgram.transfer(
                senderTokenAccountPublicKey,
                receiverTokenAccountPublicKey,
                10,
                senderPublicKey
            ))
            .build().run {
                Transaction(listOf(
                    Ed25519.sign(sender, serialize())
                ), this)
            }

        withContext(Dispatchers.Default.limitedParallelism(1)) {
            rpc.sendAndConfirmTransaction(transferTransaction, TransactionOptions(
                commitment = Commitment.CONFIRMED,
                skipPreflight = true
            )).apply {
                assertNull(this.error)
                assertNotNull(this.result)
            }
        }

        // then
        rpc.getAccountInfo(TokenAccountInfo.serializer(), receiverTokenAccountPublicKey, commitment = Commitment.CONFIRMED).apply {
            assertNull(error)
            assertNotNull(result)
            result!!.data!!.let { tokenAccountInfo ->
                assertEquals(10, tokenAccountInfo.amount)
                assertEquals(mintPublicKey, tokenAccountInfo.mint)
            }
        }

        rpc.getAccountInfo(TokenAccountInfo.serializer(), senderTokenAccountPublicKey, commitment = Commitment.CONFIRMED).apply {
            assertNull(error)
            assertNotNull(result)
            result!!.data!!.let { tokenAccountInfo ->
                assertEquals(90, tokenAccountInfo.amount)
                assertEquals(mintPublicKey, tokenAccountInfo.mint)
            }
        }

        rpc.getAccountInfo(MintAccountInfo.serializer(), mintPublicKey, commitment = Commitment.CONFIRMED).apply{
            assertNull(error)
            assertNotNull(result)
            result!!.data!!.let { mintAccountInfo ->
                assertEquals(0, mintAccountInfo.decimals)
                assertEquals(100, mintAccountInfo.supply)
                assertEquals(mintAuthorityPublicKey, mintAccountInfo.mintAuthority)
            }
        }
    }

    @Test
    fun `transferChecked successfully transfers SPL token`() = runTest {
        // given
        val mint = Ed25519.generateKeyPair()
        val mintAuthority = Ed25519.generateKeyPair()
        val mintPublicKey = SolanaPublicKey(mint.publicKey)
        val mintAuthorityPublicKey = SolanaPublicKey(mintAuthority.publicKey)
        val sender = Ed25519.generateKeyPair()
        val receiver = Ed25519.generateKeyPair()
        val senderTokenAccount = Ed25519.generateKeyPair()
        val receiverTokenAccount = Ed25519.generateKeyPair()
        val senderPublicKey = SolanaPublicKey(sender.publicKey)
        val senderTokenAccountPublicKey = SolanaPublicKey(senderTokenAccount.publicKey)
        val receiverPublicKey = SolanaPublicKey(receiver.publicKey)
        val receiverTokenAccountPublicKey = SolanaPublicKey(receiverTokenAccount.publicKey)
        val rpc = SolanaRpcClient(TestConfig.RPC_URL, KtorNetworkDriver())

        // when
        rpc.requestAirdrop(mintAuthorityPublicKey, 0.1f)

        //  Create Token Mint (Precondition)
        var rentExemptBalanceResponse = rpc.getMinBalanceForRentExemption(82)
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
                6,
                mintAuthorityPublicKey
            ))
            .build().run {
                Transaction(listOf(
                    Ed25519.sign(mintAuthority, serialize()),
                    Ed25519.sign(mint, serialize())
                ), this)
            }

        withContext(Dispatchers.Default.limitedParallelism(1)) {
            rpc.sendAndConfirmTransaction(createAndInitializeMintTransaction, TransactionOptions(
                commitment = Commitment.CONFIRMED,
                skipPreflight = true
            )).apply {
                assertNull(this.error)
                assertNotNull(this.result)
            }
        }

        // Initialize Sender Account (Precondition)
        rpc.requestAirdrop(senderPublicKey, 0.1f)

        rentExemptBalanceResponse = rpc.getMinBalanceForRentExemption(165)
        blockhashResponse = rpc.getLatestBlockhash()
        val createAndInitializeSenderAccountTransaction = Message.Builder()
            .setRecentBlockhash(blockhashResponse.result!!.blockhash)
            .addInstruction(SystemProgram.createAccount(
                senderPublicKey,
                senderTokenAccountPublicKey,
                rentExemptBalanceResponse.result!!,
                165L,
                TokenProgram.PROGRAM_ID
            ))
            .addInstruction(TokenProgram.initializeAccount(
                senderTokenAccountPublicKey,
                mintPublicKey,
                senderPublicKey
            ))
            .build().run {
                Transaction(listOf(
                    Ed25519.sign(sender, serialize()),
                    Ed25519.sign(senderTokenAccount, serialize())
                ), this)
            }

        withContext(Dispatchers.Default.limitedParallelism(1)) {
            rpc.sendAndConfirmTransaction(createAndInitializeSenderAccountTransaction, TransactionOptions(
                commitment = Commitment.CONFIRMED,
                skipPreflight = true
            )).apply {
                assertNull(this.error)
                assertNotNull(this.result)
            }
        }

        // Initialize Receiver Account (Precondition)
        rpc.requestAirdrop(receiverPublicKey, 0.1f)

        rentExemptBalanceResponse = rpc.getMinBalanceForRentExemption(165)
        blockhashResponse = rpc.getLatestBlockhash()
        val createAndInitializeReceiverAccountTransaction = Message.Builder()
            .setRecentBlockhash(blockhashResponse.result!!.blockhash)
            .addInstruction(SystemProgram.createAccount(
                receiverPublicKey,
                receiverTokenAccountPublicKey,
                rentExemptBalanceResponse.result!!,
                165L,
                TokenProgram.PROGRAM_ID
            ))
            .addInstruction(TokenProgram.initializeAccount(
                receiverTokenAccountPublicKey,
                mintPublicKey,
                receiverPublicKey
            ))
            .build().run {
                Transaction(listOf(
                    Ed25519.sign(receiver, serialize()),
                    Ed25519.sign(receiverTokenAccount, serialize())
                ), this)
            }

        withContext(Dispatchers.Default.limitedParallelism(1)) {
            rpc.sendAndConfirmTransaction(createAndInitializeReceiverAccountTransaction, TransactionOptions(
                commitment = Commitment.CONFIRMED,
                skipPreflight = true
            )).apply {
                assertNull(this.error)
                assertNotNull(this.result)
            }
        }

        // Mint to Sender Account (Precondition)
        blockhashResponse = rpc.getLatestBlockhash()
        val mintToSenderAccountTransaction = Message.Builder()
            .setRecentBlockhash(blockhashResponse.result!!.blockhash)
            .addInstruction(TokenProgram.mintTo(
                mintPublicKey,
                senderTokenAccountPublicKey,
                mintAuthorityPublicKey,
                100_000_000
            ))
            .build().run {
                Transaction(listOf(
                    Ed25519.sign(mintAuthority, serialize())
                ), this)
            }

        withContext(Dispatchers.Default.limitedParallelism(1)) {
            rpc.sendAndConfirmTransaction(mintToSenderAccountTransaction, TransactionOptions(
                commitment = Commitment.CONFIRMED,
                skipPreflight = true
            )).apply {
                assertNull(this.error)
                assertNotNull(this.result)
            }
        }

        // Transfer
        blockhashResponse = rpc.getLatestBlockhash()
        val transferTransaction = Message.Builder()
            .setRecentBlockhash(blockhashResponse.result!!.blockhash)
            .addInstruction(TokenProgram.transferChecked(
                senderTokenAccountPublicKey,
                receiverTokenAccountPublicKey,
                10_000_000,
                6,
                senderPublicKey,
                mintPublicKey
            ))
            .build().run {
                Transaction(listOf(
                    Ed25519.sign(sender, serialize())
                ), this)
            }

        withContext(Dispatchers.Default.limitedParallelism(1)) {
            rpc.sendAndConfirmTransaction(transferTransaction, TransactionOptions(
                commitment = Commitment.CONFIRMED,
                skipPreflight = true
            )).apply {
                assertNull(this.error)
                assertNotNull(this.result)
            }
        }

        // then
        rpc.getAccountInfo(TokenAccountInfo.serializer(),
            receiverTokenAccountPublicKey,
            commitment = Commitment.CONFIRMED
        ).apply {
            assertNull(error)
            assertNotNull(result)
            result!!.data!!.let { tokenAccountInfo ->
                assertEquals(10_000_000, tokenAccountInfo.amount)
                assertEquals(mintPublicKey, tokenAccountInfo.mint)
            }
        }

        rpc.getAccountInfo(TokenAccountInfo.serializer(),
            senderTokenAccountPublicKey,
            commitment = Commitment.CONFIRMED
        ).apply {
            assertNull(error)
            assertNotNull(result)
            result!!.data!!.let { tokenAccountInfo ->
                assertEquals(90_000_000, tokenAccountInfo.amount)
                assertEquals(mintPublicKey, tokenAccountInfo.mint)
            }
        }

        rpc.getAccountInfo(MintAccountInfo.serializer(),
            mintPublicKey,
            commitment = Commitment.CONFIRMED
        ).apply{
            assertNull(error)
            assertNotNull(result)
            result!!.data!!.let { mintAccountInfo ->
                assertEquals(6, mintAccountInfo.decimals)
                assertEquals(100_000_000, mintAccountInfo.supply)
                assertEquals(mintAuthorityPublicKey, mintAccountInfo.mintAuthority)
            }
        }
    }

    @Test
    fun `closeAccount successfully closes a token account`() = runTest {
        // given
        val mint = Ed25519.generateKeyPair()
        val mintAuthority = Ed25519.generateKeyPair()
        val mintPublicKey = SolanaPublicKey(mint.publicKey)
        val mintAuthorityPublicKey = SolanaPublicKey(mintAuthority.publicKey)
        val tokenAccount = Ed25519.generateKeyPair()
        val tokenOwner = Ed25519.generateKeyPair()
        val tokenAccountPublicKey = SolanaPublicKey(tokenAccount.publicKey)
        val tokenOwnerPublicKey = SolanaPublicKey(tokenOwner.publicKey)
        val rpc = SolanaRpcClient(TestConfig.RPC_URL, KtorNetworkDriver())

        // when
        rpc.requestAirdrop(mintAuthorityPublicKey, 0.1f)

        //  Create Token Mint (Precondition)
        var rentExemptBalanceResponse = rpc.getMinBalanceForRentExemption(82)
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
            rpc.sendAndConfirmTransaction(createAndInitializeMintTransaction, TransactionOptions(
                commitment = Commitment.CONFIRMED,
                skipPreflight = true
            )).apply {
                assertNull(this.error)
                assertNotNull(this.result)
            }
        }

        // Initialize Owner Account (Precondition)
        rpc.requestAirdrop(tokenOwnerPublicKey, 0.1f)

        rentExemptBalanceResponse = rpc.getMinBalanceForRentExemption(165)
        blockhashResponse = rpc.getLatestBlockhash()
        val createAndInitializeAccountTransaction = Message.Builder()
            .setRecentBlockhash(blockhashResponse.result!!.blockhash)
            .addInstruction(SystemProgram.createAccount(
                tokenOwnerPublicKey,
                tokenAccountPublicKey,
                rentExemptBalanceResponse.result!!,
                165L,
                TokenProgram.PROGRAM_ID
            ))
            .addInstruction(TokenProgram.initializeAccount(
                tokenAccountPublicKey,
                mintPublicKey,
                tokenOwnerPublicKey
            ))
            .build().run {
                Transaction(listOf(
                    Ed25519.sign(tokenOwner, serialize()),
                    Ed25519.sign(tokenAccount, serialize())
                ), this)
            }

        withContext(Dispatchers.Default.limitedParallelism(1)) {
            rpc.sendAndConfirmTransaction(createAndInitializeAccountTransaction, TransactionOptions(
                commitment = Commitment.CONFIRMED,
                skipPreflight = true
            )).apply {
                assertNull(this.error)
                assertNotNull(this.result)
            }
        }

        rpc.getAccountInfo(tokenAccountPublicKey, commitment = Commitment.CONFIRMED).apply {
            assertNull(error)
            assertNotNull(result)
            assertEquals(TokenProgram.PROGRAM_ID, result!!.owner)
            assertEquals(165, result!!.space!!.toInt())
        }

        // Close Account
        blockhashResponse = rpc.getLatestBlockhash()
        val closeAccountTransaction = Message.Builder()
            .setRecentBlockhash(blockhashResponse.result!!.blockhash)
            .addInstruction(TokenProgram.closeAccount(
                tokenAccountPublicKey,
                tokenOwnerPublicKey,
                tokenOwnerPublicKey
            ))
            .build().run {
                Transaction(listOf(
                    Ed25519.sign(tokenOwner, serialize())
                ), this)
            }

        withContext(Dispatchers.Default.limitedParallelism(1)) {
            rpc.sendAndConfirmTransaction(closeAccountTransaction, TransactionOptions(
                commitment = Commitment.CONFIRMED,
                skipPreflight = true
            )).apply {
                assertNull(this.error)
                assertNotNull(this.result)
            }
        }

        // then
        rpc.getAccountInfo(tokenAccountPublicKey, commitment = Commitment.CONFIRMED).apply {
            assertNull(error)
            assertNull(result)
        }
    }

    @Serializable
    data class MintAccountInfo(
        val mintAuthorityOption: Int,
        val mintAuthority: SolanaPublicKey,
        val supply: Long,
        val decimals: Byte,
        val isInitialized: Boolean,
        val freezeAuthorityOption: Int,
        val freezeAuthority: SolanaPublicKey
    )

    @Serializable
    data class TokenAccountInfo(
        val mint: SolanaPublicKey,
        val owner: SolanaPublicKey,
        val amount: Long,
        val delegateOption: Int,
        val delegate: SolanaPublicKey,
        val state: Byte,
        val isNativeOption: Int,
        val isNative: Long,
        val delegatedAmount: Long,
        val closeAuthorityOption: Int,
        val closeAuthority: SolanaPublicKey
    )
}