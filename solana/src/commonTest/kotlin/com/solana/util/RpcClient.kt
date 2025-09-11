package com.solana.util

import com.funkatronics.encoders.Base58
import com.funkatronics.encoders.Base64
import com.solana.networking.HttpNetworkDriver
import com.solana.networking.HttpRequest
import com.solana.networking.Rpc20Driver
import com.solana.publickey.SolanaPublicKey
import com.solana.publickey.SolanaPublicKeySerializer
import com.solana.rpccore.JsonRpc20Request
import com.solana.transaction.Transaction
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.date.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlin.math.pow

class RpcClient(val rpcDriver: Rpc20Driver) {

    constructor(url: String, networkDriver: HttpNetworkDriver = KtorHttpDriver()): this(Rpc20Driver(url, networkDriver))

    suspend fun requestAirdrop(address: SolanaPublicKey, amountSol: Float) =
        rpcDriver.makeRequest(
            AirdropRequest(address, (amountSol*10f.pow(9)).toLong()),
            String.serializer()
        )

    suspend fun getBalance(address: SolanaPublicKey, commitment: String = "confirmed") =
        rpcDriver.makeRequest(BalanceRequest(address, commitment), SolanaResponseSerializer(Long.serializer()))

    suspend fun getMinBalanceForRentExemption(size: Long, commitment: String? = null) =
        rpcDriver.makeRequest(RentExemptBalanceRequest(size, commitment), Long.serializer())

    suspend fun getLatestBlockhash() =
        rpcDriver.makeRequest(LatestBlockhashRequest(), SolanaResponseSerializer(BlockhashResponse.serializer()))

    suspend fun getAccountInfo(address: SolanaPublicKey, commitment: String = "confirmed") =
        rpcDriver.makeRequest(AccountInfoRequest(address, commitment), SolanaResponseSerializer(AccountInfo.serializer()))

    suspend fun simulateTransaction(transaction: Transaction, commitment: String = "confirmed") =
        rpcDriver.makeRequest(SimulateTransactionRequest(transaction, commitment), SolanaResponseSerializer(JsonElement.serializer()))

    suspend fun sendTransaction(transaction: Transaction) =
        rpcDriver.makeRequest(SendTransactionRequest(transaction), String.serializer())

    suspend fun sendAndConfirmTransaction(transaction: Transaction) =
        sendTransaction(transaction).apply {
            result?.let { confirmTransaction(it) }
        }

    suspend fun getSignatureStatuses(signatures: List<String>) =
        rpcDriver.makeRequest(SignatureStatusesRequest(signatures),
            SolanaResponseSerializer(ListSerializer(SignatureStatus.serializer().nullable)))

    suspend fun confirmTransaction(
        signature: String,
        commitment: String = "confirmed",
        timeout: Long = 15000
    ): Result<String> = withTimeout(timeout) {
        suspend fun getStatus() =
            getSignatureStatuses(listOf(signature))
                .result?.first()

        // wait for desired transaction status
        while(getStatus()?.confirmationStatus != commitment) {

            // wait a bit before retrying
            val millis = getTimeMillis()
            var inc = 0
            while(getTimeMillis() - millis < 300 && isActive) { inc++ }

            if (!isActive) break // breakout after timeout
        }

        Result.success(signature)
    }

    class SolanaResponseSerializer<R>(dataSerializer: KSerializer<R>)
        : KSerializer<R?> {
        private val serializer = WrappedValue.serializer(dataSerializer)
        override val descriptor: SerialDescriptor = serializer.descriptor

        override fun serialize(encoder: Encoder, value: R?) =
            encoder.encodeSerializableValue(serializer, WrappedValue(value))

        override fun deserialize(decoder: Decoder): R? =
            decoder.decodeSerializableValue(serializer).value
    }

    @Serializable
    class WrappedValue<V>(val value: V?)

