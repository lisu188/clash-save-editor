package com.lis.clash

interface Converter {
    fun toString(t: Any): String;
    fun fromString(s: String): Any

    fun toBytes(t: Any): List<Byte>;
    fun fromBytes(s: List<Byte>): Any
}

object NoneConverter : Converter {
    override fun toString(t: Any): String {
        throw IllegalArgumentException("No converter!")
    }

    override fun fromString(s: String): Any {
        throw IllegalArgumentException("No converter!")
    }

    override fun toBytes(t: Any): List<Byte> {
        throw IllegalArgumentException("No converter!")
    }

    override fun fromBytes(s: List<Byte>): Any {
        throw IllegalArgumentException("No converter!")
    }

}

object ByteConverter : Converter {
    override fun toString(t: Any): String {
        return t.toString();
    }

    override fun fromString(s: String): Any {
        return s.toByte();
    }

    override fun toBytes(t: Any): List<Byte> {
        return listOf(t as Byte)
    }

    override fun fromBytes(s: List<Byte>): Any {
        return s.first()
    }

}

object StringConverter : Converter {
    override fun toString(t: Any): String {
        return t as String
    }

    override fun fromString(s: String): Any {
        return s
    }

    override fun toBytes(t: Any): List<Byte> {
        return (t as String).toByteArray().toList()
    }

    override fun fromBytes(s: List<Byte>): Any {
        return String(s.toByteArray())
    }

}