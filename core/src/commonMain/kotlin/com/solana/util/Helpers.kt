package com.solana.util

fun divideBase10StringByLong(base10: String, divisor: Long): String {
    require(base10.isNotEmpty()) { "Invalid Base10 String: Empty string" }
    require(base10.all { it in '0'..'9' }) { "Invalid Base10 String: Non-digit character" }
    // As result can be very large store it in string but since
    // we need to modify it very often so using string builder
    val result = StringBuilder()

    // Initially the carry would be zero
    var carry = 0L

    // Iterate the dividend
    base10.forEach { c ->
        // Prepare the number to be divided
        val x: Long = (carry * 10 + c.digitToInt(10))

        // Append the result with partial quotient
        result.append(x / divisor)

        // Prepare the carry for the next Iteration
        carry = x % divisor
    }

    // Remove any leading zeros
    return result.trimStart('0').run {
        if (isEmpty()) "0" else toString()
    }
}