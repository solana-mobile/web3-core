package com.solana.publickey

import com.funkatronics.encoders.Base58
import com.funkatronics.kborsh.BorshDecoder
import com.funkatronics.kborsh.BorshEncoder
import com.solana.serialization.ByteStringSerializer
import com.solana.serialization.TransactionDecoder
import com.solana.serialization.TransactionEncoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder

@Serializable(with=SolanaPublicKeySerializer::class)
open class SolanaPublicKey(final override val bytes: ByteArray) : PublicKey {

    init {
        require(bytes.size == PUBLIC_KEY_LENGTH) { "Invalid public key length: ${bytes.size}" }
    }

    override val length = PUBLIC_KEY_LENGTH
    override val address = Base58.encodeToString(bytes)

    @Deprecated("Deprecated, use PublicKey.address or toString() instead",
        ReplaceWith("address"))
    override fun string(): String = address

    fun base58(): String = address

    companion object {
        const val PUBLIC_KEY_LENGTH = 32
        fun from(base58: String) = SolanaPublicKey(Base58.decode(base58))
    }

    override fun equals(other: Any?): Boolean {
        return (other is PublicKey) && this.bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int = bytes.contentHashCode()

    override fun toString() = address
}

object SolanaPublicKeySerializer : KSerializer<SolanaPublicKey> {
    private val borshDelegate = ByteStringSerializer(SolanaPublicKey.PUBLIC_KEY_LENGTH)
    private val jsonDelegate = String.serializer()
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SolanaPublicKey")

    override fun deserialize(decoder: Decoder): SolanaPublicKey =
        when (decoder) {
            is BorshDecoder, is TransactionDecoder ->
                SolanaPublicKey(decoder.decodeSerializableValue(borshDelegate))
            is JsonDecoder ->
                SolanaPublicKey.from(decoder.decodeSerializableValue(jsonDelegate))
            else ->
                runCatching {
                    SolanaPublicKey.from(decoder.decodeSerializableValue(jsonDelegate))
                }.getOrElse {
                    SolanaPublicKey(decoder.decodeSerializableValue(borshDelegate))
                }
        }

    override fun serialize(encoder: Encoder, value: SolanaPublicKey) =
        when (encoder) {
            is BorshEncoder, is TransactionEncoder ->
                encoder.encodeSerializableValue(borshDelegate, value.bytes)
            is JsonDecoder ->
                encoder.encodeSerializableValue(jsonDelegate, value.base58())
            else ->
                runCatching {
                    encoder.encodeSerializableValue(jsonDelegate, value.base58())
                }.getOrElse {
                    encoder.encodeSerializableValue(borshDelegate, value.bytes)
                }
        }
}