    class KtorHttpDriver : HttpNetworkDriver {
        override suspend fun makeHttpRequest(request: HttpRequest): String =
            HttpClient().use { client ->
                client.request(request.url) {
                    method = HttpMethod.parse(request.method)
                    request.properties.forEach { (k, v) ->
                        header(k, v)
                    }
                    setBody(request.body)
                }.bodyAsText().apply {
                    println(this)
                }
            }
    }

    class AirdropRequest(address: SolanaPublicKey, lamports: Long, requestId: String = "1")
        : JsonRpc20Request(
            method = "requestAirdrop",
            params = buildJsonArray {
                add(address.base58())
                add(lamports)
            },
            id = requestId
        )

    class BalanceRequest(address: SolanaPublicKey, commitment: String = "confirmed", requestId: String = "1")
        : JsonRpc20Request(
            method = "getBalance",
            params = buildJsonArray {
                add(address.base58())
                addJsonObject {
                    put("commitment", commitment)
                }
            },
            requestId
        )

    class LatestBlockhashRequest(commitment: String = "confirmed", requestId: String = "1")
        : JsonRpc20Request(
            method = "getLatestBlockhash",
            params = buildJsonArray {
                addJsonObject {
                    put("commitment", commitment)
                }
            },
            requestId
        )

    @Serializable
    class BlockhashResponse(
        val blockhash: String,
        val lastValidBlockHeight: Long
    )

    class SendTransactionRequest(transaction: Transaction, skipPreflight: Boolean = true, requestId: String = "1")
        : JsonRpc20Request(
            method = "sendTransaction",
            params = buildJsonArray {
                add(Base58.encodeToString(transaction.serialize()))
                addJsonObject {
                    put("skipPreflight", skipPreflight)
                }
            },
            requestId
        )

    class SignatureStatusesRequest(transactionIds: List<String>, searchTransactionHistory: Boolean = false, requestId: String = "1")
        : JsonRpc20Request(
            method = "getSignatureStatuses",
            params = buildJsonArray {
                addJsonArray { transactionIds.forEach { add(it) } }
                addJsonObject {
                    put("searchTransactionHistory", searchTransactionHistory)
                }
            },
            requestId
        )

    @Serializable
    data class SignatureStatus(
        val slot: Long,
        val confirmations: Long?,
        var err: JsonObject?,
        var confirmationStatus: String?
    )

    class RentExemptBalanceRequest(size: Long, commitment: String? = null, requestId: String = "1")
        : JsonRpc20Request(
            method = "getMinimumBalanceForRentExemption",
            params = buildJsonArray {
                add(size)
                commitment?.let {
                    addJsonObject {
                        put("commitment", commitment)
                    }
                }
            },
            requestId
        )

    class AccountInfoRequest(
        address: SolanaPublicKey,
        commitment: String = "confirmed",
        encoding: String = "base64",
        requestId: String = "1"
    ): JsonRpc20Request(
        method = "getAccountInfo",
        params = buildJsonArray {
            add(address.base58())
            addJsonObject {
                put("commitment", commitment)
                put("encoding", encoding)
            }
        },
        requestId
    )

    @Serializable
    data class AccountInfo(
        val data: JsonElement,
        val executable: Boolean,
        val lamports: ULong,
        val owner: SolanaPublicKey,
        val rentEpoch: ULong,
        val space: ULong
    )

    @Serializable
    data class SplTokenAccountInfo(
        val isNative: Boolean?,
        val mint: SolanaPublicKey?,
        val owner: SolanaPublicKey?,
        val state: SolanaPublicKey?,
    )

    class SimulateTransactionRequest(
        transaction: Transaction,
        commitment: String = "confirmed",
        requestId: String = "1"
    ): JsonRpc20Request(
        method = "simulateTransaction",
        params = buildJsonArray {
            add(Base64.encodeToString(transaction.serialize()))
            addJsonObject {
                put("commitment", commitment)
                put("encoding", "base64")
            }
        },
        requestId
    )
}