package com.solana.transaction

import com.solana.serialization.ByteStringSerializer
import com.solana.serialization.TransactionDecoder
import com.solana.serialization.TransactionFormat
import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object SignatureSerializer : ByteStringSerializer(Transaction.SIGNATURE_LENGTH_BYTES)

@Serializable(TransactionSerializer::class)
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

@OptIn(ExperimentalSerializationApi::class)
object TransactionSerializer : KSerializer<Transaction> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("com.solana.transaction.Transaction")

    override fun deserialize(decoder: Decoder): Transaction {
        val firstByte = decoder.decodeByte()
        val version = if (firstByte.toInt() and 0x80 == 0) -1 else firstByte.toInt() and 0x7f
        if (version == 1) {
            val message = decoder.decodeSerializableValue(V1Message.serializer())
            val signatures = List(message.signatureCount.toInt()) {
                decoder.decodeSerializableValue(SignatureSerializer)
            }
            return Transaction(signatures, message)
        } else {
            val signatures = List(TransactionDecoder.decodeCompactU16(decoder, firstByte)) {
                decoder.decodeSerializableValue(SignatureSerializer)
            }
            val message = decoder.decodeSerializableValue(MessageSerializer)
            return Transaction(signatures, message)
        }
    }

    override fun serialize(encoder: Encoder, value: Transaction) {
        when (value.message) {
            is V1Message -> {
                encoder.encodeSerializableValue(MessageSerializer, value.message)
                value.signatures.forEach {
                    encoder.encodeSerializableValue(SignatureSerializer, it)
                }
            }
            else -> {
                encoder.encodeSerializableValue(ListSerializer(SignatureSerializer), value.signatures)
                encoder.encodeSerializableValue(MessageSerializer, value.message)
            }
        }
    }
}