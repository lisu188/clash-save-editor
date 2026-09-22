package com.lis.clash.mcp

internal val Byte = ByteReferenceAdapter

internal object ByteReferenceAdapter {
    fun toUnsignedInt(value: kotlin.Byte): Int = value.toInt() and 0xFF
}
