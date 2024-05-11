package com.solana.signer

import com.solana.publickey.SolanaPublicKey
import com.solana.transaction.Message
import com.solana.transaction.Transaction

abstract class SolanaSigner : Ed25519Signer() {
    abstract suspend fun signAndSendTransaction(transaction: Transaction): Result<String>

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

suspend fun SolanaSigner.signTransaction(transaction: Transaction): Transaction =
    transaction.message.run {
        val signers = accounts.take(transaction.message.signatureCount.toInt())
        val signerIndex = signers.indexOf(publicKey)
        require(signerIndex != -1) {
            "Transaction does not require a signature from this public key (${publicKey.base58()})"
        }

        val signature = signMessage(this)
        Transaction(transaction.signatures.toMutableList().apply {
            set(signerIndex, signature)
        }, this)
    }

suspend fun SolanaSigner.signMessage(message: Message): ByteArray {
    return signPayload(message.serialize())
}
