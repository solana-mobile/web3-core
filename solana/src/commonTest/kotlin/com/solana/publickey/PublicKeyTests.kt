package com.solana.publickey

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class PublicKeyTests {

    @Test
    fun testPublicKeyFromBase58String() {
        // given
        val base58 = "MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr" // Memo program v2 account address
        val expectedPublicKeyBytes = byteArrayOf(
            0x05.toByte(), 0x4a.toByte(), 0x53.toByte(), 0x5a.toByte(), 0x99.toByte(), 0x29.toByte(), 0x21.toByte(), 0x06.toByte(),
            0x4d.toByte(), 0x24.toByte(), 0xe8.toByte(), 0x71.toByte(), 0x60.toByte(), 0xda.toByte(), 0x38.toByte(), 0x7c.toByte(),
            0x7c.toByte(), 0x35.toByte(), 0xb5.toByte(), 0xdd.toByte(), 0xbc.toByte(), 0x92.toByte(), 0xbb.toByte(), 0x81.toByte(),
            0xe4.toByte(), 0x1f.toByte(), 0xa8.toByte(), 0x40.toByte(), 0x41.toByte(), 0x05.toByte(), 0x44.toByte(), 0x8d.toByte()
        )

        // when
        val publicKey = SolanaPublicKey.from(base58)

        // then
        assertContentEquals(expectedPublicKeyBytes, publicKey.bytes)
    }

    @Test
    fun testPublicKeyToString() {
        // given
        val expectedBase58 = "MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr" // Memo program v2 account address
        val publicKeyBytes = byteArrayOf(
            0x05.toByte(), 0x4a.toByte(), 0x53.toByte(), 0x5a.toByte(), 0x99.toByte(), 0x29.toByte(), 0x21.toByte(), 0x06.toByte(),
            0x4d.toByte(), 0x24.toByte(), 0xe8.toByte(), 0x71.toByte(), 0x60.toByte(), 0xda.toByte(), 0x38.toByte(), 0x7c.toByte(),
            0x7c.toByte(), 0x35.toByte(), 0xb5.toByte(), 0xdd.toByte(), 0xbc.toByte(), 0x92.toByte(), 0xbb.toByte(), 0x81.toByte(),
            0xe4.toByte(), 0x1f.toByte(), 0xa8.toByte(), 0x40.toByte(), 0x41.toByte(), 0x05.toByte(), 0x44.toByte(), 0x8d.toByte()
        )

        // when
        val publicKey = SolanaPublicKey(publicKeyBytes)

        // then
        assertEquals(expectedBase58, publicKey.address)
    }
}