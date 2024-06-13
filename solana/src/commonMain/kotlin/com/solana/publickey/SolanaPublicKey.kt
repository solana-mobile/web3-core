package com.solana.publickey

import com.funkatronics.encoders.Base58
import com.funkatronics.kborsh.BorshDecoder
import com.funkatronics.kborsh.BorshEncoder
import com.solana.serialization.ByteStringSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with=SolanaPublicKeySerializer::class)
open class SolanaPublicKey(final override val bytes: ByteArray) : PublicKey {

    init {
        check (bytes.size == PUBLIC_KEY_LENGTH)
    }

    override val length = PUBLIC_KEY_LENGTH
    override fun string(): String = base58()

    fun base58(): String = Base58.encodeToString(bytes)

    companion object {
        const val PUBLIC_KEY_LENGTH = 32
        fun from(base58: String) = SolanaPublicKey(Base58.decode(base58))
    }

    override fun equals(other: Any?): Boolean {
        return (other is PublicKey) && this.bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int = bytes.contentHashCode()

    override fun toString() = base58()
}

object SolanaPublicKeySerializer : KSerializer<SolanaPublicKey> {
    private val borshDelegate = ByteStringSerializer(SolanaPublicKey.PUBLIC_KEY_LENGTH)
    private val jsonDelegate = String.serializer()
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SolanaPublicKey")

    override fun deserialize(decoder: Decoder): SolanaPublicKey =
        if (decoder is BorshDecoder) SolanaPublicKey(decoder.decodeSerializableValue(borshDelegate))
        else SolanaPublicKey.from(decoder.decodeSerializableValue(jsonDelegate))

    override fun serialize(encoder: Encoder, value: SolanaPublicKey) {
        if (encoder is BorshEncoder) encoder.encodeSerializableValue(borshDelegate, value.bytes)
        else encoder.encodeSerializableValue(jsonDelegate, value.base58())
    }
}
