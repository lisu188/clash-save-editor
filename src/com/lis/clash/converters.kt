package com.lis.clash

import java.math.BigInteger

val converters = mapOf(
    Byte::class to ByteConverter::class.objectInstance,
    String::class to StringConverter::class.objectInstance,
    Int::class to IntConverter::class.objectInstance
)

interface Converter {
    fun toString(t: Any): String;
    fun fromString(s: String): Any

    fun toBytes(t: Any): List<Byte>;
    fun fromBytes(s: List<Byte>): Any
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


object IntConverter : Converter {
    override fun toString(t: Any): String {
        return t.toString();
    }

    override fun fromString(s: String): Any {
        return s.toInt();
    }

    override fun toBytes(t: Any): List<Byte> {
        var retVal = (t as Int).toBigInteger().toByteArray().asList()
        if (retVal.size == 1) {
            retVal = retVal + listOf(0.toByte())
        }
        return retVal
    }

    override fun fromBytes(s: List<Byte>): Any {
        val bigInteger = BigInteger(s.toByteArray())
        return bigInteger.toInt()
    }

}