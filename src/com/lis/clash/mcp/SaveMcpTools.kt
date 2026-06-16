package com.lis.clash.mcp

import com.lis.clash.AggregatePropertyDescriptor
import com.lis.clash.ClashPropertyDescriptor
import com.lis.clash.MaskedPropertyDescriptor
import com.lis.clash.SignedPropertyDescriptor
import com.lis.clash.UnitTypes
import com.lis.clash.fromIndex
import com.lis.clash.getClassDescriptor
import com.lis.clash.objects.Army
import com.lis.clash.objects.Castle
import com.lis.clash.objects.ClashObject
import com.lis.clash.objects.Player
import com.lis.clash.objects.Save
import com.lis.clash.objects.Tile
import com.lis.clash.objects.Unit as ClashUnit
import java.io.File
import kotlin.math.min
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmErasure

internal const val MINIMUM_REPRESENTED_SAVE_SIZE = 514360

internal data class ToolDefinition(
    val name: String,
    val title: String,
    val description: String,
    val inputSchema: Map<String, Any?>
) {
    fun toProtocolMap(): Map<String, Any?> {
        return linkedMapOf(
            "name" to name,
            "title" to title,
            "description" to description,
            "inputSchema" to inputSchema
        )
    }
}

internal data class ToolCallResult(
    val structuredContent: Any?,
    val text: String = Json.stringify(structuredContent),
    val isError: Boolean = false
)

internal class ToolExecutionException(message: String) : RuntimeException(message)

internal class SaveMcpTools {
    private val tools = listOf(
        ToolDefinition(
            name = "save_get_schema",
            title = "Get Clash Save Schema",
            description = "Returns the known binary layout, object paths, aggregate sizes, and editable fields.",
            inputSchema = objectSchema(
                properties = linkedMapOf(
                    "entityType" to stringSchema(
                        "Optional entity type to narrow the schema. Allowed values: Save, Tile, Player, Army, Unit, Castle."
                    )
                )
            )
        ),
        ToolDefinition(
            name = "save_get_overview",
            title = "Inspect Clash Save Overview",
            description = "Loads a save file and Returns high-level metadata, parsed entity counts, and short entity previews.",
            inputSchema = objectSchema(
                properties = linkedMapOf(
                    "path" to stringSchema("Path to a Clash save .dat file."),
                    "limit" to integerSchema("Maximum preview rows per entity type.", minimum = 1, maximum = 100)
                ),
                required = listOf("path")
            )
        ),
        ToolDefinition(
            name = "save_list_entities",
            title = "List Parsed Save Entities",
            description = "Lists parsed tiles, players, armies, castles, army units, or castle units with compact summaries.",
            inputSchema = objectSchema(
                properties = linkedMapOf(
                    "path" to stringSchema("Path to a Clash save .dat file."),
                    "entityType" to stringSchema("One of: tiles, players, armies, castles, armyUnits, castleUnits."),
                    "offset" to integerSchema("Zero-based result offset.", minimum = 0),
                    "limit" to integerSchema("Maximum result count.", minimum = 1, maximum = 500)
                ),
                required = listOf("path", "entityType")
            )
        ),
        ToolDefinition(
            name = "save_read_object",
            title = "Read Parsed Save Object",
            description = "Reads a parsed object by path, such as save, tiles[42], players[0], armies[3].units[0], or castles[0].units[2].",
            inputSchema = objectSchema(
                properties = linkedMapOf(
                    "path" to stringSchema("Path to a Clash save .dat file."),
                    "objectPath" to stringSchema("Object path to inspect."),
                    "includeBytes" to booleanSchema("Whether to include bytes from the object's record."),
                    "byteLimit" to integerSchema("Maximum object bytes to return when includeBytes is true.", minimum = 1, maximum = 4096)
                ),
                required = listOf("path", "objectPath")
            )
        ),
        ToolDefinition(
            name = "save_set_property",
            title = "Set Parsed Save Property",
            description = "Sets one annotated simple property on a parsed object and writes the modified save to outputPath or in place.",
            inputSchema = objectSchema(
                properties = linkedMapOf(
                    "path" to stringSchema("Path to a Clash save .dat file."),
                    "objectPath" to stringSchema("Object path containing the property, for example players[0] or castles[0].units[0]."),
                    "property" to stringSchema("Annotated simple property name to edit."),
                    "value" to linkedMapOf(
                        "description" to "New value. Use a string for string fields, integer for integer fields, or integer array for byte-list fields."
                    ),
                    "outputPath" to stringSchema("Where to write the modified save. Required unless inPlace is true."),
                    "inPlace" to booleanSchema("When true and outputPath is omitted, overwrite path."),
                    "overwrite" to booleanSchema("When true, allow replacing an existing outputPath."),
                    "createBackup" to booleanSchema("When writing in place, create a .bak copy first. Defaults to true.")
                ),
                required = listOf("path", "objectPath", "property", "value")
            )
        ),
        ToolDefinition(
            name = "save_read_bytes",
            title = "Read Raw Save Bytes",
            description = "Reads raw bytes at an absolute file offset for inspecting known or unknown save fields.",
            inputSchema = objectSchema(
                properties = linkedMapOf(
                    "path" to stringSchema("Path to a Clash save .dat file."),
                    "offset" to integerSchema("Absolute byte offset.", minimum = 0),
                    "length" to integerSchema("Number of bytes to read.", minimum = 1, maximum = 4096)
                ),
                required = listOf("path", "offset", "length")
            )
        ),
        ToolDefinition(
            name = "save_write_bytes",
            title = "Write Raw Save Bytes",
            description = "Writes explicit raw bytes at an absolute file offset and saves to outputPath or in place.",
            inputSchema = objectSchema(
                properties = linkedMapOf(
                    "path" to stringSchema("Path to a Clash save .dat file."),
                    "offset" to integerSchema("Absolute byte offset.", minimum = 0),
                    "values" to linkedMapOf(
                        "description" to "Bytes to write, either as an array of integers 0..255 or a string like '01 FF 00'."
                    ),
                    "outputPath" to stringSchema("Where to write the modified save. Required unless inPlace is true."),
                    "inPlace" to booleanSchema("When true and outputPath is omitted, overwrite path."),
                    "overwrite" to booleanSchema("When true, allow replacing an existing outputPath."),
                    "createBackup" to booleanSchema("When writing in place, create a .bak copy first. Defaults to true.")
                ),
                required = listOf("path", "offset", "values")
            )
        )
    )

