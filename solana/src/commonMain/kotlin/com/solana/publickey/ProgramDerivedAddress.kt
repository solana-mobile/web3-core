package com.solana.publickey

import com.funkatronics.salt.isOnCurve
import com.solana.programs.Program
import kotlin.jvm.JvmStatic

class ProgramDerivedAddress private constructor(bytes: ByteArray, val nonce: UByte) : SolanaPublicKey(bytes) {

    private constructor(publicKey: PublicKey, nonce: UByte) : this(publicKey.bytes, nonce)

    companion object {
        @JvmStatic
        suspend fun find(seeds: List<ByteArray>, programId: PublicKey): Result<ProgramDerivedAddress> {
            for (bump in 255 downTo 0) {
                println(bump)
                val result = Program.createDerivedAddress(seeds + byteArrayOf(bump.toByte()), programId)
                if (result.isSuccess) return result.map { ProgramDerivedAddress(it, bump.toUByte()) }
            }
            return Result.failure(Error("Unable to find valid derived address for provided seeds"))
        }

        @JvmStatic
        suspend fun create(bytes: ByteArray, nonce: UByte): ProgramDerivedAddress {
            require(!bytes.isOnCurve()) { "Provided public key is not a PDA, address must be off Ed25519 curve" }
            return ProgramDerivedAddress(bytes, nonce)
        }

        @JvmStatic
        suspend fun create(publicKey: PublicKey, nonce: UByte) =
            ProgramDerivedAddress(publicKey.bytes, nonce)
    }
}