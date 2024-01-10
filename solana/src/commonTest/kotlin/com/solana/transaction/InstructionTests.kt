package com.solana.transaction

import com.solana.serialization.TransactionFormat
import com.solana.util.asVarint
import kotlin.test.Test
import kotlin.test.assertContentEquals

class InstructionTests {

    @Test
    fun testInstructionSerialize() {
        // given
        val programIdIndex = 1.toUByte()
        val accountAddressIndices = byteArrayOf(0)
        val data = "hello world".encodeToByteArray()
        val instruction = Instruction(programIdIndex, accountAddressIndices, data)

        val expectedBytes = byteArrayOf(programIdIndex.toByte()) +
                accountAddressIndices.size.asVarint() + accountAddressIndices +
                data.size.asVarint() + data

        // when
        val serializedTransaction = TransactionFormat.encodeToByteArray(Instruction.serializer(), instruction)

        // then
        assertContentEquals(expectedBytes, serializedTransaction)
    }
}