    fun listTools(): List<ToolDefinition> = tools

    fun call(name: String, arguments: Map<String, Any?>): ToolCallResult {
        return try {
            val structured = when (name) {
                "save_get_schema" -> getSchema(arguments)
                "save_get_overview" -> getOverview(arguments)
                "save_list_entities" -> listEntities(arguments)
                "save_read_object" -> readObject(arguments)
                "save_set_property" -> setProperty(arguments)
                "save_read_bytes" -> readBytes(arguments)
                "save_write_bytes" -> writeBytes(arguments)
                else -> throw ToolExecutionException("Unknown tool: $name")
            }
            ToolCallResult(structuredContent = structured)
        } catch (exception: ToolExecutionException) {
            ToolCallResult(
                structuredContent = linkedMapOf("error" to exception.message),
                text = exception.message ?: "Tool execution failed",
                isError = true
            )
        } catch (exception: Exception) {
            ToolCallResult(
                structuredContent = linkedMapOf("error" to (exception.message ?: exception::class.simpleName)),
                text = exception.message ?: "Tool execution failed",
                isError = true
            )
        }
    }

    private fun getSchema(arguments: Map<String, Any?>): Map<String, Any?> {
        val requestedType = optionalStringArg(arguments, "entityType")?.lowercase()
        val types = linkedMapOf(
            "save" to Save::class,
            "tile" to Tile::class,
            "player" to Player::class,
            "army" to Army::class,
            "unit" to ClashUnit::class,
            "castle" to Castle::class
        )

        val selectedTypes = if (requestedType == null) {
            types
        } else {
            val klass = types[requestedType] ?: throw ToolExecutionException(
                "Unsupported entityType '$requestedType'. Use one of: ${types.keys.joinToString(", ")}."
            )
            linkedMapOf(requestedType to klass)
        }

        return linkedMapOf(
            "minimumRepresentedSaveSize" to MINIMUM_REPRESENTED_SAVE_SIZE,
            "objectPathExamples" to listOf(
                "save",
                "tiles[0]",
                "players[0]",
                "armies[0]",
                "armies[0].units[0]",
                "castles[0]",
                "castles[0].units[0]"
            ),
            "types" to selectedTypes.mapValues { (_, klass) -> schemaForClass(klass) }
        )
    }

    private fun getOverview(arguments: Map<String, Any?>): Map<String, Any?> {
        val loaded = loadSave(arguments)
        val save = loaded.save
        val limit = intArg(arguments, "limit", default = 10).coerceIn(1, 100)

        return linkedMapOf(
            "path" to loaded.file.absolutePath,
            "fileSize" to loaded.originalBytes.size,
            "minimumRepresentedSaveSize" to MINIMUM_REPRESENTED_SAVE_SIZE,
            "saveName" to save.name,
            "map" to linkedMapOf(
                "width" to mapWidthOrDefault(save),
                "height" to mapHeightOrDefault(save),
                "storedWidth" to save.mapWidthTiles,
                "storedHeight" to save.mapHeightTiles
            ),
            "view" to linkedMapOf(
                "left" to save.mapViewLeft,
                "top" to save.mapViewTop,
                "activeMissionIndex" to save.activeMissionIndex,
                "turnOwnerPlayerIndex" to save.turnOwnerPlayerIndex,
                "viewedPlayerIndex" to save.viewedPlayerIndex
            ),
            "counts" to linkedMapOf(
                "tiles" to save.tiles.size,
                "players" to save.players.size,
                "activePlayers" to save.players.count { it.isActive != 0 },
                "armies" to save.armies.size,
                "castles" to save.castles.size,
                "armyUnits" to save.armies.sumOf { it.units.size },
                "castleUnits" to save.castles.sumOf { it.units.size }
            ),
            "players" to save.players.take(limit).mapIndexed { index, player -> playerSummary(index, player) },
            "armies" to save.armies.take(limit).mapIndexed { index, army -> armySummary(index, army, save) },
            "castles" to save.castles.take(limit).mapIndexed { index, castle -> castleSummary(index, castle) }
        )
    }

