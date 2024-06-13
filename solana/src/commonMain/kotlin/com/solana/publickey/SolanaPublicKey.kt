package com.solana.publickey

import com.funkatronics.encoders.Base58
import com.solana.serialization.ByteStringSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with=SolanaPublicKeySerializer::class)
open class SolanaPublicKey(final override val bytes: ByteArray) : PublicKey {

    init {
        require(bytes.size == PUBLIC_KEY_LENGTH) { "Invalid public key length: ${bytes.size}" }
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
    private val delegate = ByteStringSerializer(SolanaPublicKey.PUBLIC_KEY_LENGTH)
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): SolanaPublicKey =
        SolanaPublicKey(decoder.decodeSerializableValue(delegate))

    override fun serialize(encoder: Encoder, value: SolanaPublicKey) {
        encoder.encodeSerializableValue(delegate, value.bytes)
    }
}
