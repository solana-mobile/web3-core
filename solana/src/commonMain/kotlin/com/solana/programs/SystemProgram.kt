package com.solana.programs

import com.funkatronics.kborsh.BorshEncoder
import com.solana.publickey.SolanaPublicKey
import com.solana.transaction.AccountMeta
import com.solana.transaction.TransactionInstruction
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlin.jvm.JvmStatic

object SystemProgram {
    @JvmStatic
    val PROGRAM_ID = SolanaPublicKey.from("11111111111111111111111111111111")

    const val PROGRAM_INDEX_CREATE_ACCOUNT = 0
    const val PROGRAM_INDEX_TRANSFER = 2

    @JvmStatic
    fun transfer(
        fromPublicKey: SolanaPublicKey,
        toPublickKey: SolanaPublicKey,
        lamports: Long
    ): TransactionInstruction {
        val keys = ArrayList<AccountMeta>()
        keys.add(AccountMeta(fromPublicKey, true, true))
        keys.add(AccountMeta(toPublickKey, false, true))

        // 4 byte instruction index + 8 bytes lamports
        val data = BorshEncoder()
        data.encodeInt(PROGRAM_INDEX_TRANSFER)
        data.encodeLong(lamports)
        return TransactionInstruction(PROGRAM_ID, keys, data.borshEncodedBytes)
    }

    fun createAccount(
        fromPublicKey: SolanaPublicKey,
        newAccountPublickey: SolanaPublicKey,
        lamports: Long,
        space: Long,
        programId: SolanaPublicKey
    ): TransactionInstruction {
        val keys = ArrayList<AccountMeta>()
        keys.add(AccountMeta(fromPublicKey, true, true))
        keys.add(AccountMeta(newAccountPublickey, true, true))
        val data = BorshEncoder()
        data.encodeInt(PROGRAM_INDEX_CREATE_ACCOUNT)
        data.encodeLong(lamports)
        data.encodeLong(space)
        data.encodeSerializableValue(ByteArraySerializer(), programId.bytes)
        return TransactionInstruction(PROGRAM_ID, keys, data.borshEncodedBytes)
    }
}