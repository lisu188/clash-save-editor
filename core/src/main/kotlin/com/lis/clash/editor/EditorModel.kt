package com.lis.clash.editor

enum class RecordKind { SAVE, OPTIONS, TILE, PLAYER, ARMY, BUILDING, ARMY_UNIT, BUILDING_UNIT }

/** A table slot on disk, never an index in a filtered UI list. */
data class RecordId(val kind: RecordKind, val slot: Int = 0, val unitSlot: Int? = null)
enum class IssueSeverity { WARNING, ERROR }
data class ValidationIssue(val severity: IssueSeverity, val message: String, val recordId: RecordId? = null)
data class PropertySnapshot(val name: String, val value: String, val editable: Boolean)
data class TileSnapshot(val slot: Int, val row: Int, val column: Int, val terrain: Int,
    val overlay: Int, val road: Int, val occupancy: Int, val trapMask: Int)
data class PlayerSnapshot(val id: RecordId, val name: String, val active: Boolean,
    val human: Boolean, val intelligence: Int, val religion: Int)
data class UnitSnapshot(val id: RecordId, val type: Int, val health: Int,
    val morale: Int, val actionPoints: Int)
data class EntitySnapshot(val id: RecordId, val name: String, val row: Int, val column: Int,
    val owner: Int, val type: Int, val units: List<UnitSnapshot>)
data class DocumentSnapshot(val name: String, val mapWidth: Int, val mapHeight: Int,
    val tiles: List<TileSnapshot>, val players: List<PlayerSnapshot>,
    val entities: List<EntitySnapshot>, val revision: Long, val dirty: Boolean,
    val issues: List<ValidationIssue>)

sealed interface EditCommand {
    data class SetProperty(val id: RecordId, val property: String, val value: String) : EditCommand
    data class ConfigurePlayer(val slot: Int, val active: Boolean, val human: Boolean,
        val intelligence: Int = 0, val religion: Int = 1, val name: String = "Player") : EditCommand
    data class CreateArmy(val row: Int, val column: Int, val owner: Int, val unitType: Int = 0) : EditCommand
    data class CreateBuilding(val row: Int, val column: Int, val owner: Int,
        val type: Int = 2, val name: String = "Castle") : EditCommand
    data class Clone(val id: RecordId, val row: Int, val column: Int) : EditCommand
    data class Move(val id: RecordId, val row: Int, val column: Int) : EditCommand
    data class Delete(val id: RecordId) : EditCommand
    data class ChangeOwner(val id: RecordId, val owner: Int) : EditCommand
    data class AddUnit(val id: RecordId, val type: Int) : EditCommand
    data class RemoveUnit(val id: RecordId, val unitSlot: Int) : EditCommand
    data class PaintTiles(val slots: List<Int>, val terrain: Int? = null,
        val overlay: Int? = null, val road: Int? = null) : EditCommand
    data class SetTrap(val slot: Int, val ownerMask: Int) : EditCommand
    data class SetTraps(val slots: List<Int>, val ownerMask: Int) : EditCommand
}

/** Fields whose changes require other DAT fields or FAC facts cannot use the scalar editor. */
object PropertyPolicy {
    fun canEditIndependently(kind: RecordKind, property: String): Boolean = property !in when (kind) {
        RecordKind.SAVE -> setOf("mapWidthTiles", "mapHeightTiles", "activeMissionIndex", "turnOwnerPlayerIndex", "viewedPlayerIndex", "portTileRow", "portTileColumn")
        RecordKind.PLAYER -> setOf("isActive", "controllerMode", "aiIntelligence", "religionFlag", "firstCastleIndex", "primaryCastleIndex")
        RecordKind.ARMY, RecordKind.BUILDING -> setOf("tileRow", "tileColumn", "ownerPlayerIndex", "footprintClass", "queuedPathWaypointCount", "isHiddenOnWorldMap", "constructionTurnsRemaining", "constructionLockFlags",
            "activeProductionLicenceSlotIndex", "selectedAddonSlotIndex", "productionTurnsRemaining", "unitLicenceTypeIds", "addonTypeIds")
        RecordKind.ARMY_UNIT, RecordKind.BUILDING_UNIT -> setOf("typeId", "ownerPlayerIndex")
        RecordKind.TILE -> setOf("terrainTileId", "overlayTileId", "roadOrBridgeTileId")
        RecordKind.OPTIONS -> emptySet()
    }
}

/** Proven source IDs only; imported artwork IDs remain losslessly readable. No port lifecycle is synthesized. */
object TileEditingPolicy {
    val terrainIds = setOf(0, 4, 707, 711, 752, 755)
    val overlayIds = (728..739).toSet() + 65535
    val roadIds = (866..876).toSet() + (949..952).toSet() + 65535
    val placementGroundIds = setOf(0, 4, 707, 711)
}
