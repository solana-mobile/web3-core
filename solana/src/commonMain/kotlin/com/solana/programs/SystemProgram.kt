package com.solana.programs

import com.funkatronics.kborsh.BorshEncoder
import com.solana.publickey.SolanaPublicKey
import com.solana.transaction.AccountMeta
import com.solana.transaction.TransactionInstruction
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlin.experimental.ExperimentalObjCName
import kotlin.jvm.JvmStatic
import kotlin.native.ObjCName

@OptIn(ExperimentalObjCName::class)
@ObjCName("SystemProgram")
object SystemProgram {
    @JvmStatic
    val PROGRAM_ID = SolanaPublicKey.from("11111111111111111111111111111111")

    private const val PROGRAM_INDEX_CREATE_ACCOUNT = 0
    private const val PROGRAM_INDEX_TRANSFER = 2

    @JvmStatic
    fun transfer(
        fromPublicKey: SolanaPublicKey,
        toPublickKey: SolanaPublicKey,
        lamports: Long
    ): TransactionInstruction =
        TransactionInstruction(PROGRAM_ID,
            listOf(
                AccountMeta(fromPublicKey, true, true),
                AccountMeta(toPublickKey, false, true)
            ),
            BorshEncoder().apply {
                encodeInt(PROGRAM_INDEX_TRANSFER)
                encodeLong(lamports)
            }.borshEncodedBytes
        )

    @JvmStatic
    fun createAccount(
        fromPublicKey: SolanaPublicKey,
        newAccountPublickey: SolanaPublicKey,
        lamports: Long,
        space: Long,
        programId: SolanaPublicKey
    ): TransactionInstruction =
        TransactionInstruction(PROGRAM_ID,
            listOf(
                AccountMeta(fromPublicKey, true, true),
                AccountMeta(newAccountPublickey, true, true)
            ),
            BorshEncoder().apply {
                encodeInt(PROGRAM_INDEX_CREATE_ACCOUNT)
                encodeLong(lamports)
                encodeLong(space)
                encodeSerializableValue(ByteArraySerializer(), programId.bytes)
            }.borshEncodedBytes
        )
}