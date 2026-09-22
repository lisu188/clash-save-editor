package com.lis.clash.objects

import com.lis.clash.ClashAggregateProperty
import com.lis.clash.ClashSignedProperty
import com.lis.clash.ClashSimpleProperty
import com.lis.clash.SaveFormat

class Save : ClashObject(null, 0) {
    @ClashSimpleProperty(0, SaveFormat.LABEL_SIZE)
    var name: String by clashProperty("")

    @ClashAggregateProperty(
        SaveFormat.TILE_RECORDS_FILE_OFFSET,
        SaveFormat.TILE_RECORD_COUNT,
        SaveFormat.TILE_RECORD_SIZE,
        Tile::class
    )
    var tiles: List<Tile> by clashProperty(emptyList())

    @ClashSimpleProperty(SaveFormat.MAP_WIDTH_FILE_OFFSET, 4)
    var mapWidthTiles: Int by clashProperty(0)

    @ClashSimpleProperty(SaveFormat.MAP_HEIGHT_FILE_OFFSET, 4)
    var mapHeightTiles: Int by clashProperty(0)

    @ClashSimpleProperty(SaveFormat.MAP_VIEW_LEFT_FILE_OFFSET, 4)
    var mapViewLeft: Int by clashProperty(0)

    @ClashSimpleProperty(SaveFormat.MAP_VIEW_TOP_FILE_OFFSET, 4)
    var mapViewTop: Int by clashProperty(0)

    @ClashSimpleProperty(SaveFormat.MAP_THEME_FILE_OFFSET, 1)
    var mapThemeIndex: Int by clashProperty(0)

    @ClashSignedProperty(SaveFormat.ACTIVE_MISSION_FILE_OFFSET, 4)
    var activeMissionIndex: Int by clashProperty(-1)

    @ClashSimpleProperty(SaveFormat.MISSION_FAILURE_FILE_OFFSET, 1)
    var missionFailureFlag: Int by clashProperty(0)

    @ClashSimpleProperty(SaveFormat.GAME_TURN_COUNTER_FILE_OFFSET, 2)
    var gameTurnCounter: Int by clashProperty(0)

    @ClashAggregateProperty(
        SaveFormat.PLAYER_RECORDS_FILE_OFFSET,
        SaveFormat.PLAYER_RECORD_COUNT,
        SaveFormat.PLAYER_RECORD_SIZE,
        Player::class
    )
    var players: List<Player> by clashProperty(emptyList())

    @ClashSimpleProperty(SaveFormat.TURN_OWNER_FILE_OFFSET, 4)
    var turnOwnerPlayerIndex: Int by clashProperty(0)

    @ClashSimpleProperty(SaveFormat.VIEWED_PLAYER_FILE_OFFSET, 4)
    var viewedPlayerIndex: Int by clashProperty(0)

    @ClashAggregateProperty(
        SaveFormat.ARMY_RECORDS_FILE_OFFSET,
        SaveFormat.ARMY_RECORD_COUNT,
        SaveFormat.ARMY_RECORD_SIZE,
        Army::class
    )
    var armies: List<Army> by clashProperty(emptyList())

    @ClashAggregateProperty(
        SaveFormat.BUILDING_RECORDS_FILE_OFFSET,
        SaveFormat.BUILDING_RECORD_COUNT,
        SaveFormat.BUILDING_RECORD_SIZE,
        Castle::class
    )
    var castles: List<Castle> by clashProperty(emptyList())

    @ClashAggregateProperty(
        SaveFormat.OCCUPANCY_RECORDS_FILE_OFFSET,
        SaveFormat.OCCUPANCY_RECORD_COUNT,
        SaveFormat.OCCUPANCY_RECORD_SIZE,
        TileOccupancy::class
    )
    var tileOccupancy: List<TileOccupancy> by clashProperty(emptyList())

    @ClashAggregateProperty(
        SaveFormat.TRAP_MASK_RECORDS_FILE_OFFSET,
        SaveFormat.TRAP_MASK_RECORD_COUNT,
        SaveFormat.TRAP_MASK_RECORD_SIZE,
        TrapOwnerMask::class
    )
    var trapOwnerMasks: List<TrapOwnerMask> by clashProperty(emptyList())

    @ClashSignedProperty(SaveFormat.PORT_ROW_FILE_OFFSET, 4)
    var portTileRow: Int by clashProperty(-1)

    @ClashSignedProperty(SaveFormat.PORT_COLUMN_FILE_OFFSET, 4)
    var portTileColumn: Int by clashProperty(-1)

    @ClashSimpleProperty(SaveFormat.PORT_NEXT_REINFORCEMENT_TURN_FILE_OFFSET, 4)
    var portNextReinforcementTurn: Int by clashProperty(0)

    @ClashSimpleProperty(SaveFormat.PORT_REINFORCEMENT_READY_FILE_OFFSET, 4)
    var portReinforcementReadyFlag: Int by clashProperty(0)

    @ClashSimpleProperty(SaveFormat.PORT_REINFORCEMENT_UNIT_COUNT_FILE_OFFSET, 4)
    var portPendingReinforcementUnitCount: Int by clashProperty(0)

    @ClashSimpleProperty(SaveFormat.PORT_SHORE_VARIANT_FILE_OFFSET, 4)
    var portShorelineVariantFlag: Int by clashProperty(0)

    companion object {
        fun parse(bytes: ByteArray): Save {
            SaveFormat.requireValidDatSize(bytes.size)
            return Save().withBytes(bytes.toList())
        }
    }
}
