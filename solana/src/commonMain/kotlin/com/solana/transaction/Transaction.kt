package com.solana.transaction

import com.solana.serialization.ByteStringSerializer
import com.solana.serialization.TransactionFormat
import kotlinx.serialization.*

object SignatureSerializer : ByteStringSerializer(Transaction.SIGNATURE_LENGTH_BYTES)

@Serializable
data class Transaction(
    val signatures: List<@Serializable(with = SignatureSerializer::class) ByteArray>,
    @Serializable(with = MessageSerializer::class) val message: Message
) {

    constructor(message: Message):
            this(List(message.signatureCount.toInt()) { ByteArray(SIGNATURE_LENGTH_BYTES) }, message)

    companion object {
        const val SIGNATURE_LENGTH_BYTES = 64
        fun from(bytes: ByteArray) = TransactionFormat.decodeFromByteArray(serializer(), bytes)
    }

    fun serialize(): ByteArray = TransactionFormat.encodeToByteArray(serializer(), this)
}