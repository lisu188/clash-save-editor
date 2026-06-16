package com.lis.clash.mcp

internal object Json {
    fun parse(input: String): Any? {
        return Parser(input).parse()
    }

    fun stringify(value: Any?): String {
        return when (value) {
            null -> "null"
            is String -> quote(value)
            is Boolean -> value.toString()
            is Number -> stringifyNumber(value)
            is Map<*, *> -> value.entries.joinToString(prefix = "{", postfix = "}") { entry ->
                val key = entry.key?.toString() ?: error("JSON object keys must not be null")
                "${quote(key)}:${stringify(entry.value)}"
            }

            is Iterable<*> -> value.joinToString(prefix = "[", postfix = "]") { stringify(it) }
            is Array<*> -> value.joinToString(prefix = "[", postfix = "]") { stringify(it) }
            else -> quote(value.toString())
        }
    }

    private fun stringifyNumber(value: Number): String {
        return when (value) {
            is Double -> {
                require(value.isFinite()) { "JSON numbers must be finite" }
                value.toString()
            }

            is Float -> {
                require(value.isFinite()) { "JSON numbers must be finite" }
                value.toString()
            }

            else -> value.toString()
        }
    }

    private fun quote(value: String): String {
        val builder = StringBuilder(value.length + 2)
        builder.append('"')
        value.forEach { char ->
            when (char) {
                '"' -> builder.append("\\\"")
                '\\' -> builder.append("\\\\")
                '\b' -> builder.append("\\b")
                '\u000C' -> builder.append("\\f")
                '\n' -> builder.append("\\n")
                '\r' -> builder.append("\\r")
                '\t' -> builder.append("\\t")
                else -> {
                    if (char.code < 0x20) {
                        builder.append("\\u")
                        builder.append(char.code.toString(16).padStart(4, '0'))
                    } else {
                        builder.append(char)
                    }
                }
            }
        }
        builder.append('"')
        return builder.toString()
    }

    private class Parser(private val input: String) {
        private var index = 0

        fun parse(): Any? {
            val value = parseValue()
            skipWhitespace()
            if (index != input.length) {
                fail("Unexpected trailing content")
            }
            return value
        }

        private fun parseValue(): Any? {
            skipWhitespace()
            if (index >= input.length) {
                fail("Unexpected end of JSON")
            }

            return when (val char = input[index]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't' -> parseLiteral("true", true)
                'f' -> parseLiteral("false", false)
                'n' -> parseLiteral("null", null)
                '-', in '0'..'9' -> parseNumber()
                else -> fail("Unexpected character '$char'")
            }
        }

        private fun parseObject(): Map<String, Any?> {
            expect('{')
            skipWhitespace()
            val result = linkedMapOf<String, Any?>()
            if (consume('}')) {
                return result
            }

            while (true) {
                skipWhitespace()
                if (peek() != '"') {
                    fail("Expected object key")
                }
                val key = parseString()
                skipWhitespace()
                expect(':')
                result[key] = parseValue()
                skipWhitespace()
                if (consume('}')) {
                    return result
                }
                expect(',')
            }
        }

        private fun parseArray(): List<Any?> {
            expect('[')
            skipWhitespace()
            val result = mutableListOf<Any?>()
            if (consume(']')) {
                return result
            }

            while (true) {
                result += parseValue()
                skipWhitespace()
                if (consume(']')) {
                    return result
                }
                expect(',')
            }
        }

        private fun parseString(): String {
            expect('"')
            val builder = StringBuilder()
            while (index < input.length) {
                val char = input[index++]
                when (char) {
                    '"' -> return builder.toString()
                    '\\' -> builder.append(parseEscape())
                    else -> {
                        if (char.code < 0x20) {
                            fail("Unescaped control character in string")
                        }
                        builder.append(char)
                    }
                }
            }
            fail("Unterminated string")
        }

        private fun parseEscape(): Char {
            if (index >= input.length) {
                fail("Unterminated escape sequence")
            }
            return when (val escaped = input[index++]) {
                '"' -> '"'
                '\\' -> '\\'
                '/' -> '/'
                'b' -> '\b'
                'f' -> '\u000C'
                'n' -> '\n'
                'r' -> '\r'
                't' -> '\t'
                'u' -> parseUnicodeEscape()
                else -> fail("Unsupported escape sequence \\$escaped")
            }
        }

        private fun parseUnicodeEscape(): Char {
            if (index + 4 > input.length) {
                fail("Incomplete unicode escape")
            }
            val hex = input.substring(index, index + 4)
            if (!hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
                fail("Invalid unicode escape")
            }
            index += 4
            return hex.toInt(16).toChar()
        }

        private fun parseLiteral(text: String, value: Any?): Any? {
            if (!input.startsWith(text, index)) {
                fail("Expected $text")
            }
            index += text.length
            return value
        }

        private fun parseNumber(): Number {
            val start = index
            consume('-')
            if (consume('0')) {
                // Leading zero is only valid for the single zero digit.
            } else {
                readDigits()
            }

            var floatingPoint = false
            if (consume('.')) {
                floatingPoint = true
                readDigits()
            }
            if (consume('e') || consume('E')) {
                floatingPoint = true
                consume('+') || consume('-')
                readDigits()
            }

            val text = input.substring(start, index)
            return if (floatingPoint) {
                text.toDoubleOrNull() ?: fail("Invalid number")
            } else {
                text.toLongOrNull() ?: fail("Invalid number")
            }
        }

        private fun readDigits() {
            val start = index
            while (index < input.length && input[index] in '0'..'9') {
                index++
            }
            if (start == index) {
                fail("Expected digit")
            }
        }

        private fun skipWhitespace() {
            while (index < input.length && input[index] in listOf(' ', '\t', '\r', '\n')) {
                index++
            }
        }

        private fun peek(): Char? {
            return input.getOrNull(index)
        }

        private fun expect(char: Char) {
            if (!consume(char)) {
                fail("Expected '$char'")
            }
        }

        private fun consume(char: Char): Boolean {
            if (input.getOrNull(index) == char) {
                index++
                return true
            }
            return false
        }

        private fun fail(message: String): Nothing {
            throw IllegalArgumentException("$message at character $index")
        }
    }
}
