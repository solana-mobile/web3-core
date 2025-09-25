package com.solana.programs

import com.funkatronics.kborsh.BorshEncoder
import com.solana.publickey.SolanaPublicKey
import com.solana.publickey.SolanaPublicKeySerializer
import com.solana.transaction.AccountMeta
import com.solana.transaction.TransactionInstruction
import kotlin.jvm.JvmStatic

object TokenProgram : Program {
    @JvmStatic
    val PROGRAM_ID = SolanaPublicKey.from("TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA")
    @JvmStatic
    val SYSVAR_RENT_PUBKEY = SolanaPublicKey.from("SysvarRent111111111111111111111111111111111")

    private const val PROGRAM_INDEX_INIT_MINT = 0.toByte()
    private const val PROGRAM_INDEX_INIT_ACCOUNT = 1.toByte()
    private const val PROGRAM_INDEX_TRANSFER = 3.toByte()
    private const val PROGRAM_INDEX_MINT_TO = 7.toByte()
    private const val PROGRAM_INDEX_CLOSE_ACCOUNT = 9.toByte()
    private const val PROGRAM_INDEX_TRANSFER_CHECKED = 12.toByte()

    @JvmStatic
    fun initializeMint(
        mintAccount: SolanaPublicKey,
        decimals: Byte,
        mintAuthority: SolanaPublicKey,
        freezeAuthority: SolanaPublicKey? = null
    ): TransactionInstruction =
        TransactionInstruction(
            PROGRAM_ID,
            listOf(
                AccountMeta(mintAccount, false, true),
                AccountMeta(SYSVAR_RENT_PUBKEY, false, false)
            ),
            BorshEncoder().apply {
                encodeByte(PROGRAM_INDEX_INIT_MINT)
                encodeByte(decimals)
                encodeSerializableValue(SolanaPublicKeySerializer, mintAuthority)
                encodeByte(if (freezeAuthority != null) 1 else 0)
                encodeSerializableValue(SolanaPublicKeySerializer, freezeAuthority ?: SolanaPublicKey(ByteArray(SolanaPublicKey.PUBLIC_KEY_LENGTH)))
            }.borshEncodedBytes
        )

    @JvmStatic
    fun initializeAccount(
        account: SolanaPublicKey,
        mintAccount: SolanaPublicKey,
        owner: SolanaPublicKey,
    ): TransactionInstruction =
        TransactionInstruction(
            PROGRAM_ID,
            listOf(
                AccountMeta(account, false, true),
                AccountMeta(mintAccount, false, false),
                AccountMeta(owner, false, true),
                AccountMeta(SYSVAR_RENT_PUBKEY, false, false)
            ),
            BorshEncoder().apply {
                encodeByte(PROGRAM_INDEX_INIT_ACCOUNT)
            }.borshEncodedBytes
        )

    @JvmStatic
    fun transfer(
        from: SolanaPublicKey,
        to: SolanaPublicKey,
        amount: Long,
        owner: SolanaPublicKey
    ): TransactionInstruction =
        TransactionInstruction(
            PROGRAM_ID,
            listOf(
                AccountMeta(from, false, true),
                AccountMeta(to, false, true),
                AccountMeta(owner, true, true)
            ),
            BorshEncoder().apply {
                encodeByte(PROGRAM_INDEX_TRANSFER)
                encodeLong(amount)
            }.borshEncodedBytes
        )

    @JvmStatic
    fun mintTo(
        mint: SolanaPublicKey,
        destination: SolanaPublicKey,
        mintAuthority: SolanaPublicKey,
        amount: Long
    ): TransactionInstruction =
        TransactionInstruction(
            PROGRAM_ID,
            listOf(
                AccountMeta(mint, false, true),
                AccountMeta(destination, false, true),
                AccountMeta(mintAuthority, true, true)
            ),
            BorshEncoder().apply {
                encodeByte(PROGRAM_INDEX_MINT_TO)
                encodeLong(amount)
            }.borshEncodedBytes
        )

    @JvmStatic
    fun closeAccount(
        account: SolanaPublicKey,
        destination: SolanaPublicKey,
        owner: SolanaPublicKey
    ): TransactionInstruction =
        TransactionInstruction(
            PROGRAM_ID,
            listOf(
                AccountMeta(account, false, true),
                AccountMeta(destination, false, true),
                AccountMeta(owner, true, true)
            ),
            BorshEncoder().apply {
                encodeByte(PROGRAM_INDEX_CLOSE_ACCOUNT)
            }.borshEncodedBytes
        )

    @JvmStatic
    fun transferChecked(
        from: SolanaPublicKey,
        to: SolanaPublicKey,
        amount: Long,
        decimals: Byte,
        owner: SolanaPublicKey,
        mint: SolanaPublicKey
    ): TransactionInstruction =
        TransactionInstruction(
            PROGRAM_ID,
            listOf(
                AccountMeta(from, false, true),
                AccountMeta(mint, false, false),
                AccountMeta(to, false, true),
                AccountMeta(owner, true, true)
            ),
            BorshEncoder().apply {
                encodeByte(PROGRAM_INDEX_TRANSFER_CHECKED)
                encodeLong(amount)
                encodeByte(decimals)
            }.borshEncodedBytes
        )

    override val programId = PROGRAM_ID
}