    private fun listEntities(arguments: Map<String, Any?>): Map<String, Any?> {
        val loaded = loadSave(arguments)
        val save = loaded.save
        val entityType = normalizeEntityType(stringArg(arguments, "entityType"))
        val offset = intArg(arguments, "offset", default = 0).coerceAtLeast(0)
        val limit = intArg(arguments, "limit", default = 50).coerceIn(1, 500)

        val items = when (entityType) {
            "tiles" -> save.tiles.mapIndexed { index, tile -> tileSummary(index, tile, save) }
            "players" -> save.players.mapIndexed { index, player -> playerSummary(index, player) }
            "armies" -> save.armies.mapIndexed { index, army -> armySummary(index, army, save) }
            "castles" -> save.castles.mapIndexed { index, castle -> castleSummary(index, castle) }
            "armyUnits" -> save.armies.flatMapIndexed { armyIndex, army ->
                army.units.mapIndexed { slotIndex, unit -> armyUnitSummary(armyIndex, slotIndex, army, unit, save) }
            }
            "castleUnits" -> save.castles.flatMapIndexed { castleIndex, castle ->
                castle.units.mapIndexed { slotIndex, unit -> castleUnitSummary(castleIndex, slotIndex, castle, unit) }
            }
            else -> throw ToolExecutionException(
                "Unsupported entityType '$entityType'. Use one of: tiles, players, armies, castles, armyUnits, castleUnits."
            )
        }

        return linkedMapOf(
            "path" to loaded.file.absolutePath,
            "entityType" to entityType,
            "total" to items.size,
            "offset" to offset,
            "limit" to limit,
            "items" to items.drop(offset).take(limit)
        )
    }

    private fun readObject(arguments: Map<String, Any?>): Map<String, Any?> {
        val loaded = loadSave(arguments)
        val objectPath = stringArg(arguments, "objectPath")
        val resolution = resolveObject(loaded.save, objectPath)
        val includeBytes = boolArg(arguments, "includeBytes", default = false)
        val byteLimit = intArg(arguments, "byteLimit", default = 256).coerceIn(1, 4096)
        return describeObject(resolution, includeBytes, byteLimit)
    }

    private fun setProperty(arguments: Map<String, Any?>): Map<String, Any?> {
        val loaded = loadSave(arguments)
        val objectPath = stringArg(arguments, "objectPath")
        val propertyName = stringArg(arguments, "property")
        if (!arguments.containsKey("value")) {
            throw ToolExecutionException("Missing required argument: value")
        }

        val resolution = resolveObject(loaded.save, objectPath)
        val descriptor = getClassDescriptor(resolution.obj::class).getSimpleProperty(propertyName)
            ?: throw ToolExecutionException("Property '$propertyName' is not an editable simple property on ${typeName(resolution.obj)}.")

        val before = descriptor.get(resolution.obj).asSerializableValue()
        val convertedValue = convertPropertyValue(arguments["value"], descriptor.get(resolution.obj), propertyName)
        descriptor.set(resolution.obj, convertedValue)
        val after = descriptor.get(resolution.obj).asSerializableValue()

        val writeResult = writeSaveOutput(loaded.file, loaded.save.bytes.toByteArray(), arguments)
        return linkedMapOf(
            "path" to loaded.file.absolutePath,
            "objectPath" to resolution.normalizedPath,
            "property" to propertyName,
            "absoluteOffset" to absoluteByteIndex(resolution.obj) + descriptor.index(),
            "length" to descriptor.length(),
            "before" to before,
            "after" to after,
            "write" to writeResult
        )
    }

    private fun readBytes(arguments: Map<String, Any?>): Map<String, Any?> {
        val loaded = loadSave(arguments)
        val offset = intArg(arguments, "offset")
        val length = intArg(arguments, "length").coerceIn(1, 4096)
        if (offset < 0 || offset + length > loaded.originalBytes.size) {
            throw ToolExecutionException(
                "Byte range [$offset, ${offset + length}) is outside file size ${loaded.originalBytes.size}."
            )
        }
        val bytes = loaded.originalBytes.copyOfRange(offset, offset + length).map { it.toUnsignedInt() }
        return linkedMapOf(
            "path" to loaded.file.absolutePath,
            "offset" to offset,
            "length" to length,
            "hex" to bytes.joinToString(" ") { it.toHex(2) },
            "bytes" to bytes
        )
    }

    private fun writeBytes(arguments: Map<String, Any?>): Map<String, Any?> {
        val loaded = loadSave(arguments)
        val offset = intArg(arguments, "offset")
        val values = bytesArg(arguments, "values")
        if (offset < 0 || offset + values.size > loaded.originalBytes.size) {
            throw ToolExecutionException(
                "Byte range [$offset, ${offset + values.size}) is outside file size ${loaded.originalBytes.size}."
            )
        }

        val edited = loaded.originalBytes.copyOf()
        values.forEachIndexed { index, byte -> edited[offset + index] = byte }
        val before = loaded.originalBytes.copyOfRange(offset, offset + values.size).map { it.toUnsignedInt() }
        val after = values.map { it.toUnsignedInt() }
        val writeResult = writeSaveOutput(loaded.file, edited, arguments)
        return linkedMapOf(
            "path" to loaded.file.absolutePath,
            "offset" to offset,
            "length" to values.size,
            "beforeHex" to before.joinToString(" ") { it.toHex(2) },
            "afterHex" to after.joinToString(" ") { it.toHex(2) },
            "before" to before,
            "after" to after,
            "write" to writeResult
        )
    }

