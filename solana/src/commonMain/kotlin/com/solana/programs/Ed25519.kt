package com.solana.programs

import com.solana.publickey.SolanaPublicKey
import com.solana.transaction.AccountMeta
import com.solana.transaction.TransactionInstruction
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.jvm.JvmStatic

/**
 * Kotlin equivalent of web3.js Ed25519Program (single-signature).
 * Instruction layout:
 * [u8 numSignatures][u8 padding]
 * [u16 signatureOffset][u16 signatureInstructionIndex]
 * [u16 publicKeyOffset][u16 publicKeyInstructionIndex]
 * [u16 messageDataOffset][u16 messageDataSize]
 * [u16 messageInstructionIndex]
 * [32B publicKey][64B signature][message bytes...]
 */
object Ed25519Program : Program {
    @JvmStatic
    val PROGRAM_ID: SolanaPublicKey =
        SolanaPublicKey.from("Ed25519SigVerify111111111111111111111111111")

    override val programId: SolanaPublicKey = PROGRAM_ID

    private const val PRIVATE_KEY_BYTES = 64
    private const val PUBLIC_KEY_BYTES = 32
    private const val SIGNATURE_BYTES = 64
    private const val HEADER_LEN = 16  // ED25519_INSTRUCTION_LAYOUT.span in web3.js

    /**
     * Create an ed25519 instruction with a public key and detached signature.
     *
     * @param publicKey 32-byte ed25519 public key
     * @param message   bytes that were signed
     * @param signature 64-byte ed25519 signature (R||S)
     * @param instructionIndex optional; null -> 0xFFFF (“this instruction”), matching web3.js
     */
    @JvmStatic
    fun createInstructionWithPublicKey(
        publicKey: ByteArray,
        message: ByteArray,
        signature: ByteArray,
        instructionIndex: Int? = null
    ): TransactionInstruction {
        require(publicKey.size == PUBLIC_KEY_BYTES) {
            "Public key must be $PUBLIC_KEY_BYTES bytes but received ${publicKey.size} bytes"
        }
        require(signature.size == SIGNATURE_BYTES) {
            "Signature must be $SIGNATURE_BYTES bytes but received ${signature.size} bytes"
        }
        require(message.size <= 0xFFFF) {
            "Message too long: ${message.size} (max 65535)"
        }

        val publicKeyOffset = HEADER_LEN
        val signatureOffset = publicKeyOffset + PUBLIC_KEY_BYTES         // 16 + 32 = 48
        val messageOffset   = signatureOffset + SIGNATURE_BYTES          // 48 + 64 = 112
        val messageSize     = message.size
        val idxShort        = ((instructionIndex ?: 0xFFFF) and 0xFFFF).toShort()

        // Header (little-endian), field order matches web3.js ED25519_INSTRUCTION_LAYOUT
        val header = ByteBuffer.allocate(HEADER_LEN).order(ByteOrder.LITTLE_ENDIAN).apply {
            put(1)                       // numSignatures
            put(0)                       // padding
            putShort(signatureOffset.toShort())
            putShort(idxShort)           // signatureInstructionIndex
            putShort(publicKeyOffset.toShort())
            putShort(idxShort)           // publicKeyInstructionIndex
            putShort(messageOffset.toShort())
            putShort(messageSize.toShort())
            putShort(idxShort)           // messageInstructionIndex
        }.array()

        // data = header | pubkey | signature | message
        val data = ByteArray(HEADER_LEN + PUBLIC_KEY_BYTES + SIGNATURE_BYTES + messageSize)
        System.arraycopy(header,    0, data, 0,                  HEADER_LEN)
        System.arraycopy(publicKey, 0, data, publicKeyOffset,    PUBLIC_KEY_BYTES)
        System.arraycopy(signature, 0, data, signatureOffset,    SIGNATURE_BYTES)
        System.arraycopy(message,   0, data, messageOffset,      messageSize)

        // NOTE: native program takes no accounts → keys = emptyList()
        return TransactionInstruction(PROGRAM_ID, emptyList<AccountMeta>(), data)
    }
}

