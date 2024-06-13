package com.solana.serialization

import com.funkatronics.hash.Sha256
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer

open class DiscriminatorSerializer<T>(val discriminator: ByteArray, serializer: KSerializer<T>)
    : KSerializer<T> {

    private val accountSerializer = serializer
    override val descriptor: SerialDescriptor = accountSerializer.descriptor

    override fun serialize(encoder: Encoder, value: T) {
        discriminator.forEach { encoder.encodeByte(it) }
        accountSerializer.serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): T {
        ByteArray(discriminator.size).map { decoder.decodeByte() }
        return accountSerializer.deserialize(decoder)
    }
}

open class AnchorDiscriminatorSerializer<T>(namespace: String, ixName: String,
                                            serializer: KSerializer<T>)
    : DiscriminatorSerializer<T>(buildDiscriminator(namespace, ixName), serializer) {
    companion object {
        private fun buildDiscriminator(namespace: String, ixName: String) =
            Sha256.hash("$namespace:$ixName".encodeToByteArray()).sliceArray(0 until 8)
    }
}

class AnchorInstructionSerializer<T>(namespace: String, ixName: String, serializer: KSerializer<T>)
    : AnchorDiscriminatorSerializer<T>(namespace, ixName, serializer) {
        constructor(ixName: String, serializer: KSerializer<T>) : this("global", ixName, serializer)
    }

inline fun <reified A> AnchorInstructionSerializer(namespace: String, ixName: String) =
    AnchorInstructionSerializer<A>(namespace, ixName, serializer())

inline fun <reified A> AnchorInstructionSerializer(ixName: String) =
    AnchorInstructionSerializer<A>(ixName, serializer())