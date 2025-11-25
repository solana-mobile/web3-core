package com.solana.publickey

import com.funkatronics.salt.isOnCurve
import com.solana.programs.AssociatedTokenProgram
import com.solana.programs.TokenProgram

suspend fun PublicKey.isOnCurve() = bytes.isOnCurve()

suspend fun SolanaPublicKey.findPda(vararg seeds: ByteArray) =
    ProgramDerivedAddress.find(seeds.toList(), this).getOrElse {
        throw Error(
            "Unable to find derived address for program ${this.address} " +
                    "with seeds [${seeds.joinToString { it.contentToString() }}]"
        )
    }

suspend fun SolanaPublicKey.findPda(seed: String) = findPda(seed.encodeToByteArray())

suspend fun SolanaPublicKey.ata(mint: SolanaPublicKey) =
    ProgramDerivedAddress.find(listOf(
        this.bytes,
        TokenProgram.PROGRAM_ID.bytes,
        mint.bytes), AssociatedTokenProgram.PROGRAM_ID
    ).getOrElse {
        throw Error("Unable to find ATA for user ${this.address} with mint ${mint.address}]")
    }