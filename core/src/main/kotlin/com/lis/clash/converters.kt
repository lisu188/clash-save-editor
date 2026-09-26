package com.lis.clash

import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

val converters = mapOf(
    Byte::class to ByteConverter,
    String::class to StringConverter,
    Int::class to IntConverter,
    Long::class to UnsignedLongConverter,
    List::class to ListConverter
)

interface Converter {
    fun toString(t: Any): String = t.toString()
    fun fromString(s: String): Any
    fun toBytes(t: Any, length: Int): List<Byte>
    fun fromBytes(s: List<Byte>, length: Int = s.size): Any
}

fun readLittleEndianInt(bytes: List<Byte>): Int {
    require(bytes.size in 1..4) { "Unsupported integer length: ${bytes.size}" }
    return bytes.foldIndexed(0) { index, result, byte -> result or ((byte.toInt() and 0xFF) shl (index * 8)) }
}

/** Raw low bits; semantic range checks belong to the converters. */
fun writeLittleEndianInt(value: Int, length: Int): List<Byte> {
    require(length in 1..4) { "Unsupported integer length: $length" }
    return List(length) { index -> ((value ushr (index * 8)) and 0xFF).toByte() }
}

fun readLittleEndianSignedInt(bytes: List<Byte>): Int {
    val raw = readLittleEndianInt(bytes)
    val shift = 32 - bytes.size * 8
    return (raw shl shift) shr shift
}

object ByteConverter : Converter {
    override fun fromString(s: String): Any = s.toByte()
    override fun toBytes(t: Any, length: Int): List<Byte> {
        require(length == 1)
        return listOf(t as Byte)
    }
    override fun fromBytes(s: List<Byte>, length: Int): Any = s.first()
}

object ListConverter : Converter {
    override fun toString(t: Any): String = (t as List<*>).joinToString(",", "[", "]")
    override fun fromString(s: String): Any {
        require(s.startsWith("[") && s.endsWith("]")) { "Byte lists use [1,2,3] syntax" }
        return if (s == "[]") emptyList<Byte>() else s.substring(1, s.length - 1).split(",").map { it.trim().toByte() }
    }
    override fun toBytes(t: Any, length: Int): List<Byte> {
        val value = (t as List<*>).map { it as Byte }
        require(value.size == length) { "Expected exactly $length bytes, received ${value.size}" }
        return value
    }
    override fun fromBytes(s: List<Byte>, length: Int): Any = s.toList()
}

object StringConverter : Converter {
    val DEFAULT_CHARSET: Charset = Charset.forName("windows-1250")
    override fun fromString(s: String): Any = s
    fun encode(value: String, length: Int, charset: Charset = DEFAULT_CHARSET, nulTerminated: Boolean = false): List<Byte> {
        require('\u0000' !in value) { "Text cannot contain NUL characters" }
        val buffer = charset.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).encode(CharBuffer.wrap(value))
        val limit = if (nulTerminated) length - 1 else length
        require(buffer.remaining() <= limit) { "Text needs ${buffer.remaining()} bytes; field allows $limit in ${charset.name()}" }
        val encoded = ByteArray(length)
        buffer.get(encoded, 0, buffer.remaining())
        return encoded.toList()
    }
    fun decode(value: List<Byte>, charset: Charset = DEFAULT_CHARSET): String =
        String(value.takeWhile { it != 0.toByte() }.toByteArray(), charset)
    override fun toBytes(t: Any, length: Int): List<Byte> = encode(t as String, length)
    override fun fromBytes(s: List<Byte>, length: Int): Any = decode(s.take(length))
}

object IntConverter : Converter {
    override fun fromString(s: String): Any = s.toInt()
    override fun toBytes(t: Any, length: Int): List<Byte> {
        val value = t as Int
        require(length in 1..4)
        require(length == 4 || value in 0..((1 shl (length * 8)) - 1)) { "Value $value exceeds unsigned ${length * 8}-bit range" }
        return writeLittleEndianInt(value, length)
    }
    override fun fromBytes(s: List<Byte>, length: Int): Any = readLittleEndianInt(s.take(length))
}

object UnsignedLongConverter : Converter {
    override fun fromString(s: String): Any = s.toLong()
    override fun toBytes(t: Any, length: Int): List<Byte> {
        require(length in 1..4)
        val value = t as Long
        require(value in 0..((1L shl (length * 8)) - 1)) { "Value $value exceeds unsigned ${length * 8}-bit range" }
        return writeLittleEndianInt(value.toInt(), length)
    }
    override fun fromBytes(s: List<Byte>, length: Int): Any =
        s.take(length).foldIndexed(0L) { index, result, byte -> result or ((byte.toLong() and 0xFF) shl (index * 8)) }
}

object SignedIntConverter : Converter {
    override fun fromString(s: String): Any = s.toInt()
    override fun toBytes(t: Any, length: Int): List<Byte> {
        require(length in 1..4)
        val value = t as Int
        val limit = 1L shl (length * 8 - 1)
        require(value.toLong() in -limit until limit) { "Value $value exceeds signed ${length * 8}-bit range" }
        return writeLittleEndianInt(value, length)
    }
    override fun fromBytes(s: List<Byte>, length: Int): Any = readLittleEndianSignedInt(s.take(length))
}
