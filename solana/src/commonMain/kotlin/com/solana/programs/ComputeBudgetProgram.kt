package com.solana.programs

import com.funkatronics.kborsh.BorshEncoder
import com.solana.publickey.SolanaPublicKey
import com.solana.transaction.TransactionInstruction
import kotlin.jvm.JvmStatic

object ComputeBudgetProgram : Program {
    @JvmStatic
    val PROGRAM_ID = SolanaPublicKey.from("ComputeBudget111111111111111111111111111111")

    private const val PROGRAM_INDEX_REQUEST_HEAP_FRAME = 1.toByte()
    private const val PROGRAM_INDEX_SET_COMPUTE_UNIT_LIMIT = 2.toByte()
    private const val PROGRAM_INDEX_SET_COMPUTE_UNIT_PRICE = 3.toByte()
    private const val PROGRAM_INDEX_SET_LOADED_ACCOUNTS_DATA_SIZE_LIMIT = 4.toByte()

    /**
     * Request a specific transaction-wide program heap region size in bytes. The value
     * requested must be a multiple of 1024. This new heap region size applies to each
     * program executed in the transaction, including all calls to CPIs.
     *
     * @param bytes the heap region size, in bytes
     */
    @JvmStatic
    fun requestHeapFrame(
        bytes: UInt
    ): TransactionInstruction =
        TransactionInstruction(PROGRAM_ID,
            listOf(),
            BorshEncoder().apply {
                encodeByte(PROGRAM_INDEX_REQUEST_HEAP_FRAME)
                encodeInt(bytes.toInt())
            }.borshEncodedBytes
        )

    /**
     * Set a specific compute unit limit that the transaction is allowed to consume.
     *
     * @param units the maximum compute units the that the transaction is allowed to consume
     */
    @JvmStatic
    fun setComputeUnitLimit(
        units: UInt
    ): TransactionInstruction =
        TransactionInstruction(PROGRAM_ID,
            listOf(),
            BorshEncoder().apply {
                encodeByte(PROGRAM_INDEX_SET_COMPUTE_UNIT_LIMIT)
                encodeInt(units.toInt())
            }.borshEncodedBytes
        )

    /**
     * Set a compute unit price in “micro-lamports” to pay a higher transaction fee for higher
     * transaction prioritization.
     *
     * @param uLamports the micro-lamport unit price for the transaction
     */
    @JvmStatic
    fun setComputeUnitPrice(
        uLamports: ULong
    ): TransactionInstruction =
        TransactionInstruction(PROGRAM_ID,
            listOf(),
            BorshEncoder().apply {
                encodeByte(PROGRAM_INDEX_SET_COMPUTE_UNIT_PRICE)
                encodeLong(uLamports.toLong())
            }.borshEncodedBytes
        )

    /**
     * Set a specific transaction-wide account data size limit, in bytes, is allowed to load.
     *
     * @param bytes the account data size limit, in bytes
     */
    @JvmStatic
    fun setLoadedAccountsDataSizeLimit(
        bytes: UInt
    ): TransactionInstruction =
        TransactionInstruction(PROGRAM_ID,
            listOf(),
            BorshEncoder().apply {
                encodeByte(PROGRAM_INDEX_SET_LOADED_ACCOUNTS_DATA_SIZE_LIMIT)
                encodeLong(bytes.toLong())
            }.borshEncodedBytes
        )

    override val programId = PROGRAM_ID
}