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
     * returns a string representation of the Public Key
     */
    fun string(): String
}