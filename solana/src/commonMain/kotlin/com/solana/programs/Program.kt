package com.solana.programs

import com.funkatronics.hash.Sha256
import com.solana.publickey.ProgramDerivedAddress
import com.solana.publickey.PublicKey
import com.solana.publickey.SolanaPublicKey
import com.solana.salt.TweetNaclFast
import kotlin.jvm.JvmStatic

interface Program {
    val programId: SolanaPublicKey

    suspend fun createProgramAddress(seeds: List<ByteArray>) =
        createProgramAddress(seeds, programId)

    suspend fun findProgramAddress(seeds: List<ByteArray>) =
        findProgramAddress(seeds, programId)

    companion object {
        @JvmStatic
        suspend fun findProgramAddress(seeds: List<ByteArray>, programId: PublicKey) =
            ProgramDerivedAddress.find(seeds, programId)

        @JvmStatic
        suspend fun createProgramAddress(seeds: List<ByteArray>, programId: PublicKey): Result<SolanaPublicKey> {
            val publicKeyBytes = Sha256.hash(
                seeds.foldIndexed(ByteArray(0)) { i, a, s ->
                    require(s.size <= 32) { "Seed length must be <= 32 bytes" }; a + s
                } + programId.bytes + "ProgramDerivedAddress".encodeToByteArray()
            )

            if (TweetNaclFast.is_on_curve(publicKeyBytes)) {
                return Result.failure(
                    IllegalArgumentException("Invalid seeds, address must fall off curve")
                )
            }

            return Result.success(SolanaPublicKey(publicKeyBytes))
        }
    }
}