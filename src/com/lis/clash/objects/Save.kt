package com.lis.clash.objects

import com.lis.clash.ClashAggregateProperty
import com.lis.clash.ClashSignedProperty
import com.lis.clash.ClashSimpleProperty
import com.lis.clash.readLittleEndianInt

class Save : ClashObject(null, 0) {
    companion object {
        const val FILE_HEADER_SIZE = 16
        const val GAME_DATA_SIZE = 586398
        const val EXPECTED_FILE_SIZE = FILE_HEADER_SIZE + GAME_DATA_SIZE
        const val MAP_TILE_COUNT = 10000
        const val ARMY_COUNT = 500
        const val BUILDING_COUNT = 100
        const val OCCUPANCY_EMPTY = 0xFFFF
        const val OCCUPANCY_BUILDING_BASE = 0x8000
    }

    @ClashSimpleProperty(0, 16)
    var name: String by clashProperty("")

    @ClashAggregateProperty(16, MAP_TILE_COUNT, 14, Tile::class)
    var tiles: List<Tile> by clashProperty(emptyList())

    @ClashSimpleProperty(140016, 4)
    var mapWidthTiles: Int by clashProperty(0)

    @ClashSimpleProperty(140020, 4)
    var mapHeightTiles: Int by clashProperty(0)

    @ClashSimpleProperty(140024, 4)
    var mapViewLeft: Int by clashProperty(0)

    @ClashSimpleProperty(140028, 4)
    var mapViewTop: Int by clashProperty(0)

    @ClashSimpleProperty(140032, 1)
    var mapThemeId: Int by clashProperty(0)

    @ClashSignedProperty(140033, 4)
    var activeMissionIndex: Int by clashProperty(0)

    @ClashSimpleProperty(140037, 1)
    var missionFailureFlag: Int by clashProperty(0)

    @ClashSimpleProperty(140038, 2)
    var turnCounter: Int by clashProperty(0)

    @ClashAggregateProperty(140040, 5, 1423, Player::class)
    var players: List<Player> by clashProperty(emptyList())

    @ClashSimpleProperty(147155, 4)
    var turnOwnerPlayerIndex: Int by clashProperty(0)

    @ClashSimpleProperty(147159, 4)
    var viewedPlayerIndex: Int by clashProperty(0)

    @ClashSimpleProperty(147163, 4)
    var transitionAnimationsEnabled: Int by clashProperty(0)

    @ClashSimpleProperty(147167, 4)
    var gridOverlayEnabled: Int by clashProperty(0)

    @ClashSimpleProperty(147171, 4)
    var statusOverlayEnabled: Int by clashProperty(0)

    @ClashSimpleProperty(147175, 4)
    var fastMovementAnimationsEnabled: Int by clashProperty(0)

    @ClashSimpleProperty(147179, 4)
    var musicEnabled: Int by clashProperty(0)

    @ClashSimpleProperty(147183, 4)
    var soundEffectsEnabled: Int by clashProperty(0)

    @ClashSimpleProperty(147187, 1)
    var scrollSpeedRaw: Int by clashProperty(0)

    @ClashSimpleProperty(147188, 1)
    var soundVolumeRaw: Int by clashProperty(0)

    @ClashSignedProperty(147189, 1)
    var musicVolumeRaw: Int by clashProperty(0)

    @ClashAggregateProperty(147190, ARMY_COUNT, 725, Army::class)
    var armies: List<Army> by clashProperty(emptyList())

    @ClashAggregateProperty(509690, BUILDING_COUNT, 467, Castle::class)
    var castles: List<Castle> by clashProperty(emptyList())

    @ClashSimpleProperty(556390, 20000)
    var occupancyLayerRaw: List<Byte> by clashProperty(emptyList())

    @ClashSimpleProperty(576390, 10000)
    var trapOwnerMaskLayer: List<Byte> by clashProperty(emptyList())

    @ClashSignedProperty(586390, 4)
    var portTileRow: Int by clashProperty(0)

    @ClashSignedProperty(586394, 4)
    var portTileColumn: Int by clashProperty(0)

    @ClashSimpleProperty(586398, 4)
    var portNextReinforcementTurn: Int by clashProperty(0)

    @ClashSimpleProperty(586402, 4)
    var portReinforcementReadyFlag: Int by clashProperty(0)

    @ClashSimpleProperty(586406, 4)
    var portReinforcementUnitCount: Int by clashProperty(0)

    @ClashSimpleProperty(586410, 4)
    var portShorelineVariantFlag: Int by clashProperty(0)

    fun occupancyValue(tileRow: Int, tileColumn: Int): Int {
        val index = tileRow * 100 + tileColumn
        require(index in 0 until MAP_TILE_COUNT)
        val byteIndex = index * 2
        return readLittleEndianInt(occupancyLayerRaw.slice(byteIndex until byteIndex + 2))
    }

    fun occupancyAt(tileRow: Int, tileColumn: Int): OccupancyEntry {
        val value = occupancyValue(tileRow, tileColumn)
        return when {
            value == OCCUPANCY_EMPTY -> OccupancyEntry.Empty
            value in 0 until ARMY_COUNT -> OccupancyEntry.Army(value)
            value in OCCUPANCY_BUILDING_BASE until OCCUPANCY_BUILDING_BASE + BUILDING_COUNT ->
                OccupancyEntry.Building(value - OCCUPANCY_BUILDING_BASE)
            else -> OccupancyEntry.Unknown(value)
        }
    }

    fun trapOwnerMask(tileRow: Int, tileColumn: Int): Int {
        val index = tileRow * 100 + tileColumn
        require(index in trapOwnerMaskLayer.indices)
        return trapOwnerMaskLayer[index].toInt() and 0xFF
    }
}

sealed class OccupancyEntry {
    data object Empty : OccupancyEntry()
    data class Army(val armyIndex: Int) : OccupancyEntry()
    data class Building(val buildingIndex: Int) : OccupancyEntry()
    data class Unknown(val rawValue: Int) : OccupancyEntry()
}
