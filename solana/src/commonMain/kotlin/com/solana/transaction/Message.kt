package com.solana.transaction

import com.solana.publickey.SolanaPublicKey
import com.solana.publickey.SolanaPublicKeySerializer
import com.solana.serialization.TransactionFormat
import com.solana.signer.Signer
import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

typealias Blockhash = SolanaPublicKey
val Blockhash.blockhash get() = this.bytes

@OptIn(ExperimentalSerializationApi::class)
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
            val writable = writableSigners + writableNonSigners
            val accounts = (signers + writableNonSigners + readOnlyNonSigners + programIds)
                .sortedWith(compareBy({ it !in signers }, { it !in writable }))
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
                signers.count { it !in writable }.toUByte(),
                accounts.count { it !in signers && it !in writable }.toUByte(),
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

@Serializable(V0MessageSerializer::class)
data class V0Message(
    override val signatureCount: UByte,
    override val readOnlyAccounts: UByte,
    override val readOnlyNonSigners: UByte,
    override val blockhash: Blockhash,
    override val accounts: List<SolanaPublicKey>,
    override val instructions: List<Instruction>,
    val addressTableLookups: List<AddressTableLookup>
) : Message()

@Serializable(V1MessageSerializer::class)
data class V1Message(
    override val signatureCount: UByte,
    override val readOnlyAccounts: UByte,
    override val readOnlyNonSigners: UByte,
    override val blockhash: Blockhash,
    override val accounts: List<SolanaPublicKey>,
    override val instructions: List<Instruction>,
    val config: TransactionConfig
) : Message()

@Serializable
data class TransactionConfig(
    val priorityFeeLamports: ULong? = null,
    val computeUnitLimit: UInt? = null,
    val loadedAccountsDataSizeLimit: UInt? = null,
    val requestedHeapSize: UInt? = null,
)

@Serializable
data class AddressTableLookup(
    val account: SolanaPublicKey,
    val writableIndexes: List<UByte>,
    val readOnlyIndexes: List<UByte>
)

object MessageSerializer : KSerializer<Message> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("com.solana.transaction.Message")

    private val VERSION_PREFIX_MASK = 0x80

    override fun deserialize(decoder: Decoder): Message {
        val firstByte = decoder.decodeByte()
        val version = if (firstByte.toInt() and VERSION_PREFIX_MASK == 0) -1 else firstByte.toInt() and 0x7f
        return when(version) {
            -1 -> decodeLegacy(decoder, firstByte)
            0 -> decoder.decodeSerializableValue(V0Message.serializer())
            1 -> decoder.decodeSerializableValue(V1Message.serializer())
            else -> throw SerializationException("Unknown transaction version: $version")
        }
    }

    override fun serialize(encoder: Encoder, value: Message) {
        when (value) {
            is LegacyMessage -> encoder.encodeSerializableValue(LegacyMessage.serializer(), value)
            is V0Message -> encoder.encodeSerializableValue(V0Message.serializer(), value)
            is V1Message -> encoder.encodeSerializableValue(V1Message.serializer(), value)
        }
    }

    private fun decodeLegacy(decoder: Decoder, firstByte: Byte): LegacyMessage {
        val signatureCount = firstByte.toUByte()
        val readOnlyAccounts = decoder.decodeByte().toUByte()
        val readOnlyNonSigners = decoder.decodeByte().toUByte()
        val accounts = decoder.decodeSerializableValue(ListSerializer(SolanaPublicKeySerializer))
        val blockhash = Blockhash(decoder.decodeSerializableValue(SolanaPublicKeySerializer).bytes)
        val instructions = decoder.decodeSerializableValue(ListSerializer(Instruction.serializer()))
        return LegacyMessage(
            signatureCount, readOnlyAccounts, readOnlyNonSigners,
            accounts, blockhash, instructions,
        )
    }
}

object V0MessageSerializer : KSerializer<V0Message> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("com.solana.transaction.V0Message") {
            LegacyMessage.serializer().descriptor.apply {
                elementNames.zip(elementDescriptors).toMap().forEach {
                    element(it.key, it.value)
                }
            }
            element("addressTableLookups", ListSerializer(AddressTableLookup.serializer()).descriptor)
        }

    private const val VERSION_PREFIX = 0x80.toByte()

    override fun deserialize(decoder: Decoder): V0Message {
        val firstByte = decoder.decodeByte()
        val signatureCount = if (firstByte == VERSION_PREFIX) decoder.decodeByte().toUByte() else firstByte.toUByte()
        val readOnlyAccounts = decoder.decodeByte().toUByte()
        val readOnlyNonSigners = decoder.decodeByte().toUByte()
        val accounts = decoder.decodeSerializableValue(ListSerializer(SolanaPublicKeySerializer))
        val blockhash = Blockhash(decoder.decodeSerializableValue(SolanaPublicKeySerializer).bytes)
        val instructions = decoder.decodeSerializableValue(ListSerializer(Instruction.serializer()))
        return V0Message(
            signatureCount, readOnlyAccounts, readOnlyNonSigners,
            blockhash, accounts, instructions,
            decoder.decodeSerializableValue(ListSerializer(AddressTableLookup.serializer()))
        )
    }

    override fun serialize(encoder: Encoder, value: V0Message) {
        encoder.encodeByte(VERSION_PREFIX)
        // header
        encoder.encodeByte(value.signatureCount.toByte())
        encoder.encodeByte(value.readOnlyAccounts.toByte())
        encoder.encodeByte(value.readOnlyNonSigners.toByte())
        // accounts
        encoder.encodeSerializableValue(ListSerializer(SolanaPublicKeySerializer), value.accounts)
        // blockhash
        encoder.encodeSerializableValue(SolanaPublicKeySerializer, value.blockhash)
        // instructions
        encoder.encodeSerializableValue(ListSerializer(Instruction.serializer()), value.instructions)
        // lookup tables
        encoder.encodeSerializableValue(ListSerializer(AddressTableLookup.serializer()), value.addressTableLookups)
    }
}

