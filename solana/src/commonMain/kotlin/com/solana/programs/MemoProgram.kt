package com.solana.programs

import com.solana.publickey.SolanaPublicKey
import com.solana.transaction.AccountMeta
import com.solana.transaction.TransactionInstruction
import kotlin.jvm.JvmStatic

object MemoProgram : Program {
    @JvmStatic
    val PROGRAM_ID = SolanaPublicKey.from("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr")

    @JvmStatic
    fun publishMemo(account: SolanaPublicKey, memo: String): TransactionInstruction =
        TransactionInstruction(PROGRAM_ID,
            listOf(AccountMeta(account, true, true)),
            memo.encodeToByteArray()
        )

    override val programId = PROGRAM_ID
}