package com.lis.clash

import com.lis.clash.TERRAIN
import com.lis.clash.objects.Army
import com.lis.clash.objects.Castle
import com.lis.clash.objects.ClashObject
import com.lis.clash.objects.Save
import com.lis.clash.objects.Tile
import com.lis.clash.objects.Unit as ClashUnit
import java.io.File
import kotlin.math.sqrt

annotation class ClashScript

object Scripts {
    /**
     * Count how many tiles of each terrainId you have,
     * so you can verify your TERRAIN enum is complete.
     */
    @ClashScript
    fun dumpTerrainCounts(save: Save): Map<Int, Int> =
        save.tiles
            .groupingBy { it.terrainId }
            .eachCount()

    /**
     * Quickly locate every occupied tile,
     * returning (tileIndex, terrainId, occupantId).
     */
    @ClashScript
    fun findOccupiedTiles(save: Save): List<Triple<Int, Int, Int>> =
        save.tiles
            .mapIndexed { idx, t -> Triple(idx, t.terrainId, t.occupantId) }
            .filter { it.third != 0 && it.third != 0xFFFF }

    /**
     * Find the tile-indices of every unit of a given typeId
     * (e.g. typeId 0x0002 might be “Archer”).
     */
    @ClashScript
    fun findUnitsByType(save: Save, typeId: Int): List<Int> =
        buildList<ClashUnit> {
            save.armies.forEach { addAll(it.units) }
            save.castles.forEach { addAll(it.units) }
        }
            .filter { it.type.toInt() == typeId }
            .map { it.index }  // or some .tileIndex property if you add one

    /**
     * For a given tile-index, list its 8 neighbors’ terrainIds.
     * Computes width = sqrt(tileCount).
     */
    @ClashScript
    fun listNeighbors(save: Save, centerIdx: Int): List<Pair<Int, Int>> {
        val count = save.tiles.size
        if (count == 0) return emptyList()
        val width = sqrt(count.toDouble()).toInt()
        val height = count / width
        val x = centerIdx % width
        val y = centerIdx / width
        return sequence {
            for (dy in -1..1) for (dx in -1..1) {
                if (dx == 0 && dy == 0) continue
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until width && ny in 0 until height) {
                    val ni = ny * width + nx
                    yield(nx to save.tiles[ni].terrainId)
                }
            }
        }.toList()
    }

    /**
     * Export all castle positions as (tileIndex, castleType).
     */
    @ClashScript
    fun dumpCastles(save: Save): List<Pair<Int, Int>> {
        if (save.tiles.isEmpty()) return emptyList()
        val width = sqrt(save.tiles.size.toDouble()).toInt()
        return save.castles.map { castle ->
            val idx = castle.y.toInt() * width + castle.x.toInt()
            idx to castle.type.toInt()
        }
    }

    @ClashScript
    fun setTiles(save: Save) {
        save.tiles.forEach { tile ->
            tile.terrainHigh = 0
            tile.terrainLow = 1
            tile.occupantHigh = (-1).toByte()
            tile.occupantLow = (-1).toByte()
        }
    }

    @ClashScript
    fun findTreasures(save: Save): List<Pair<Int, Int>> {
        if (save.tiles.isEmpty()) return emptyList()
        val width = sqrt(save.tiles.size.toDouble()).toInt()
        return save.tiles.mapIndexedNotNull { index, tile ->
            if (tile.terrain == TERRAIN.TREASURE) {
                val x = index % width
                val y = index / width
                x to y
            } else {
                null
            }
        }
    }

    @ClashScript
    fun countTiles(save: Save): Set<Pair<Byte, Byte>> =
        save.tiles
            .map { tile -> tile.occupantHigh to tile.occupantLow }
            .toSortedSet(compareBy<Pair<Byte, Byte>> { it.first }.thenBy { it.second })

    @ClashScript
    fun exploreAll(save: Save) {
        save.players.forEach { player ->
            player.explored = List(1300) { 0xFF.toByte() }
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
        val mapWidth = if (save.tiles.isEmpty()) null else sqrt(save.tiles.size.toDouble()).toInt()
        val saveMap = save.toStructuredMap(mapWidth = mapWidth)
        val json = saveMap.toJsonString()
        val outputFile = File("save-data.json")
        outputFile.writeText(json)
        return outputFile
    }

    private fun ClashObject.toStructuredMap(
        listIndex: Int? = null,
        mapWidth: Int? = null
    ): Map<String, Any?> {
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
                child.toStructuredMap(index, mapWidth)
            }
        }

        result["byteIndex"] = index
        listIndex?.let { result["listIndex"] = it }

        if (this is Tile && listIndex != null) {
            result["mapIndex"] = listIndex
            if (mapWidth != null && mapWidth != 0) {
                result["mapX"] = listIndex % mapWidth
                result["mapY"] = listIndex / mapWidth
            }
            result["tileName"] = terrain?.name
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
