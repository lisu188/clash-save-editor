package com.lis.clash.editor

/** Lossless CLIPS fact framing. ISO-8859-1 is a byte mapping, not a claim about game text encoding. */
class FacDocument private constructor(private val text: String, val forms: List<FactForm>) {
    data class FactForm(val start: Int, val end: Int, val atoms: List<String>, val nested: Boolean)
    fun toByteArray(): ByteArray = text.toByteArray(Charsets.ISO_8859_1)

    fun structuralProblem(): String? = forms.firstOrNull { !supported(it) }?.let {
        "FAC dependency '${it.atoms.firstOrNull() ?: "nested form"}' is not supported for structural editing"
    }

    /** Replace only the matching form spans; comments, whitespace and unrelated forms stay byte-identical. */
    fun replace(predicate: (List<String>) -> Boolean, replacement: String?): FacDocument {
        val matches = forms.filter { predicate(it.atoms) }
        var result = text
        matches.asReversed().forEach { result = result.removeRange(it.start, it.end) }
        if (replacement != null) result += (if (result.endsWith("\n") || result.isEmpty()) "" else "\n") + replacement + "\n"
        return parse(result.toByteArray(Charsets.ISO_8859_1))
    }

    fun player(slot: Int, active: Boolean, human: Boolean, intelligence: Int, religion: Int) = replace(
        { it.size == 9 && it[0] == "gameinfo" && it[1] == "gracz" && it[2] == slot.toString() },
        if (active) "(gameinfo gracz $slot komputer ${if (human) 0 else 1} inteligencja $intelligence chrzesc $religion)" else null
    )

    fun site(kind: String, row: Int, column: Int, present: Boolean) = replace(
        { it == listOf(kind, row.toString(), column.toString()) },
        if (present) "($kind $row $column)" else null
    )

    fun removeBuilding(slot: Int): FacDocument = replace({ atoms ->
        (atoms.size == 4 && atoms.take(3) == listOf("zamek", "w", "budowie") && atoms[3] == slot.toString()) ||
        (atoms.size == 3 && atoms.take(2) == listOf("zbudowano", "zamek") && atoms[2] == slot.toString()) ||
        (atoms.size == 4 && atoms[0] == "schemat" && atoms[2] == slot.toString())
    }, null)

    fun buildingOwner(slot: Int, owner: Int): FacDocument {
        val matching = forms.filter { it.atoms.size == 4 && it.atoms[0] == "schemat" && it.atoms[2] == slot.toString() }
        return matching.fold(this) { fac, form -> fac.replace({ it == form.atoms }, "(schemat $owner $slot ${form.atoms[3]})") }
    }

    fun cloneBuilding(source: Int, target: Int): FacDocument {
        val facts = forms.mapNotNull { form ->
            val a = form.atoms
            when {
                a.size == 4 && a.take(3) == listOf("zamek", "w", "budowie") && a[3] == "$source" -> "(zamek w budowie $target)"
                a.size == 3 && a.take(2) == listOf("zbudowano", "zamek") && a[2] == "$source" -> "(zbudowano zamek $target)"
                a.size == 4 && a[0] == "schemat" && a[2] == "$source" -> "(schemat ${a[1]} $target ${a[3]})"
                else -> null
            }
        }
        return facts.fold(this) { fac, fact -> fac.replace({ false }, fact) }
    }

    companion object {
        fun emptyScenario() = parse("(initial-fact)\n(misja -1)\n".toByteArray(Charsets.US_ASCII))

        private fun supported(form: FactForm): Boolean {
            if (form.nested) return false
            val a = form.atoms
            // Noncanonical numeric spellings remain lossless, but cannot bypass exact typed dependency matching.
            fun number(index: Int, range: IntRange): Boolean = a.getOrNull(index)?.let { value ->
                value.toIntOrNull()?.let { it in range && it.toString() == value }
            } == true
            return when (a.firstOrNull()) {
                "initial-fact" -> a.size == 1
                "misja" -> a.size == 2 && a[1] == "-1"
                "gameinfo" -> a.size == 9 && a[1] == "gracz" && a[3] == "komputer" &&
                    a[5] == "inteligencja" && a[7] == "chrzesc" && number(2, 0..4) && number(4, 0..1) && number(6, 0..2) && number(8, 0..1)
                "zamek_place", "swiatynia", "skarb", "pulapka" -> a.size == 3 && number(1, 0..99) && number(2, 0..99)
                "zamek" -> a.size == 4 && a[1] == "w" && a[2] == "budowie" && number(3, 0..99)
                "zbudowano" -> a.size == 3 && a[1] == "zamek" && number(2, 0..99)
                "schemat" -> a.size == 4 && number(1, 0..4) && number(2, 0..99) && number(3, 1..3)
                else -> false
            }
        }

        fun parse(bytes: ByteArray): FacDocument {
            require(bytes.size <= 8 * 1024 * 1024) { "FAC exceeds the 8 MiB safety limit" }
            val text = bytes.toString(Charsets.ISO_8859_1)
            require('\u0000' !in text) { "FAC contains NUL bytes" }
            val forms = mutableListOf<FactForm>()
            var index = 0
            while (index < text.length) {
                when {
                    text[index].isWhitespace() -> index++
                    text[index] == ';' -> { while (index < text.length && text[index] != '\n') index++ }
                    text[index] == '(' -> {
                        val start = index++
                        var depth = 1
                        var nested = false
                        val atoms = mutableListOf<String>()
                        while (index < text.length && depth > 0) {
                            when (val c = text[index]) {
                                ';' -> while (index < text.length && text[index] != '\n') index++
                                '(' -> { nested = true; depth++; index++ }
                                ')' -> { depth--; index++ }
                                '"' -> {
                                    val tokenStart = index++
                                    var closed = false
                                    while (index < text.length) {
                                        if (text[index] == '\\') { index += 2; continue }
                                        if (text[index++] == '"') { closed = true; break }
                                    }
                                    require(closed) { "FAC has an unterminated string at byte $tokenStart" }
                                    if (depth == 1) atoms += text.substring(tokenStart, index)
                                }
                                else -> if (c.isWhitespace()) index++ else {
                                    val tokenStart = index
                                    while (index < text.length && !text[index].isWhitespace() && text[index] !in "();\"") index++
                                    if (depth == 1) atoms += text.substring(tokenStart, index)
                                }
                            }
                            require(depth <= 128) { "FAC nesting exceeds 128 levels" }
                        }
                        require(depth == 0) { "FAC has an unclosed form at byte $start" }
                        require(atoms.isNotEmpty() || nested) { "FAC contains an empty form at byte $start" }
                        forms += FactForm(start, index, atoms.toList(), nested)
                    }
                    else -> error("FAC has unexpected text at byte $index")
                }
            }
            return FacDocument(text, forms.toList())
        }
    }
}
