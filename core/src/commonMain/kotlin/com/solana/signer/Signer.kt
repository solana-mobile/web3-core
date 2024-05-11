package com.solana.signer

import com.solana.publickey.PublicKey

interface Signer {
    val publicKey: PublicKey
    val ownerLength: Number
    val signatureLength: Number
    suspend fun signPayload(payload: ByteArray): ByteArray
}