    private fun schemaForClass(klass: KClass<out ClashObject>): Map<String, Any?> {
        val descriptor = getClassDescriptor(klass)
        return linkedMapOf(
            "byteLength" to byteLengthForClass(klass),
            "simpleProperties" to descriptor.getSimpleProperties()
                .sortedBy { it.index() }
                .map(::schemaForSimpleProperty),
            "aggregateProperties" to descriptor.getAggregateProperties()
                .sortedBy { it.index() }
                .map(::schemaForAggregateProperty)
        )
    }

    private fun schemaForSimpleProperty(property: ClashPropertyDescriptor): Map<String, Any?> {
        val schema = linkedMapOf(
            "name" to property.getName(),
            "offset" to property.index(),
            "length" to property.length(),
            "valueType" to (property._property.getter.returnType.jvmErasure.simpleName ?: "unknown"),
            "encoding" to when (property) {
                is SignedPropertyDescriptor -> "signedLittleEndian"
                is MaskedPropertyDescriptor -> "maskedLittleEndian"
                else -> "littleEndianOrFixedString"
            },
            "editable" to true
        )
        if (property is MaskedPropertyDescriptor) {
            schema["mask"] = property.mask()
            schema["maskHex"] = property.mask().toHex(property.length() * 2)
            schema["shift"] = property.shift()
            schema["minimum"] = 0
            schema["maximum"] = property.mask()
        }
        return schema
    }

    private fun schemaForAggregateProperty(property: AggregatePropertyDescriptor): Map<String, Any?> {
        return linkedMapOf(
            "name" to property.getName(),
            "offset" to property.index(),
            "count" to property.count(),
            "entrySize" to property.size(),
            "totalLength" to property.length(),
            "elementType" to (property.getConstructor().returnType.jvmErasure.simpleName ?: "ClashObject")
        )
    }

    private fun describeObject(
        resolution: ObjectResolution,
        includeBytes: Boolean,
        byteLimit: Int
    ): Map<String, Any?> {
        val obj = resolution.obj
        val descriptor = getClassDescriptor(obj::class)
        val absoluteByteIndex = absoluteByteIndex(obj)
        val fields = descriptor.getSimpleProperties()
            .sortedBy { it.index() }
            .map { property ->
                linkedMapOf(
                    "name" to property.getName(),
                    "value" to property.get(obj).asSerializableValue(),
                    "offset" to property.index(),
                    "absoluteOffset" to absoluteByteIndex + property.index(),
                    "length" to property.length(),
                    "valueType" to (property._property.getter.returnType.jvmErasure.simpleName ?: "unknown"),
                    "editable" to true
                )
            }

        val properties = linkedMapOf<String, Any?>()
        fields.forEach { field -> properties[field["name"].toString()] = field["value"] }

        val aggregates = descriptor.getAggregateProperties()
            .sortedBy { it.index() }
            .map { property ->
                @Suppress("UNCHECKED_CAST")
                val children = property.get(obj) as List<ClashObject>
                linkedMapOf(
                    "name" to property.getName(),
                    "offset" to property.index(),
                    "absoluteOffset" to absoluteByteIndex + property.index(),
                    "entrySize" to property.size(),
                    "declaredCount" to property.count(),
                    "parsedCount" to children.size,
                    "elementType" to (property.getConstructor().returnType.jvmErasure.simpleName ?: "ClashObject")
                )
            }

        val result = linkedMapOf<String, Any?>(
            "objectPath" to resolution.normalizedPath,
            "type" to typeName(obj),
            "byteIndex" to obj.index,
            "absoluteByteIndex" to absoluteByteIndex,
            "byteLength" to obj.bytes.size,
            "properties" to properties,
            "fields" to fields,
            "aggregates" to aggregates,
            "derived" to derivedFields(obj, resolution.listIndex)
        )

        if (includeBytes) {
            val count = min(byteLimit, obj.bytes.size)
            val bytes = obj.bytes.take(count).map { it.toUnsignedInt() }
            result["bytes"] = linkedMapOf(
                "offset" to absoluteByteIndex,
                "retunedLength" to count,
                "truncated" to (count < obj.bytes.size),
                "hex" to bytes.joinToString(" ") { it.toHex(2) },
                "values" to bytes
            )
        }

        return result
    }

