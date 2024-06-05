package com.solana.signer.mwa

import com.funkatronics.encoders.Base64
import com.solana.publickey.SolanaPublicKey
import com.solana.transaction.Message
import com.solana.transaction.Transaction
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.ConnectionIdentity
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import com.solana.mobilewalletadapter.clientlib.successPayload
import com.solana.signer.SolanaSigner

private typealias SignedPayloadResult = Pair<ByteArray, ByteArray>
private infix fun ByteArray.withSignature(signature: ByteArray): SignedPayloadResult = Pair(this, signature)
private val SignedPayloadResult.payload get() = first
private val SignedPayloadResult.signature get() = second

class MwaSigner(val walletAdapter: MobileWalletAdapter, val sender: ActivityResultSender) : SolanaSigner() {

    constructor(connectionIdentity: ConnectionIdentity, sender: ActivityResultSender)
            : this(MobileWalletAdapter(connectionIdentity), sender)

    private val placeholderPublicKey = SolanaPublicKey.from("Mobi1eWa11etAdapter111111111111111111111111")
    private var authorizedPublicKey: SolanaPublicKey? = null

    override val publicKey: SolanaPublicKey
        get() = authorizedPublicKey ?: placeholderPublicKey//throw IllegalStateException("No wallet connected, call MwaSigner.connect()")

    suspend fun connect(): TransactionResult<Unit> {
        return walletAdapter.transact(sender) { authResult ->
            authorizedPublicKey = SolanaPublicKey(authResult.accounts.first().publicKey)
        }
    }

    override suspend fun signPayload(payload: ByteArray): Result<ByteArray> =
        signPayloadInternal(payload).map { it.signature }

    override suspend fun signAndSendTransaction(transaction: Transaction): Result<String> =
        when (val result = walletAdapter.transact(sender) { authResult ->
            authorizedPublicKey = SolanaPublicKey(authResult.accounts.first().publicKey)
            signAndSendTransactions(arrayOf(transaction.serialize()))
        }) {
            is TransactionResult.Success ->
                Result.success(Base64.encodeToString(result.payload.signatures.first()))
            is TransactionResult.Failure ->
                Result.failure(SignPayloadException(result.message, result.e))
            is TransactionResult.NoWalletFound ->
                Result.failure(NoWalletFoundException())
        }

    override suspend fun signTransaction(transactionMessage: Message): Result<Transaction> {
        val signers = transactionMessage.accounts.take(transactionMessage.signatureCount.toInt())
        val signerIndex = signers.indexOf(publicKey)
        if (signerIndex == -1) {
            return Result.failure(IllegalArgumentException(
                "Transaction does not require a signature from this public key (${publicKey.base58()})"
            ))
        }

        return signPayloadInternal(transactionMessage.serialize()).map { result ->
            Transaction(MutableList(transactionMessage.signatureCount.toInt()) { ByteArray(ownerLength) }.apply {
                set(signerIndex, result.signature)
            }, Message.from(result.payload))
        }
    }

    class SignPayloadException(message: String, cause: Throwable): Exception(message, cause)
    class NoWalletFoundException: Exception("Cannot sign payload: no wallet found")

    private suspend fun signPayloadInternal(payload: ByteArray): Result<SignedPayloadResult> =
        when (val result = walletAdapter.transact(sender) { authResult ->
            authorizedPublicKey = SolanaPublicKey(authResult.accounts.first().publicKey)
            if (runCatching {
                    Message.from(payload)
                }.isSuccess) {
                // replace the placeholder pubkey with the real one (authorizedPublicKey)
                val newPayload = payload.replace(placeholderPublicKey.bytes, authorizedPublicKey!!.bytes)
                newPayload withSignature signTransactions(arrayOf(newPayload)).signedPayloads.first()
            } else {
                payload withSignature signMessagesDetached(
                    arrayOf(payload),
                    arrayOf(authorizedPublicKey!!.bytes)
                ).messages.first().signatures.first()
            }
        }) {
            is TransactionResult.Success ->
                Result.success(result.successPayload!!)
            is TransactionResult.Failure ->
                Result.failure(SignPayloadException(result.message, result.e))
            is TransactionResult.NoWalletFound ->
                Result.failure(NoWalletFoundException())
        }

    private fun ByteArray.replace(oldBytes: ByteArray, newBytes: ByteArray): ByteArray {
        var pos = 0
        forEachIndexed { index, byte ->
            if (byte == oldBytes[pos]) pos++ else pos = 0

            if (pos == oldBytes.size) {
                newBytes.copyInto(this, index - pos + 1)
                pos = 0
            }
        }
        return this
    }
}