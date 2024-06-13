package com.solana.publickey

import com.funkatronics.salt.isOnCurve

suspend fun PublicKey.isOnCurve() = bytes.isOnCurve()