    private fun derivedFields(obj: ClashObject, listIndex: Int?): Map<String, Any?> {
        return when (obj) {
            is Save -> linkedMapOf(
                "mapWidthOrDefault" to mapWidthOrDefault(obj),
                "mapHeightOrDefault" to mapHeightOrDefault(obj)
            )

            is Tile -> {
                val save = rootSave(obj)
                val tileIndex = listIndex ?: ((absoluteByteIndex(obj) - 16) / 14)
                val (row, column) = fromIndex(tileIndex, mapWidthOrDefault(save))
                linkedMapOf(
                    "tileIndex" to tileIndex,
                    "mapRow" to row,
                    "mapColumn" to column,
                    "terrainTileIdHex" to obj.terrainTileId.toHex(4),
                    "overlayTileIdHex" to obj.overlayTileId.toHex(4),
                    "roadOrBridgeTileIdHex" to obj.roadOrBridgeTileId.toHex(4),
                    "hasOverlay" to obj.hasOverlay(),
                    "hasRoadOrBridge" to obj.hasRoadOrBridge(),
                    "isTemple" to obj.isTemple(),
                    "templeVariant" to obj.templeVariant(),
                    "templeVisitedOrEmpty" to obj.templeVisitedOrEmpty(),
                    "isBuriedTreasure" to obj.isBuriedTreasure()
                )
            }

            is Army -> {
                val save = rootSave(obj)
                linkedMapOf(
                    "tileIndex" to (obj.tileRow * mapWidthOrDefault(save) + obj.tileColumn),
                    "unitCount" to obj.units.size,
                    "queuedPathWaypoints" to obj.queuedPathWaypoints().map { waypoint ->
                        linkedMapOf(
                            "tileRow" to waypoint.tileRow,
                            "tileColumn" to waypoint.tileColumn,
                            "cumulativeCost" to waypoint.cumulativeCost
                        )
                    }
                )
            }

            is ClashUnit -> {
                val metadata = UnitTypes.metadata(obj.typeId)
                linkedMapOf(
                    "unitTypeName" to metadata?.displayName,
                    "unitLocalizedNames" to metadata?.localizedNames,
                    "unitSpriteFolder" to metadata?.folder,
                    "unitCategories" to metadata?.categories?.map { it.name.lowercase() }.orEmpty(),
                    "experienceLevel" to obj.experienceLevel,
                    "experienceProgress" to obj.experienceProgress,
                    "isFullyExperienced" to obj.hasMaximumExperience(),
                    "lowMoraleFlag" to obj.lowMoraleFlag,
                    "hasLowMoraleFlag" to obj.hasLowMoraleFlag(),
                    "moraleBand" to obj.moraleBand(),
                    "fatigueBand" to obj.fatigueBand(),
                    "moraleFatigueProtectedType" to obj.isMoraleFatigueProtectedType(),
                    "stanceBitsHex" to obj.stanceBits.toHex(2),
                    "stateFlagsHex" to obj.stateFlags.toHex(2),
                    "stateBits2Hex" to obj.stateBits2.toHex(2),
                    "auxRuntimeStateHex" to obj.auxRuntimeState.toHex(8)
                )
            }

            is Player -> linkedMapOf(
                "revealedTileCount" to obj.revealedTilesBitset.sumOf { countBits(it) },
                "prisonerTransferQueueEntries" to obj.prisonerTransferQueueEntries().map { entry ->
                    linkedMapOf(
                        "marker" to entry.marker,
                        "isEmpty" to entry.isEmpty,
                        "rawBytes" to entry.rawBytes.map { it.toUnsignedInt() }
                    )
                }
            )

            is Castle -> linkedMapOf(
                "buildingNames" to obj.buildingNames(),
                "addonSlots" to obj.addonSlots().map { slot ->
                    linkedMapOf(
                        "slotIndex" to slot.slotIndex,
                        "typeId" to slot.typeId,
                        "typeName" to slot.displayName
                    )
                },
                "addonTypeNames" to obj.addonTypeNames(),
                "garrisonOrders" to obj.garrisonOrders().map { order ->
                    linkedMapOf(
                        "rawValue" to order.rawValue,
                        "trainCountdown" to order.trainCountdown,
                        "repairCountdown" to order.repairCountdown
                    )
                },
                "prisonerSlots" to obj.prisonerSlots().map { slot ->
                    linkedMapOf(
                        "marker" to slot.marker,
                        "rawBytes" to slot.rawBytes.map { it.toUnsignedInt() }
                    )
                }
            )

            else -> emptyMap()
        }
    }

    private fun tileSummary(index: Int, tile: Tile, save: Save): Map<String, Any?> {
        val (row, column) = fromIndex(index, mapWidthOrDefault(save))
        return linkedMapOf(
            "objectPath" to "tiles[$index]",
            "absoluteByteIndex" to absoluteByteIndex(tile),
            "index" to index,
            "row" to row,
            "column" to column,
            "terrainTileId" to tile.terrainTileId,
            "terrainTileIdHex" to tile.terrainTileId.toHex(4),
            "overlayTileId" to tile.overlayTileId,
            "overlayTileIdHex" to tile.overlayTileId.toHex(4),
            "roadOrBridgeTileId" to tile.roadOrBridgeTileId,
            "roadOrBridgeTileIdHex" to tile.roadOrBridgeTileId.toHex(4),
            "isTemple" to tile.isTemple(),
            "templeVariant" to tile.templeVariant(),
            "templeVisitedOrEmpty" to tile.templeVisitedOrEmpty(),
            "isBuriedTreasure" to tile.isBuriedTreasure()
        )
    }

    private fun playerSummary(index: Int, player: Player): Map<String, Any?> {
        return linkedMapOf(
            "objectPath" to "players[$index]",
            "absoluteByteIndex" to absoluteByteIndex(player),
            "index" to index,
            "displayName" to player.displayName,
            "isActive" to player.isActive,
            "controllerMode" to player.controllerMode,
            "religionFlag" to player.religionFlag,
            "techLevel" to player.techLevel,
            "queenRelationshipState" to player.queenRelationshipState,
            "revealedTileCount" to player.revealedTilesBitset.sumOf { countBits(it) }
        )
    }

    private fun armySummary(index: Int, army: Army, save: Save): Map<String, Any?> {
        return linkedMapOf(
            "objectPath" to "armies[$index]",
            "absoluteByteIndex" to absoluteByteIndex(army),
            "index" to index,
            "tileRow" to army.tileRow,
            "tileColumn" to army.tileColumn,
            "tileIndex" to (army.tileRow * mapWidthOrDefault(save) + army.tileColumn),
            "ownerPlayerIndex" to army.ownerPlayerIndex,
            "facingDirection" to army.facingDirection,
            "unitCount" to army.units.size,
            "queuedPathWaypointCount" to army.queuedPathWaypointCount,
            "isHiddenOnWorldMap" to army.isHiddenOnWorldMap
        )
    }

