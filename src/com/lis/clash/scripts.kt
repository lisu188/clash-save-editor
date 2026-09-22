package com.lis.clash

import com.lis.clash.objects.ClashObject
import com.lis.clash.objects.Save
import com.lis.clash.objects.Tile
import java.io.File

annotation class ClashScript

object Scripts {
    @ClashScript
    fun setTiles(save: Save) {
        save.tiles.stream().forEach {
            it.type1 = 0
            it.type2 = 1
            it.type3 = -1
            it.type4 = -1
        }

    }

    @ClashScript
    fun findTreasures(save: Save): List<Pair<Int, Int>> {
        return save.tiles.mapIndexed { index, tile ->
            index to tile
        }.filter { TILE.TREASURE.matches(it.second) }
            .map { fromIndex(it.first) }
    }

    @ClashScript
    fun countTiles(save: Save): Set<Pair<Byte, Byte>> {
        return save.tiles.map { it.type3 to it.type4 }
            .toSortedSet(compareBy<Pair<Byte, Byte>> { it.first }.thenBy { it.second })
    }

    @ClashScript
    fun exploreAll(save: Save) {
        return save.players.forEach {
            it.explored = List(1300) { Integer.valueOf(255).toByte() }
        }
    }

    @ClashScript
    fun exportMapData(save: Save): String {
        val outputFile = writeSaveData(save)
        return "Save data exported to \"${outputFile.absolutePath}\""
    }

    @ClashScript
    fun exportSaveData(save: Save): String {
        val outputFile = writeSaveData(save)
        return "Save data exported to \"${outputFile.absolutePath}\""
    }

    private fun writeSaveData(save: Save): File {
        val saveMap = save.toStructuredMap()
        val json = saveMap.toJsonString()
        val outputFile = File("save-data.json")
        outputFile.writeText(json)
        return outputFile
    }

    private fun ClashObject.toStructuredMap(listIndex: Int? = null): Map<String, Any?> {
        val descriptor = getClassDescriptor(this::class)
        val result = linkedMapOf<String, Any?>()

        descriptor.getSimpleProperties().forEach { property ->
            val value = property.get(this)
            result[property.getName()] = value.asSerializableValue()
        }

        descriptor.getAggregateProperties().forEach { property ->
            @Suppress("UNCHECKED_CAST")
            val children = property.get(this) as List<ClashObject>
            result[property.getName()] = children.mapIndexed { index, child ->
                child.toStructuredMap(index)
            }
        }

        result["byteIndex"] = index
        listIndex?.let { result["listIndex"] = it }

        if (this is Tile && listIndex != null) {
            result["mapIndex"] = listIndex
            result["mapX"] = listIndex / 100
            result["mapY"] = listIndex % 100
            val tileName = TILE.values().firstOrNull { it.matches(this) }?.name
            result["tileName"] = tileName
        }

        return result
    }

    private fun Any?.asSerializableValue(): Any? = when (this) {
        null -> null
        is Byte -> this.toInt()
        is List<*> -> this.map { element ->
            when (element) {
                is Byte -> element.toInt()
                else -> element
            }
        }
        else -> this
    }

    private fun Any?.toJsonString(indentLevel: Int = 0): String = when (this) {
        null -> "null"
        is String -> "\"${escapeJson(this)}\""
        is Number, is Boolean -> this.toString()
        is Map<*, *> -> {
            val entries = this.entries.toList()
            if (entries.isEmpty()) {
                "{}"
            } else {
                buildString {
                    append("{\n")
                    entries.forEachIndexed { index, entry ->
                        appendIndent(indentLevel + 1)
                        val key = entry.key?.toString() ?: "null"
                        append("\"${escapeJson(key)}\": ")
                        append(entry.value.toJsonString(indentLevel + 1))
                        if (index != entries.lastIndex) {
                            append(",")
                        }
                        append("\n")
                    }
                    appendIndent(indentLevel)
                    append("}")
                }
            }
        }
        is Iterable<*> -> {
            val list = this.toList()
            if (list.isEmpty()) {
                "[]"
            } else {
                buildString {
                    append("[\n")
                    list.forEachIndexed { index, element ->
                        appendIndent(indentLevel + 1)
                        append(element.toJsonString(indentLevel + 1))
                        if (index != list.lastIndex) {
                            append(",")
                        }
                        append("\n")
                    }
                    appendIndent(indentLevel)
                    append("]")
                }
            }
        }
        else -> "\"${escapeJson(this.toString())}\""
    }

    private fun escapeJson(value: String): String {
        val builder = StringBuilder()
        value.forEach { char ->
            when (char) {
                '\\' -> builder.append("\\\\")
                '"' -> builder.append("\\\"")
                '\n' -> builder.append("\\n")
                '\r' -> builder.append("\\r")
                '\t' -> builder.append("\\t")
                else -> builder.append(char)
            }
        }
        return builder.toString()
    }

    private fun StringBuilder.appendIndent(indentLevel: Int) {
        repeat(indentLevel) { append("  ") }
    }
}
