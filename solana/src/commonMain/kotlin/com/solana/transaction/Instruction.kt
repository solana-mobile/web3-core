package com.solana.transaction

import com.solana.publickey.SolanaPublicKey
import kotlinx.serialization.Serializable

@Serializable
data class Instruction(
    val programIdIndex: UByte,
    val accountIndices: ByteArray,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Instruction

        if (programIdIndex != other.programIdIndex) return false
        if (!accountIndices.contentEquals(other.accountIndices)) return false
        return data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = programIdIndex.hashCode()
        result = 31 * result + accountIndices.contentHashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

data class TransactionInstruction(
    val programId: SolanaPublicKey,
    val accounts: List<AccountMeta>,
    val data: ByteArray
)

data class AccountMeta(
    val publicKey: SolanaPublicKey,
    val isSigner: Boolean,
    val isWritable: Boolean,
)