    private fun castleSummary(index: Int, castle: Castle): Map<String, Any?> {
        return linkedMapOf(
            "objectPath" to "castles[$index]",
            "absoluteByteIndex" to absoluteByteIndex(castle),
            "index" to index,
            "displayName" to castle.displayName,
            "tileRow" to castle.tileRow,
            "tileColumn" to castle.tileColumn,
            "ownerPlayerIndex" to castle.ownerPlayerIndex,
            "appearance" to castle.appearance,
            "footprintClass" to castle.footprintClass,
            "unitCount" to castle.units.size,
            "wallStrength" to castle.wallStrength,
            "storedMoney" to castle.storedMoney,
            "buildingNames" to castle.buildingNames(),
            "addonTypeNames" to castle.addonTypeNames()
        )
    }

    private fun armyUnitSummary(
        armyIndex: Int,
        slotIndex: Int,
        army: Army,
        unit: ClashUnit,
        save: Save
    ): Map<String, Any?> {
        return unitSummary(
            objectPath = "armies[$armyIndex].units[$slotIndex]",
            unit = unit,
            location = linkedMapOf(
                "armyIndex" to armyIndex,
                "slotIndex" to slotIndex,
                "tileRow" to army.tileRow,
                "tileColumn" to army.tileColumn,
                "tileIndex" to (army.tileRow * mapWidthOrDefault(save) + army.tileColumn)
            )
        )
    }

    private fun castleUnitSummary(
        castleIndex: Int,
        slotIndex: Int,
        castle: Castle,
        unit: ClashUnit
    ): Map<String, Any?> {
        return unitSummary(
            objectPath = "castles[$castleIndex].units[$slotIndex]",
            unit = unit,
            location = linkedMapOf(
                "castleIndex" to castleIndex,
                "slotIndex" to slotIndex,
                "tileRow" to castle.tileRow,
                "tileColumn" to castle.tileColumn
            )
        )
    }

    private fun unitSummary(
        objectPath: String,
        unit: ClashUnit,
        location: Map<String, Any?>
    ): Map<String, Any?> {
        val metadata = UnitTypes.metadata(unit.typeId)
        return linkedMapOf(
            "objectPath" to objectPath,
            "absoluteByteIndex" to absoluteByteIndex(unit),
            "location" to location,
            "typeId" to unit.typeId,
            "unitTypeName" to metadata?.displayName,
            "ownerPlayerIndex" to unit.ownerPlayerIndex,
            "currentActionPoints" to unit.currentActionPoints,
            "currentHealthPercent" to unit.currentHealthPercent,
            "fatigue" to unit.fatigue,
            "morale" to unit.morale,
            "experienceLevel" to unit.experienceLevel,
            "experienceProgress" to unit.experienceProgress,
            "isFullyExperienced" to unit.hasMaximumExperience(),
            "hasLowMoraleFlag" to unit.hasLowMoraleFlag(),
            "moraleBand" to unit.moraleBand(),
            "fatigueBand" to unit.fatigueBand(),
            "stanceBitsHex" to unit.stanceBits.toHex(2),
            "stateFlagsHex" to unit.stateFlags.toHex(2),
            "stateBits2Hex" to unit.stateBits2.toHex(2)
        )
    }

    private fun resolveObject(save: Save, objectPath: String): ObjectResolution {
        var path = objectPath.trim()
        if (path == "$" || path == "save" || path.isEmpty()) {
            return ObjectResolution(save, "save", null)
        }
        if (path.startsWith("$.")) {
            path = path.removePrefix("$.")
        }
        if (path.startsWith("save.")) {
            path = path.removePrefix("save.")
        }

        var current: ClashObject = save
        var normalizedPath = "save"
        var listIndex: Int? = null

        path.split(".").filter { it.isNotBlank() }.forEach { segment ->
            val match = OBJECT_SEGMENT.matchEntire(segment)
                ?: throw ToolExecutionException("Invalid object path segment '$segment'. Use aggregate[index], for example armies[0].")
            val aggregateName = match.groupValues[1]
            val index = match.groupValues[2].toInt()
            val aggregate = getClassDescriptor(current::class).getAggregateProperty(aggregateName)
                ?: throw ToolExecutionException("${typeName(current)} has no aggregate property '$aggregateName'.")
            @Suppress("UNCHECKED_CAST")
            val children = aggregate.get(current) as List<ClashObject>
            if (index !in children.indices) {
                throw ToolExecutionException(
                    "Index $index is outside parsed ${typeName(current)}.$aggregateName range 0..${children.lastIndex}."
                )
            }
            current = children[index]
            normalizedPath += ".$aggregateName[$index]"
            listIndex = index
        }

        return ObjectResolution(current, normalizedPath.removePrefix("save."), listIndex)
    }

