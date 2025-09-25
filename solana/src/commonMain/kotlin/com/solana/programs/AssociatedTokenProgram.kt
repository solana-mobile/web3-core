package com.solana.programs

import com.solana.publickey.SolanaPublicKey
import com.solana.transaction.AccountMeta
import com.solana.transaction.TransactionInstruction
import kotlin.jvm.JvmStatic

object AssociatedTokenProgram : Program {
    @JvmStatic
    val PROGRAM_ID = SolanaPublicKey.from("ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL")

    @JvmStatic
    fun createAssociatedTokenAccount(
        mint: SolanaPublicKey,
        associatedAccount: SolanaPublicKey,
        owner: SolanaPublicKey,
        payer: SolanaPublicKey,
        associatedProgramId: SolanaPublicKey = PROGRAM_ID,
        programId: SolanaPublicKey = TokenProgram.PROGRAM_ID,
    ): TransactionInstruction =
        TransactionInstruction(
            associatedProgramId,
            listOf(
                AccountMeta(payer, true, true),
                AccountMeta(associatedAccount, false, true),
                AccountMeta(owner, false, false),
                AccountMeta(mint, false, false),
                AccountMeta(SystemProgram.PROGRAM_ID, false, false),
                AccountMeta(programId, false, false),
                AccountMeta(TokenProgram.SYSVAR_RENT_PUBKEY, false, false)
            ),
            data = byteArrayOf()
        )

    override val programId = PROGRAM_ID
}