object V1MessageSerializer : KSerializer<V1Message> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("com.solana.transaction.Message")

    private const val VERSION_PREFIX = 0x81.toByte()

    @Serializable
    private data class IxHeader(
        val programIdIndex: UByte,
        val numAccounts: UByte,
        val dataLength: UShort,
    )

    private val TransactionConfig.mask get() =
        (priorityFeeLamports?.let { 0x03 } ?: 0) or
        (computeUnitLimit?.let { 0x04 } ?: 0) or
        (loadedAccountsDataSizeLimit?.let { 0x08 } ?: 0) or
        (requestedHeapSize?.let { 0x10 } ?: 0)

    override fun deserialize(decoder: Decoder): V1Message {
        val firstByte = decoder.decodeByte()
        val signatureCount = if (firstByte == VERSION_PREFIX) decoder.decodeByte().toUByte() else firstByte.toUByte()
        val readOnlyAccounts = decoder.decodeByte().toUByte()
        val readOnlyNonSigners = decoder.decodeByte().toUByte()
        val configMask = decoder.decodeInt()
        if (configMask and 0xFFE0 > 0) throw SerializationException("Invalid config mask: ${configMask.toString(2)}")
        if (((configMask + 1) and 0b10) != 0) throw SerializationException("Invalid config mask: partial priority fee bits detected")
        val blockhash = Blockhash(decoder.decodeSerializableValue(SolanaPublicKeySerializer).bytes)
        val numInstructions = decoder.decodeByte().toInt()
        val numAddresses = decoder.decodeByte().toInt()
        val addresses = List(numAddresses) {
            decoder.decodeSerializableValue(SolanaPublicKeySerializer)
        }
        val config = TransactionConfig(
            if (configMask and 0x03 == 0x03) decoder.decodeLong().toULong() else null,
            if (configMask and 0x04 == 0x04) decoder.decodeInt().toUInt() else null,
            if (configMask and 0x08 == 0x08) decoder.decodeInt().toUInt() else null,
            if (configMask and 0x10 == 0x10) decoder.decodeInt().toUInt() else null
        )
        val instructionHeaders = List(numInstructions) {
            decoder.decodeSerializableValue(IxHeader.serializer())
        }
        val instructions = instructionHeaders.map { ixHeader ->
            val accountIndices = ByteArray(ixHeader.numAccounts.toInt()) {
                decoder.decodeByte()
            }
            val data = ByteArray(ixHeader.dataLength.toInt()) {
                decoder.decodeByte()
            }
            Instruction(ixHeader.programIdIndex, accountIndices, data)
        }
        return V1Message(
            signatureCount, readOnlyAccounts, readOnlyNonSigners,
            blockhash, addresses, instructions, config
        )
    }

    override fun serialize(encoder: Encoder, value: V1Message) {
        encoder.encodeByte(VERSION_PREFIX)
        // header
        encoder.encodeByte(value.signatureCount.toByte())
        encoder.encodeByte(value.readOnlyAccounts.toByte())
        encoder.encodeByte(value.readOnlyNonSigners.toByte())
        // config mask
        encoder.encodeInt(value.config.mask)
        // blockhash
        encoder.encodeSerializableValue(SolanaPublicKeySerializer, value.blockhash)
        // counts
        encoder.encodeByte(value.instructions.size.toByte())
        encoder.encodeByte(value.accounts.size.toByte())
        // accounts
        value.accounts.forEach {
            encoder.encodeSerializableValue(SolanaPublicKeySerializer, it)
        }
        // config values
        value.config.priorityFeeLamports?.apply {
            encoder.encodeLong(this.toLong())
        }
        value.config.computeUnitLimit?.apply {
            encoder.encodeInt(this.toInt())
        }
        value.config.loadedAccountsDataSizeLimit?.apply {
            encoder.encodeInt(this.toInt())
        }
        value.config.requestedHeapSize?.apply {
            encoder.encodeInt(this.toInt())
        }
        // instructions
        value.instructions.forEach { ix ->
            encoder.encodeSerializableValue(IxHeader.serializer(),
                IxHeader(
                    ix.programIdIndex,
                    ix.accountIndices.size.toUByte(),
                    ix.data.size.toUShort()
                )
            )
        }
        value.instructions.forEach { ix ->
            ix.accountIndices.forEach {
                encoder.encodeByte(it)
            }
            ix.data.forEach {
                encoder.encodeByte(it)
            }
        }
    }
}

fun Message.toUnsignedTransaction(): Transaction = Transaction(this)
suspend fun Message.Builder.buildSignedTransaction(vararg signers: Signer) = build().toSignedTransaction(*signers)
suspend fun Message.toSignedTransaction(vararg signers: Signer): Transaction {
    val serializedMessage = serialize()
    return Transaction(signers.map { it.signPayload(serializedMessage).getOrThrow() }, this)
}