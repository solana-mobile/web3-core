package com.solana.signer

interface Signer {
    val publicKey: ByteArray
    val ownerLength: Number
    val signatureLength: Number
    suspend fun signPayload(payload: ByteArray): ByteArray
}