    private fun loadSave(arguments: Map<String, Any?>): LoadedSave {
        val path = stringArg(arguments, "path")
        val file = File(path).absoluteFile
        if (!file.isFile) {
            throw ToolExecutionException("Save file does not exist: ${file.absolutePath}")
        }

        val bytes = file.readBytes()
        if (bytes.size < MINIMUM_REPRESENTED_SAVE_SIZE) {
            throw ToolExecutionException(
                "File is too small to be a represented Clash save: ${bytes.size} bytes, expected at least $MINIMUM_REPRESENTED_SAVE_SIZE."
            )
        }

        return LoadedSave(file, bytes, Save().withBytes(bytes.toList()))
    }

    private fun writeSaveOutput(
        inputFile: File,
        bytes: ByteArray,
        arguments: Map<String, Any?>
    ): Map<String, Any?> {
        val outputPath = optionalStringArg(arguments, "outputPath")
        val inPlace = boolArg(arguments, "inPlace", default = false)
        val overwrite = boolArg(arguments, "overwrite", default = false)
        val createBackup = boolArg(arguments, "createBackup", default = true)
        val outputFile = when {
            !outputPath.isNullOrBlank() -> File(outputPath).absoluteFile
            inPlace -> inputFile.absoluteFile
            else -> throw ToolExecutionException("Provide outputPath or set inPlace=true to write modified bytes.")
        }

        val sameFile = inputFile.canonicalPath == outputFile.canonicalPath
        val parent = outputFile.parentFile
        if (parent != null && !parent.isDirectory) {
            throw ToolExecutionException("Output directory does not exist: ${parent.absolutePath}")
        }
        if (outputFile.exists() && !sameFile && !overwrite) {
            throw ToolExecutionException("Output file already exists. Set overwrite=true to replace it: ${outputFile.absolutePath}")
        }

        val backupFile = if (sameFile && createBackup) {
            nextBackupFile(inputFile).also { inputFile.copyTo(it, overwrite = false) }
        } else {
            null
        }

        outputFile.writeBytes(bytes)
        return linkedMapOf(
            "outputPath" to outputFile.absolutePath,
            "bytesWritten" to bytes.size,
            "inPlace" to sameFile,
            "backupPath" to backupFile?.absolutePath
        )
    }

    private fun nextBackupFile(inputFile: File): File {
        val directory = inputFile.parentFile ?: File(".")
        val first = File(directory, "${inputFile.name}.bak")
        if (!first.exists()) {
            return first
        }
        for (index in 1..9999) {
            val candidate = File(directory, "${inputFile.name}.bak.$index")
            if (!candidate.exists()) {
                return candidate
            }
        }
        throw ToolExecutionException("Could not choose a backup path for ${inputFile.absolutePath}")
    }

    private fun convertPropertyValue(value: Any?, currentValue: Any?, propertyName: String): Any {
        if (value == null) {
            throw ToolExecutionException("Property '$propertyName' cannot be set to null.")
        }

        return when (currentValue) {
            is String -> value as? String
                ?: throw ToolExecutionException("Property '$propertyName' expects a string value.")

            is Int -> intValue(value, propertyName)
            is Byte -> byteValue(value, propertyName)
            is List<*> -> bytesValue(value, propertyName)
            else -> throw ToolExecutionException("Unsupported property type for '$propertyName': ${currentValue?.let { it::class.simpleName }}")
        }
    }

    private fun intValue(value: Any, propertyName: String): Int {
        return when (value) {
            is Number -> {
                val long = value.toLong()
                if (long < Int.MIN_VALUE || long > Int.MAX_VALUE) {
                    throw ToolExecutionException("Property '$propertyName' is outside Int range: $value")
                }
                long.toInt()
            }

            is String -> value.toIntOrNull()
                ?: throw ToolExecutionException("Property '$propertyName' expects an integer value.")

            else -> throw ToolExecutionException("Property '$propertyName' expects an integer value.")
        }
    }

    private fun byteValue(value: Any, propertyName: String): Byte {
        val int = intValue(value, propertyName)
        if (int !in 0..255) {
            throw ToolExecutionException("Property '$propertyName' byte value must be in range 0..255.")
        }
        return int.toByte()
    }

    private fun bytesValue(value: Any, propertyName: String): List<Byte> {
        return when (value) {
            is List<*> -> value.mapIndexed { index, element ->
                byteValue(element ?: throw ToolExecutionException("Byte list '$propertyName' contains null at index $index."), propertyName)
            }

            is String -> parseByteString(value)
            else -> throw ToolExecutionException("Property '$propertyName' expects an array of bytes or a byte string.")
        }
    }

    private fun bytesArg(arguments: Map<String, Any?>, name: String): ByteArray {
        val value = arguments[name] ?: throw ToolExecutionException("Missing required argument: $name")
        return when (value) {
            is List<*> -> value.mapIndexed { index, element ->
                byteValue(element ?: throw ToolExecutionException("Byte list '$name' contains null at index $index."), name)
            }.toByteArray()

            is String -> parseByteString(value).toByteArray()
            else -> throw ToolExecutionException("Argument '$name' expects an array of bytes or a byte string.")
        }
    }

    private fun normalizeEntityType(value: String): String {
        return when (value.lowercase().replace("_", "").replace("-", "")) {
            "tiles" -> "tiles"
            "players" -> "players"
            "armies" -> "armies"
            "castles" -> "castles"
            "armyunits" -> "armyUnits"
            "castleunits" -> "castleUnits"
            else -> throw ToolExecutionException(
                "Unsupported entityType '$value'. Use one of: tiles, players, armies, castles, armyUnits, castleUnits."
            )
        }
    }

