package com.solana.publickey

/**
 * PublicKey Interface
 *
 * @author Funkatronics
 */
interface PublicKey {
    /**
     * byte length of the public key
     */
    val length: Number

    /**
     * the bytes making up the public key
     */
    val bytes: ByteArray

    /**
     * the readable address of the public key, typically a string encoding fo the public key bytes
     */
    val address: String

    /**
     * returns a string representation of the Public Key
     */
    @Deprecated("Deprecated, use PublicKey.address or toString() instead")
    fun string(): String
}