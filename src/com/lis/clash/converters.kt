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
        return s.subSequence(1, s.length - 2).split(",").map { it.toByte() }
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
        return (t as String).toByteArray().toList()
    }

    override fun fromBytes(s: List<Byte>, length: Int): Any {
        return String(s.toByteArray())
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
