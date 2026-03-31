package com.lis.clash

import com.lis.clash.objects.Army
import com.lis.clash.objects.Castle
import com.lis.clash.objects.ClashObject
import com.lis.clash.objects.Player
import com.lis.clash.objects.Save
import com.lis.clash.objects.Tile
import com.lis.clash.objects.Unit
import java.io.File

annotation class ClashScript

private const val DEFAULT_LIST_LIMIT = 200

object Scripts {
    @ClashScript
    fun exploreAll(save: Save) {
        save.players.forEach { player ->
            player.revealedTilesBitset = List(1300) { 0xFF.toByte() }
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

    @ClashScript
    fun listCargoUnits(save: Save): String {
        return formatLocatedUnits(
            title = "Cargo units",
            units = save.locatedUnits().filter { it.metadata?.hasCategory(UnitCategory.CARGO) == true }
        )
    }

    @ClashScript
    fun listCastleBuildings(save: Save): String {
        if (save.castles.isEmpty()) {
            return "No castles found"
        }

        return save.castles.joinToString("\n") { castle ->
            val buildings = castle.buildingNames().ifEmpty { listOf("none") }
            "Castle(${castle.tileRow},${castle.tileColumn}) owner=${castle.ownerPlayerIndex} walls=${castle.wallStrength} buildings=${buildings.joinToString(",")}"
        }
    }

    @ClashScript
    fun listDamagedUnits(save: Save): String {
        return formatLocatedUnits(
            title = "Damaged units",
            units = save.locatedUnits().filter { it.unit.currentHealthPercent < 100 }
        )
    }

    @ClashScript
    fun listExhaustedUnits(save: Save): String {
        return formatLocatedUnits(
            title = "Exhausted units",
            units = save.locatedUnits().filter { it.unit.fatigue >= 80 }
        )
    }

    @ClashScript
    fun listFlyingUnits(save: Save): String {
        return formatLocatedUnits(
            title = "Flying units",
            units = save.locatedUnits().filter { it.metadata?.hasCategory(UnitCategory.FLYING) == true }
        )
    }

    @ClashScript
    fun listHiddenArmyStacks(save: Save): String {
        val hiddenArmies = save.armies.mapIndexed { armyIndex, army -> armyIndex to army }
            .filter { (_, army) -> army.isHiddenOnWorldMap != 0 }

        if (hiddenArmies.isEmpty()) {
            return "Hidden army stacks: none"
        }

        val lines = hiddenArmies.map { (armyIndex, army) ->
            "army#$armyIndex row=${army.tileRow} column=${army.tileColumn} owner=${army.ownerPlayerIndex} units=${army.units.size} queuedWaypoints=${army.queuedPathWaypointCount}"
        }
        return renderSection("Hidden army stacks", hiddenArmies.size, lines)
    }

    @ClashScript
    fun listLowMoraleUnits(save: Save): String {
        return formatLocatedUnits(
            title = "Low-morale units",
            units = save.locatedUnits().filter { it.unit.morale <= 6 }
        )
    }

    @ClashScript
    fun listQueuedArmyPaths(save: Save): String {
        val queuedArmies = save.armies.mapIndexed { armyIndex, army -> armyIndex to army }
            .filter { (_, army) -> army.queuedPathWaypointCount > 0 }

        if (queuedArmies.isEmpty()) {
            return "Queued army paths: none"
        }

        val lines = queuedArmies.map { (armyIndex, army) ->
            val waypointPreview = army.queuedPathWaypoints()
                .take(5)
                .joinToString(" -> ") { waypoint ->
                    "(${waypoint.tileRow},${waypoint.tileColumn};cost=${waypoint.cumulativeCost})"
                }
                .ifEmpty { "no decoded waypoints" }
            "army#$armyIndex row=${army.tileRow} column=${army.tileColumn} owner=${army.ownerPlayerIndex} hidden=${army.isHiddenOnWorldMap} waypointCount=${army.queuedPathWaypointCount} preview=$waypointPreview"
        }
        return renderSection("Queued army paths", queuedArmies.size, lines)
    }

    @ClashScript
    fun listQueuedPrisonerTransfers(save: Save): String {
        val lines = save.players.mapIndexedNotNull { playerIndex, player ->
            val nonEmptyEntries = player.prisonerTransferQueueEntries().mapIndexedNotNull { entryIndex, entry ->
                if (entry.isEmpty) {
                    null
                } else {
                    "entry#$entryIndex=${entry.rawBytes.toUnsignedSummary()}"
                }
            }
            if (nonEmptyEntries.isEmpty()) {
                null
            } else {
                "player#$playerIndex name=\"${player.displayName}\" ${nonEmptyEntries.joinToString(" ")}"
            }
        }

        if (lines.isEmpty()) {
            return "Queued prisoner transfers: none"
        }

        return renderSection("Queued prisoner transfers", lines.size, lines)
    }

    @ClashScript
    fun listSpecialPersonageUnits(save: Save): String {
        return formatLocatedUnits(
            title = "Special personage units",
            units = save.locatedUnits().filter { it.metadata?.hasCategory(UnitCategory.SPECIAL_PERSONAGE) == true }
        )
    }

    @ClashScript
    fun listTilesWithOverlays(save: Save): String {
        return formatLocatedTiles(
            title = "Tiles with overlays",
            tiles = save.locatedTiles().filter { it.tile.hasOverlay() }
        ) { locatedTile ->
            "terrain=${formatTileId(locatedTile.tile.terrainTileId)} overlay=${formatTileId(locatedTile.tile.overlayTileId)}"
        }
    }

    @ClashScript
    fun listTilesWithRoadsOrBridges(save: Save): String {
        return formatLocatedTiles(
            title = "Tiles with roads or bridges",
            tiles = save.locatedTiles().filter { it.tile.hasRoadOrBridge() }
        ) { locatedTile ->
            "terrain=${formatTileId(locatedTile.tile.terrainTileId)} roadOrBridge=${formatTileId(locatedTile.tile.roadOrBridgeTileId)}"
        }
    }

    @ClashScript
    fun listUnitsWithAuxRuntimeState(save: Save): String {
        return formatLocatedUnits(
            title = "Units with aux runtime state",
            units = save.locatedUnits().filter { it.unit.auxRuntimeState != 0 }
        ) { locatedUnit ->
            "auxRuntimeState=${locatedUnit.unit.auxRuntimeState.toHex(8)}"
        }
    }

    @ClashScript
    fun listUnitsWithStateBits2(save: Save): String {
        return formatLocatedUnits(
            title = "Units with stateBits2",
            units = save.locatedUnits().filter { it.unit.stateBits2 != 0 }
        ) { locatedUnit ->
            "stateBits2=${locatedUnit.unit.stateBits2.toHex(2)}"
        }
    }

    @ClashScript
    fun listUnitsWithStateFlags(save: Save): String {
        return formatLocatedUnits(
            title = "Units with state flags",
            units = save.locatedUnits().filter { it.unit.stateFlags != 0 }
        ) { locatedUnit ->
            "stateFlags=${locatedUnit.unit.stateFlags.toHex(2)}"
        }
    }

    @ClashScript
    fun summarizeArmyStacks(save: Save): String {
        val ownerCounts = save.armies.groupingBy { it.ownerPlayerIndex }.eachCount()
        return buildString {
            appendLine("Army stacks")
            appendLine("total=${save.armies.size}")
            appendLine("hidden=${save.armies.count { it.isHiddenOnWorldMap != 0 }}")
            appendLine("withQueuedPath=${save.armies.count { it.queuedPathWaypointCount > 0 }}")
            appendLine("owners=${ownerCounts.entries.sortedBy { it.key }.joinToString(",") { "p${it.key}=${it.value}" }}")
        }.trimEnd()
    }

    @ClashScript
    fun summarizeCastleAddonFlags(save: Save): String {
        if (save.castles.isEmpty()) {
            return "Castle add-on flags: none"
        }

        val lines = save.castles.mapIndexed { castleIndex, castle ->
            val buildings = castle.buildingNames().ifEmpty { listOf("none") }
            "castle#$castleIndex row=${castle.tileRow} column=${castle.tileColumn} owner=${castle.ownerPlayerIndex} flags=${castle.castleAddonFlags.toHex(2)} buildings=${buildings.joinToString(",")}"
        }
        return renderSection("Castle add-on flags", save.castles.size, lines)
    }

    @ClashScript
    fun summarizeCastleGarrisonOrders(save: Save): String {
        if (save.castles.isEmpty()) {
            return "Castle garrison orders: none"
        }

        val lines = save.castles.mapIndexed { castleIndex, castle ->
            val activeOrders = castle.garrisonOrders().mapIndexedNotNull { slotIndex, order ->
                if (order.rawValue == 0) {
                    null
                } else {
                    "slot#$slotIndex(raw=${order.rawValue.toHex(2)},train=${order.trainCountdown},repair=${order.repairCountdown})"
                }
            }
            "castle#$castleIndex row=${castle.tileRow} column=${castle.tileColumn} activeOrders=${activeOrders.size}${if (activeOrders.isEmpty()) "" else " ${activeOrders.joinToString(" ")}"}"
        }
        return renderSection("Castle garrison orders", save.castles.size, lines)
    }

    @ClashScript
    fun summarizeOverlayTileIds(save: Save): String {
        return summarizeTileField(
            title = "Overlay tile ids",
            ids = save.tiles.mapNotNull { tile -> tile.overlayTileId.takeUnless { it == 0xFFFF } }
        )
    }

    @ClashScript
    fun summarizePlayerExploration(save: Save): String {
        val activePlayers = save.players.mapIndexed { playerIndex, player -> playerIndex to player }
            .filter { (_, player) -> player.isActive != 0 }

        if (activePlayers.isEmpty()) {
            return "Player exploration: no active players"
        }

        val mapTileCount = save.mapWidthOrDefault() * save.mapHeightOrDefault()
        val lines = activePlayers.map { (playerIndex, player) ->
            val revealedTileCount = player.revealedTilesBitset.sumOf { countBits(it) }
            val coverage = if (mapTileCount == 0) 0.0 else revealedTileCount * 100.0 / mapTileCount
            "player#$playerIndex name=\"${player.displayName}\" revealed=$revealedTileCount/${mapTileCount} coverage=${"%.2f".format(coverage)}% queenState=${player.queenRelationshipState.toHex(2)}"
        }
        return renderSection("Player exploration", activePlayers.size, lines)
    }

    @ClashScript
    fun summarizeRoadOrBridgeTileIds(save: Save): String {
        return summarizeTileField(
            title = "Road or bridge tile ids",
            ids = save.tiles.mapNotNull { tile -> tile.roadOrBridgeTileId.takeUnless { it == 0xFFFF } }
        )
    }

    @ClashScript
    fun summarizeTerrainTileIds(save: Save): String {
        return summarizeTileField(
            title = "Terrain tile ids",
            ids = save.tiles.map { it.terrainTileId }
        )
    }

    @ClashScript
    fun summarizeTileCombinations(save: Save): String {
        val counts = save.tiles.groupingBy { tile ->
            Triple(tile.terrainTileId, tile.overlayTileId, tile.roadOrBridgeTileId)
        }.eachCount()

        if (counts.isEmpty()) {
            return "Tile combinations: none"
        }

        val lines = counts.entries
            .sortedWith(compareByDescending<Map.Entry<Triple<Int, Int, Int>, Int>> { it.value }.thenBy { it.key.first }.thenBy { it.key.second }.thenBy { it.key.third })
            .take(50)
            .map { (key, count) ->
                "terrain=${formatTileId(key.first)} overlay=${formatTileId(key.second)} roadOrBridge=${formatTileId(key.third)} count=$count"
            }
        return renderSection("Tile combinations", counts.size, lines, 50)
    }

    @ClashScript
    fun summarizeUnitStateBits2(save: Save): String {
        return summarizeUnitField(
            title = "Unit stateBits2",
            values = save.locatedUnits().map { it.unit.stateBits2 },
            formatter = { value -> value.toHex(2) }
        )
    }

    @ClashScript
    fun summarizeUnitStateFlags(save: Save): String {
        return summarizeUnitField(
            title = "Unit state flags",
            values = save.locatedUnits().map { it.unit.stateFlags },
            formatter = { value -> value.toHex(2) }
        )
    }

    @ClashScript
    fun summarizeUnitTypeCounts(save: Save): String {
        val counts = save.locatedUnits().groupingBy { it.unit.typeId }.eachCount()
        if (counts.isEmpty()) {
            return "Unit type counts: none"
        }

        val lines = counts.entries
            .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
            .map { (typeId, count) ->
                val metadata = UnitTypes.metadata(typeId)
                val label = metadata?.displayName ?: "unknown"
                "type=$typeId name=$label count=$count"
            }
        return renderSection("Unit type counts", counts.size, lines, 50)
    }

    @ClashScript
    fun summarizeWorldViewState(save: Save): String {
        return buildString {
            appendLine("World view state")
            appendLine("saveName=\"${save.name}\"")
            appendLine("mapSize=${save.mapWidthOrDefault()}x${save.mapHeightOrDefault()}")
            appendLine("mapViewLeft=${save.mapViewLeft}")
            appendLine("mapViewTop=${save.mapViewTop}")
            appendLine("activeMissionIndex=${save.activeMissionIndex}")
            appendLine("turnOwnerPlayerIndex=${save.turnOwnerPlayerIndex}")
            appendLine("viewedPlayerIndex=${save.viewedPlayerIndex}")
            appendLine("tiles=${save.tiles.size} armies=${save.armies.size} players=${save.players.size} castles=${save.castles.size}")
        }.trimEnd()
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

        when (this) {
            is Tile -> {
                if (listIndex != null) {
                    val (mapRow, mapColumn) = fromIndex(listIndex, (parent as? Save)?.mapWidthOrDefault() ?: 100)
                    result["mapIndex"] = listIndex
                    result["mapRow"] = mapRow
                    result["mapColumn"] = mapColumn
                }
                result["terrainTileIdHex"] = terrainTileId.toHex(4)
                result["overlayTileIdHex"] = overlayTileId.toHex(4)
                result["roadOrBridgeTileIdHex"] = roadOrBridgeTileId.toHex(4)
                result["hasOverlay"] = hasOverlay()
                result["hasRoadOrBridge"] = hasRoadOrBridge()
            }

            is Unit -> {
                UnitTypes.metadata(typeId)?.let { metadata ->
                    result["unitTypeName"] = metadata.displayName
                    result["unitLocalizedNames"] = metadata.localizedNames
                    result["unitSpriteFolder"] = metadata.folder
                    result["unitCategories"] = metadata.categories.map { it.name.lowercase() }
                }
                result["stanceBitsHex"] = stanceBits.toHex(2)
                result["stateFlagsHex"] = stateFlags.toHex(2)
                result["stateBits2Hex"] = stateBits2.toHex(2)
                result["auxRuntimeStateHex"] = auxRuntimeState.toHex(8)
            }

            is Army -> {
                result["tileIndex"] = toIndex(tileRow, tileColumn, (parent as? Save)?.mapWidthOrDefault() ?: 100)
                result["queuedPathWaypoints"] = queuedPathWaypoints().map { waypoint ->
                    linkedMapOf(
                        "tileRow" to waypoint.tileRow,
                        "tileColumn" to waypoint.tileColumn,
                        "cumulativeCost" to waypoint.cumulativeCost
                    )
                }
            }

            is Player -> {
                result["revealedTileCount"] = revealedTilesBitset.sumOf { countBits(it) }
                result["prisonerTransferQueueEntries"] = prisonerTransferQueueEntries().map { entry ->
                    linkedMapOf<String, Any?>(
                        "marker" to entry.marker,
                        "isEmpty" to entry.isEmpty,
                        "rawBytes" to entry.rawBytes.map { byte -> byte.toInt() }
                    )
                }
            }

            is Castle -> {
                result["buildingNames"] = buildingNames()
                result["garrisonOrders"] = garrisonOrders().map { order ->
                    linkedMapOf<String, Any>(
                        "rawValue" to order.rawValue,
                        "trainCountdown" to order.trainCountdown,
                        "repairCountdown" to order.repairCountdown
                    )
                }
                result["prisonerSlots"] = prisonerSlots().map { slot ->
                    linkedMapOf<String, Any>(
                        "marker" to slot.marker,
                        "rawBytes" to slot.rawBytes.map { byte -> byte.toInt() }
                    )
                }
            }
        }

        return result
    }

    private fun Save.locatedUnits(): List<LocatedUnit> {
        val result = mutableListOf<LocatedUnit>()
        armies.forEachIndexed { armyIndex, army ->
            army.units.forEachIndexed { slotIndex, unit ->
                result += LocatedUnit(
                    location = "army#$armyIndex@(${army.tileRow},${army.tileColumn})",
                    slotIndex = slotIndex,
                    ownerPlayerIndex = army.ownerPlayerIndex,
                    unit = unit
                )
            }
        }
        castles.forEachIndexed { castleIndex, castle ->
            castle.units.forEachIndexed { slotIndex, unit ->
                result += LocatedUnit(
                    location = "castle#$castleIndex@(${castle.tileRow},${castle.tileColumn})",
                    slotIndex = slotIndex,
                    ownerPlayerIndex = castle.ownerPlayerIndex,
                    unit = unit
                )
            }
        }
        return result
    }

    private fun Save.locatedTiles(): List<LocatedTile> {
        val mapWidth = mapWidthOrDefault()
        return tiles.mapIndexed { tileIndex, tile ->
            val (tileRow, tileColumn) = fromIndex(tileIndex, mapWidth)
            LocatedTile(tileIndex, tileRow, tileColumn, tile)
        }
    }

    private fun Save.mapWidthOrDefault(): Int {
        return mapWidthTiles.takeIf { it > 0 } ?: 100
    }

    private fun Save.mapHeightOrDefault(): Int {
        return mapHeightTiles.takeIf { it > 0 } ?: 100
    }

    private fun summarizeTileField(title: String, ids: List<Int>): String {
        val counts = ids.groupingBy { it }.eachCount()
        if (counts.isEmpty()) {
            return "$title: none"
        }
        val lines = counts.entries
            .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
            .map { (tileId, count) -> "${formatTileId(tileId)} count=$count" }
        return renderSection(title, counts.size, lines, 50)
    }

    private fun summarizeUnitField(title: String, values: List<Int>, formatter: (Int) -> String): String {
        val counts = values.groupingBy { it }.eachCount()
        if (counts.isEmpty()) {
            return "$title: none"
        }
        val lines = counts.entries
            .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
            .map { (value, count) -> "${formatter(value)} count=$count" }
        return renderSection(title, counts.size, lines, 50)
    }

    private fun formatLocatedUnits(
        title: String,
        units: List<LocatedUnit>,
        extraFormatter: (LocatedUnit) -> String = { "" }
    ): String {
        if (units.isEmpty()) {
            return "$title: none"
        }
        val lines = units.take(DEFAULT_LIST_LIMIT).map { locatedUnit ->
            val metadata = locatedUnit.metadata
            val typeLabel = metadata?.displayName ?: "unknown"
            buildString {
                append(locatedUnit.location)
                append(" slot=")
                append(locatedUnit.slotIndex)
                append(" owner=")
                append(locatedUnit.ownerPlayerIndex)
                append(" type=")
                append(locatedUnit.unit.typeId)
                append("/")
                append(typeLabel)
                append(" ap=")
                append(locatedUnit.unit.currentActionPoints)
                append(" hp=")
                append(locatedUnit.unit.currentHealthPercent)
                append(" fatigue=")
                append(locatedUnit.unit.fatigue)
                append(" morale=")
                append(locatedUnit.unit.morale)
                append(" stance=")
                append(locatedUnit.unit.stanceBits.toHex(2))
                val extra = extraFormatter(locatedUnit)
                if (extra.isNotBlank()) {
                    append(" ")
                    append(extra)
                }
            }
        }
        return renderSection(title, units.size, lines)
    }

    private fun formatLocatedTiles(
        title: String,
        tiles: List<LocatedTile>,
        formatter: (LocatedTile) -> String
    ): String {
        if (tiles.isEmpty()) {
            return "$title: none"
        }

        val lines = tiles.take(DEFAULT_LIST_LIMIT).map { locatedTile ->
            "tile#${locatedTile.index} row=${locatedTile.tileRow} column=${locatedTile.tileColumn} ${formatter(locatedTile)}"
        }
        return renderSection(title, tiles.size, lines)
    }

    private fun renderSection(title: String, totalCount: Int, lines: List<String>, limit: Int = DEFAULT_LIST_LIMIT): String {
        val truncated = if (lines.size > limit) lines.take(limit) else lines
        return buildString {
            appendLine("$title ($totalCount)")
            truncated.forEach { line ->
                appendLine(line)
            }
            if (lines.size > limit) {
                append("... ${lines.size - limit} more")
            }
        }.trimEnd()
    }

    private fun formatTileId(tileId: Int): String {
        return if (tileId == 0xFFFF) {
            "none"
        } else {
            "$tileId (${tileId.toHex(4)})"
        }
    }

    private fun Int.toHex(width: Int): String {
        return "0x" + this.toUInt().toString(16).uppercase().padStart(width, '0')
    }

    private fun List<Byte>.toUnsignedSummary(): String {
        return joinToString(prefix = "[", postfix = "]") { byte -> (byte.toInt() and 0xFF).toString() }
    }

    private fun countBits(byte: Byte): Int {
        return Integer.bitCount(byte.toInt() and 0xFF)
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

    private data class LocatedTile(
        val index: Int,
        val tileRow: Int,
        val tileColumn: Int,
        val tile: Tile
    )

    private data class LocatedUnit(
        val location: String,
        val slotIndex: Int,
        val ownerPlayerIndex: Int,
        val unit: Unit
    ) {
        val metadata: UnitTypeMetadata?
            get() = UnitTypes.metadata(unit.typeId)
    }
}
