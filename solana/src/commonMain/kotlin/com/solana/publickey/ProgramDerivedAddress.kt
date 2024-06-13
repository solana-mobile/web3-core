package com.solana.publickey

import com.solana.programs.Program
import kotlin.jvm.JvmStatic

class ProgramDerivedAddress(bytes: ByteArray, val nonce: UByte) : SolanaPublicKey(bytes) {

    constructor(publicKey: PublicKey, nonce: UByte) : this(publicKey.bytes, nonce)

    companion object {
        @JvmStatic
        suspend fun find(seeds: List<ByteArray>, programId: PublicKey): Result<ProgramDerivedAddress> {
            for (bump in 255 downTo 0) {
                val result = Program.createProgramAddress(seeds + byteArrayOf(bump.toByte()), programId)
                if (result.isSuccess) return result.map { ProgramDerivedAddress(it, bump.toUByte()) }
            }
            return Result.failure(Error("Unable to find valid derived address for provided seeds"))
        }
    }
}