    private fun parseByteString(value: String): List<Byte> {
        val tokens = value.trim()
            .removePrefix("[")
            .removeSuffix("]")
            .split(Regex("[,\\s]+"))
            .filter { it.isNotBlank() }
        if (tokens.isEmpty()) {
            throw ToolExecutionException("Byte string is empty.")
        }

        return tokens.map { token ->
            val trimmed = token.trim()
            val radix = if (
                trimmed.startsWith("0x", ignoreCase = true) ||
                trimmed.any { it.uppercaseChar() in 'A'..'F' } ||
                trimmed.length <= 2
            ) 16 else 10
            val text = trimmed.removePrefix("0x").removePrefix("0X")
            val int = text.toIntOrNull(radix)
                ?: throw ToolExecutionException("Invalid byte token '$token'.")
            if (int !in 0..255) {
                throw ToolExecutionException("Byte token '$token' is outside range 0..255.")
            }
            int.toByte()
        }
    }

    private fun stringArg(arguments: Map<String, Any?>, name: String): String {
        return optionalStringArg(arguments, name) ?: throw ToolExecutionException("Missing required argument: $name")
    }

    private fun optionalStringArg(arguments: Map<String, Any?>, name: String): String? {
        val value = arguments[name] ?: return null
        return value as? String ?: throw ToolExecutionException("Argument '$name' expects a string.")
    }

    private fun intArg(arguments: Map<String, Any?>, name: String, default: Int? = null): Int {
        val value = arguments[name] ?: return default ?: throw ToolExecutionException("Missing required argument: $name")
        return when (value) {
            is Number -> value.toLong().let { long ->
                if (long < Int.MIN_VALUE || long > Int.MAX_VALUE) {
                    throw ToolExecutionException("Argument '$name' is outside Int range: $value")
                }
                long.toInt()
            }

            is String -> value.toIntOrNull() ?: throw ToolExecutionException("Argument '$name' expects an integer.")
            else -> throw ToolExecutionException("Argument '$name' expects an integer.")
        }
    }

    private fun boolArg(arguments: Map<String, Any?>, name: String, default: Boolean): Boolean {
        val value = arguments[name] ?: return default
        return value as? Boolean ?: throw ToolExecutionException("Argument '$name' expects a boolean.")
    }

    private fun absoluteByteIndex(obj: ClashObject): Int {
        var total = 0
        var current: ClashObject? = obj
        while (current != null) {
            total += current.index
            current = current.parent
        }
        return total
    }

    private fun rootSave(obj: ClashObject): Save {
        var current: ClashObject = obj
        while (current.parent != null) {
            current = current.parent!!
        }
        return current as? Save ?: throw ToolExecutionException("Object is not attached to a Save root.")
    }

    private fun typeName(obj: ClashObject): String {
        return obj::class.simpleName ?: "ClashObject"
    }

    private fun byteLengthForClass(klass: KClass<out ClashObject>): Int {
        return when (klass) {
            Save::class -> MINIMUM_REPRESENTED_SAVE_SIZE
            Tile::class -> 14
            Player::class -> 1423
            Army::class -> 725
            ClashUnit::class -> 31
            Castle::class -> 467
            else -> 0
        }
    }

    private fun mapWidthOrDefault(save: Save): Int {
        return save.mapWidthTiles.takeIf { it > 0 } ?: 100
    }

    private fun mapHeightOrDefault(save: Save): Int {
        return save.mapHeightTiles.takeIf { it > 0 } ?: 100
    }

    private fun countBits(byte: Byte): Int {
        return Integer.bitCount(byte.toInt() and 0xFF)
    }

    private fun Any?.asSerializableValue(): Any? = when (this) {
        null -> null
        is Byte -> toUnsignedInt()
        is List<*> -> map { element ->
            when (element) {
                is Byte -> element.toUnsignedInt()
                else -> element
            }
        }

        else -> this
    }

    private fun Byte.toUnsignedInt(): Int {
        return toInt() and 0xFF
    }

    private fun Int.toHex(width: Int): String {
        return "0x" + toUInt().toString(16).uppercase().padStart(width, '0')
    }

    private fun objectSchema(
        properties: Map<String, Any?>,
        required: List<String> = emptyList()
    ): Map<String, Any?> {
        val schema = linkedMapOf<String, Any?>(
            "type" to "object",
            "properties" to properties,
            "additionalProperties" to false
        )
        if (required.isNotEmpty()) {
            schema["required"] = required
        }
        return schema
    }

    private fun stringSchema(description: String): Map<String, Any?> {
        return linkedMapOf("type" to "string", "description" to description)
    }

    private fun integerSchema(description: String, minimum: Int? = null, maximum: Int? = null): Map<String, Any?> {
        val schema = linkedMapOf<String, Any?>("type" to "integer", "description" to description)
        minimum?.let { schema["minimum"] = it }
        maximum?.let { schema["maximum"] = it }
        return schema
    }

    private fun booleanSchema(description: String): Map<String, Any?> {
        return linkedMapOf("type" to "boolean", "description" to description)
    }

    private data class LoadedSave(
        val file: File,
        val originalBytes: ByteArray,
        val save: Save
    )

    private data class ObjectResolution(
        val obj: ClashObject,
        val normalizedPath: String,
        val listIndex: Int?
    )

    private companion object {
        val OBJECT_SEGMENT = Regex("""([A-Za-z_][A-Za-z0-9_]*)\[(\d+)]""")
    }
}
