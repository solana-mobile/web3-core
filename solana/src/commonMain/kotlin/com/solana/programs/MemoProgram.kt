package com.solana.programs

import com.solana.publickey.SolanaPublicKey
import com.solana.transaction.AccountMeta
import com.solana.transaction.TransactionInstruction
import kotlin.experimental.ExperimentalObjCName
import kotlin.jvm.JvmStatic
import kotlin.native.ObjCName

@OptIn(ExperimentalObjCName::class)
@ObjCName("MemoProgram")
object MemoProgram {
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