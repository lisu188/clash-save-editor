package com.lis.clash

val converters = mapOf(
    Byte::class to ByteConverter::class.objectInstance,
    String::class to StringConverter::class.objectInstance,
    Int::class to IntConverter::class.objectInstance,
    List::class to ListConverter::class.objectInstance
)

interface Converter {
    fun toString(t: Any): String
    fun fromString(s: String): Any

    fun toBytes(t: Any, length: Int): List<Byte>
    fun fromBytes(s: List<Byte>, length: Int = s.size): Any
}

internal fun readLittleEndianInt(bytes: List<Byte>): Int {
    var result = 0
    bytes.forEachIndexed { index, byte ->
        result = result or ((byte.toInt() and 0xFF) shl (index * 8))
    }
    return result
}

internal fun writeLittleEndianInt(value: Int, length: Int): List<Byte> {
    require(length in 1..4) { "Unsupported integer length: $length" }
    return List(length) { index ->
        ((value ushr (index * 8)) and 0xFF).toByte()
    }
}

internal fun readLittleEndianSignedInt(bytes: List<Byte>): Int {
    val unsignedValue = readLittleEndianInt(bytes)
    if (bytes.isEmpty() || bytes.size >= 4) {
        return unsignedValue
    }

    val bitCount = bytes.size * 8
    val signBit = 1 shl (bitCount - 1)
    return if ((unsignedValue and signBit) != 0) {
        unsignedValue - (1 shl bitCount)
    } else {
        unsignedValue
    }
}

object ByteConverter : Converter {
    override fun toString(t: Any): String {
        return t.toString()
    }

    override fun fromString(s: String): Any {
        return s.toByte()
    }

    override fun toBytes(t: Any, length: Int): List<Byte> {
        return listOf(t as Byte)
    }

    override fun fromBytes(s: List<Byte>, length: Int): Any {
        return s.first()
    }
}

object ListConverter : Converter {
    override fun toString(t: Any): String {
        return (t as List<*>).joinToString(",", "[", "]")
    }

    override fun fromString(s: String): Any {
        if (s == "[]") {
            return emptyList<Byte>()
        }
        return s.subSequence(1, s.length - 1)
            .split(",")
            .map { it.trim().toByte() }
    }

    override fun toBytes(t: Any, length: Int): List<Byte> {
        @Suppress("UNCHECKED_CAST")
        return t as List<Byte>
    }

    override fun fromBytes(s: List<Byte>, length: Int): Any {
        return s
    }
}

object StringConverter : Converter {
    override fun toString(t: Any): String {
        return t as String
    }

    override fun fromString(s: String): Any {
        return s
    }

    override fun toBytes(t: Any, length: Int): List<Byte> {
        val encoded = (t as String).toByteArray().toList().take(length)
        return encoded + List((length - encoded.size).coerceAtLeast(0)) { 0 }
    }

    override fun fromBytes(s: List<Byte>, length: Int): Any {
        return String(s.toByteArray()).trimEnd('\u0000')
    }

}


object IntConverter : Converter {
    override fun toString(t: Any): String {
        return t.toString()
    }

    override fun fromString(s: String): Any {
        return s.toInt()
    }

    override fun toBytes(t: Any, length: Int): List<Byte> {
        return writeLittleEndianInt(t as Int, length)
    }

    override fun fromBytes(s: List<Byte>, length: Int): Any {
        return readLittleEndianInt(s.take(length))
    }

}

object SignedIntConverter : Converter {
    override fun toString(t: Any): String {
        return t.toString()
    }

    override fun fromString(s: String): Any {
        return s.toInt()
    }

    override fun toBytes(t: Any, length: Int): List<Byte> {
        return writeLittleEndianInt(t as Int, length)
    }

    override fun fromBytes(s: List<Byte>, length: Int): Any {
        return readLittleEndianSignedInt(s.take(length))
    }
}
