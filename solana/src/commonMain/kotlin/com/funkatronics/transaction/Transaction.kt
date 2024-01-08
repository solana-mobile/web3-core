package com.funkatronics.transaction

import com.funkatronics.serialization.ByteStringSerializer
import com.funkatronics.serialization.TransactionFormat
import kotlinx.serialization.*

object SignatureSerializer : ByteStringSerializer(64)

@Serializable
data class Transaction(
    val signatures: List<@Serializable(with = SignatureSerializer::class) ByteArray>,
    @Serializable(with = MessageSerializer::class) val message: Message
) {

    constructor(message: Message): this(buildList(message.signatureCount.toInt()) { ByteArray(size) }, message)

    companion object {
        fun from(bytes: ByteArray) = TransactionFormat.decodeFromByteArray(serializer(), bytes)
    }

    fun serialize(): ByteArray = TransactionFormat.encodeToByteArray(serializer(), this)
}