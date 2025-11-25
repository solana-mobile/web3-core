package com.solana.transaction

import com.solana.publickey.SolanaPublicKey
import com.solana.publickey.SolanaPublicKeySerializer
import com.solana.serialization.TransactionFormat
import com.solana.signer.Signer
import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.experimental.and
import kotlin.experimental.or

typealias Blockhash = SolanaPublicKey
val Blockhash.blockhash get() = this.bytes

sealed class Message {

    abstract val signatureCount: UByte
    abstract val readOnlyAccounts: UByte
    abstract val readOnlyNonSigners: UByte
    abstract val accounts: List<SolanaPublicKey>
    abstract val blockhash: Blockhash
    abstract val instructions: List<Instruction>

    companion object {
        fun from(bytes: ByteArray) = TransactionFormat.decodeFromByteArray(MessageSerializer, bytes)
    }

    fun serialize(): ByteArray = TransactionFormat.encodeToByteArray(MessageSerializer, this)

    data class Builder(
        val instructions: MutableList<TransactionInstruction> = mutableListOf(),
        var blockhash: Blockhash? = null,
        var feePayer: SolanaPublicKey? = null,
    ) {

        fun addInstruction(instruction: TransactionInstruction) = apply {
            instructions.add(instruction)
        }

        fun addFeePayer(feePayer: SolanaPublicKey) = apply {
            this.feePayer = feePayer
        }

        fun setRecentBlockhash(blockhash: String) = setRecentBlockhash(Blockhash.from(blockhash))
        fun setRecentBlockhash(blockhash: Blockhash) = apply {
            this.blockhash = blockhash
        }

        fun build(): Message {
            check(blockhash != null)
            val writableSigners = mutableSetOf<SolanaPublicKey>()
            val readOnlySigners = mutableSetOf<SolanaPublicKey>()
            val writableNonSigners = mutableSetOf<SolanaPublicKey>()
            val readOnlyNonSigners = mutableSetOf<SolanaPublicKey>()
            val programIds = mutableSetOf<SolanaPublicKey>()
            feePayer?.apply { writableSigners.add(this) }
            instructions.forEach { instruction ->
                instruction.accounts.forEach { account ->
                    if (account.isSigner) {
                        if (account.isWritable) writableSigners.add(account.publicKey)
                        else readOnlySigners.add(account.publicKey)
                    } else {
                        if (account.isWritable) writableNonSigners.add(account.publicKey)
                        else readOnlyNonSigners.add(account.publicKey)
                    }
                }
                programIds.add(instruction.programId)
            }

            check(writableSigners.isNotEmpty()) {
                "Invalid transaction message: no fee payer was provided and no instruction contains a writable signer."
            }

            val signers = writableSigners + readOnlySigners
            val accounts = signers + writableNonSigners + readOnlyNonSigners + programIds
            val compiledInstructions = instructions.map { instruction ->
                Instruction(
                    accounts.indexOf(instruction.programId).toUByte(),
                    instruction.accounts.map {
                        accounts.indexOf(it.publicKey).toByte()
                    }.toByteArray(),
                    instruction.data
                )
            }

            return LegacyMessage(
                signers.size.toUByte(),
                readOnlySigners.count { it !in signers }.toUByte(),
                readOnlyNonSigners.count { it !in signers && it !in readOnlySigners }.toUByte(),
                accounts.toList(),
                blockhash!!,
                compiledInstructions
            )
        }
    }
}

@Serializable
data class LegacyMessage(
    override val signatureCount: UByte,
    override val readOnlyAccounts: UByte,
    override val readOnlyNonSigners: UByte,
    override val accounts: List<SolanaPublicKey>,
    override val blockhash: Blockhash,
    override val instructions: List<Instruction>
) : Message()

@Serializable
data class VersionedMessage(
    @Transient val version: Byte = 0,
    override val signatureCount: UByte,
    override val readOnlyAccounts: UByte,
    override val readOnlyNonSigners: UByte,
    override val accounts: List<SolanaPublicKey>,
    override val blockhash: Blockhash,
    override val instructions: List<Instruction>,
    val addressTableLookups: List<AddressTableLookup>
) : Message()

@Serializable
data class AddressTableLookup(
    val account: SolanaPublicKey,
    val writableIndexes: List<UByte>,
    val readOnlyIndexes: List<UByte>
)

object MessageSerializer : KSerializer<Message> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("com.solana.transaction.Message")

    override fun deserialize(decoder: Decoder): Message {
        val firstByte = decoder.decodeByte()
        val version = if (firstByte.toInt() and 0x80 == 0) -1 else firstByte and 0x7f
        val signatureCount = if (version >= 0) decoder.decodeByte().toUByte() else firstByte.toUByte()
        val readOnlyAccounts = decoder.decodeByte().toUByte()
        val readOnlyNonSigners = decoder.decodeByte().toUByte()
        val accounts = decoder.decodeSerializableValue(ListSerializer(SolanaPublicKeySerializer))
        val blockhash = Blockhash(decoder.decodeSerializableValue(SolanaPublicKeySerializer).bytes)
        val instructions = decoder.decodeSerializableValue(ListSerializer(Instruction.serializer()))
        return if (version >= 0)
            VersionedMessage(
                version,
                signatureCount, readOnlyAccounts, readOnlyNonSigners,
                accounts, blockhash, instructions,
                decoder.decodeSerializableValue(ListSerializer(AddressTableLookup.serializer()))
            )
        else
            LegacyMessage(
                signatureCount, readOnlyAccounts, readOnlyNonSigners,
                accounts, blockhash, instructions,
            )
    }

    override fun serialize(encoder: Encoder, value: Message) {
        if (value is VersionedMessage) encoder.encodeByte(0x80.toByte() or value.version)
        when (value) {
            is LegacyMessage -> encoder.encodeSerializableValue(LegacyMessage.serializer(), value)
            is VersionedMessage -> encoder.encodeSerializableValue(VersionedMessage.serializer(), value)
        }
    }
}

fun Message.toUnsignedTransaction(): Transaction = Transaction(this)
suspend fun Message.Builder.buildSignedTransaction(vararg signers: Signer) = build().toSignedTransaction(*signers)
suspend fun Message.toSignedTransaction(vararg signers: Signer): Transaction {
    val serializedMessage = serialize()
    return Transaction(signers.map { it.signPayload(serializedMessage).getOrThrow() }, this)
}