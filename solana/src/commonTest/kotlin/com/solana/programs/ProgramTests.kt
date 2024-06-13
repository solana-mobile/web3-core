package com.solana.programs

import com.solana.publickey.SolanaPublicKey
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProgramTests {

    @Test
    fun `createProgramAddress returns failure for on chain pubkey`() = runTest {
        // given
        val seeds = listOf("helloWorld".encodeToByteArray(), byteArrayOf(255.toByte()))
        val program = object : Program {
            override val programId = SolanaPublicKey.from("11111111111111111111111111111111")
        }

        // when
        val result = program.createDerivedAddress(seeds)

        // then
        assertTrue { result.isFailure }
    }

    @Test
    fun `createProgramAddress returns expected pubkey for nonce`() = runTest {
        // given
        val seeds = listOf("helloWorld".encodeToByteArray(), byteArrayOf(252.toByte()))
        val expectedPublicKey = SolanaPublicKey.from("THfBMgduMonjaNsCisKa7Qz2cBoG1VCUYHyso7UXYHH")
        val program = object : Program {
            override val programId = SolanaPublicKey.from("11111111111111111111111111111111")
        }

        // when
        val result = program.createDerivedAddress(seeds)

        // then
        assertTrue { result.isSuccess }
        assertEquals(expectedPublicKey, result.getOrNull()!!)
    }

    @Test
    fun `findProgramAddress returns expected pubkey and nonce`() = runTest {
        // given
        val seeds = listOf<ByteArray>()
        val expectedBump = 255.toUByte()
        val expectedPublicKey = SolanaPublicKey.from("Cu7NwqCXSmsR5vgGA3Vw9uYVViPi3kQvkbKByVQ8nPY9")
        val program = object : Program {
            override val programId = SolanaPublicKey.from("11111111111111111111111111111111")
        }

        // when
        val result = program.findDerivedAddress(seeds)

        // then
        assertTrue { result.isSuccess }
        assertEquals(expectedPublicKey, result.getOrNull()!!)
        assertEquals(expectedBump, result.getOrNull()!!.nonce)
    }

    @Test
    fun `findProgramAddress returns expected pubkey and nonce for seeds`() = runTest {
        // given
        val seeds = listOf("helloWorld".encodeToByteArray())
        val expectedBump = 254.toUByte()
        val expectedPublicKey = SolanaPublicKey.from("46GZzzetjCURsdFPb7rcnspbEMnCBXe9kpjrsZAkKb6X")
        val program = object : Program {
            override val programId = SolanaPublicKey.from("11111111111111111111111111111111")
        }

        // when
        val result = program.findDerivedAddress(seeds)

        // then
        assertTrue { result.isSuccess }
        assertEquals(expectedPublicKey, result.getOrNull()!!)
        assertEquals(expectedBump, result.getOrNull()!!.nonce)
    }
}