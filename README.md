# Web3 Core
Multiplatform Web3 Library

[![badge-latest-release]][url-latest-release]
[![badge-license]][url-license]
[![badge-kotlin]][url-kotlin]

## ⚠️ Experimental
This library is still under development. The library is functional and being used downstream by the likes of [SolanaKMP](https://github.com/metaplex-foundation/solana-kmp). Breaking changes ~~may~~ will be added leading up to a v1.0 API. You have been warned. 

# Usage

## Solana

### Build a Transaction

```kotlin
// Solana Memo Program 
val account = SolanaPublicKey(keyPair.publicKey)
val memoProgramId = SolanaPublicKey.from("MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr")
val memoInstruction = TransactionInstruction(
    memoProgramId,
    listOf(AccountMeta(account, true, true)),
    "hello world ".encodeToByteArray()
)

// Build Message
val blockhash = Blockhash(getRecentBlockhash())
val message = Message.Builder()
    .addInstruction(memoInstruction)
    .setRecentBlockhash(blockhash)
    .build()
```

### Prepare a Signer

using [Diglol Crypto library](https://github.com/diglol/crypto) for ED25519 signing:

```kotlin
// prepare signer
val keyPair = Ed25519.generateKeyPair()
val signer = object : Ed25519Signer() {
    override val publicKey: ByteArray get() = keyPair.publicKey
    override suspend fun signPayload(payload: ByteArray): ByteArray = Ed25519.sign(keyPair, payload)
}
```

### Sign Message & Build Transaction

```kotlin
// Sign Message
val signature = signer.signPayload(message.serialize())

// Build Transaction
val transaction = Transaction(listOf(signature), message)
```

### Send Transaction to RPC

Using Base58 encoding from [MultiMult](https://github.com/Funkatronics/multimult) and Solana RPC driver from [RpcCore](https://github.com/Funkatronics/RpcCore)

```kotlin
// serialize transaction
val transactionBytes = transaction.serialize()
val encodedTransaction = Base58.encodeToString(transactionBytes)

// setup RPC driver
val rpcUrl = "https://api.endpoint.com"
val rpcDriver = Rpc20Driver(rpcUrl, MyNetworkDriver())

class SendTransactionRequest(encodedTransaction: String, requestId: String)
    : JsonRpc20Request(
        method = "sendTransaction",
        params = buildJsonArray {
            add(encodedTransaction)
        },
        requestId
    )

// build rpc request
val requestId = 1
val rpcRequest = SendTransactionRequest(encodedTransaction, requestId)

// send the request and get response
// using JsonElement.serializer() will return the JSON RPC response. you can use your own serializer to get back a specific object
val rpcResponse = rpcDriver.makeRequest(rpcRequest, JsonElement.serializer())
```

<!-- TAG_VERSION -->
[badge-latest-release]: https://img.shields.io/badge/latest--release-0.2.5-blue.svg?style=flat
[badge-license]: https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat

<!-- TAG_DEPENDENCIES -->
[badge-kotlin]: https://img.shields.io/badge/kotlin-1.9.21-blue.svg?logo=kotlin

[url-latest-release]: https://github.com/solana-mobile/web3-core/releases/latest
[url-license]: https://www.apache.org/licenses/LICENSE-2.0.txt
[url-kotlin]: https://kotlinlang.org
