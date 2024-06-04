package com.solana.signer

import com.solana.publickey.SolanaPublicKey
import com.solana.transaction.Message
import com.solana.transaction.Transaction

abstract class SolanaSigner : Ed25519Signer() {
    abstract override val publicKey: SolanaPublicKey
    abstract suspend fun signAndSendTransaction(transaction: Transaction): Result<String>

    open suspend fun signTransaction(transaction: Transaction): Result<Transaction> =
        signTransaction(transaction.message)

    open suspend fun signTransaction(transactionMessage: Message): Result<Transaction> {
        val signers = transactionMessage.accounts.take(transactionMessage.signatureCount.toInt())
        val signerIndex = signers.indexOf(publicKey)
        if (signerIndex == -1) {
            return Result.failure(IllegalArgumentException(
                "Transaction does not require a signature from this public key (${publicKey.base58()})"
            ))
        }

        return signPayload(transactionMessage.serialize()).map { signature ->
            Transaction(MutableList(transactionMessage.signatureCount.toInt()) { ByteArray(ownerLength) }.apply {
                set(signerIndex, signature)
            }, transactionMessage)
        }
    }

    suspend fun signOffChainMessage(message: ByteArray): Result<ByteArray> {
        runCatching { Message.from(message) }.onSuccess {
            return Result.failure(IllegalArgumentException("Attempting to sign a transaction as off chain message"))
        }
        return signPayload(message)
    }

    @Deprecated(
        "signMessage is deprecated, use the asynchronous signPayload method or an equivalent extension function",
        level = DeprecationLevel.ERROR
    )
    open fun signMessage(message: ByteArray): ByteArray {
        throw NotImplementedError("signMessage is deprecated, use the asynchronous signPayload method or an equivalent extension function")
    }

    @Deprecated(
        "signTransaction is deprecated, use the asynchronous signPayload method or an equivalent extension function",
        level = DeprecationLevel.ERROR
    )
    open fun signTransaction(transaction: ByteArray): ByteArray {
        throw NotImplementedError("signTransaction is deprecated, use the asynchronous signPayload method or an equivalent extension function")
    }
}


