package com.lis.clash.objects

import com.lis.clash.ClashSimpleProperty

class Tile(parent: ClashObject, index: Int) : ClashObject(parent, index) {

    @ClashSimpleProperty(0, 2)
    var terrainTileId: Int by clashProperty(0)

    @ClashSimpleProperty(2, 2)
    var overlayTileId: Int by clashProperty(0)

    @ClashSimpleProperty(4, 2)
    var roadOrBridgeTileId: Int by clashProperty(0)

    fun hasOverlay(): Boolean {
        return overlayTileId != 0xFFFF
    }

    fun hasRoadOrBridge(): Boolean {
        return roadOrBridgeTileId != 0xFFFF
    }

    fun isTemple(): Boolean {
        return overlayTileId in TEMPLE_OVERLAY_IDS
    }

    fun templeVariant(): Int? {
        return if (isTemple()) {
            (overlayTileId - TEMPLE_OVERLAY_ID_START) / 2
        } else {
            null
        }
    }

    fun templeVisitedOrEmpty(): Boolean? {
        return if (isTemple()) {
            (overlayTileId - TEMPLE_OVERLAY_ID_START) % 2 == 1
        } else {
            null
        }
    }

    fun isBuriedTreasure(): Boolean {
        return terrainTileId in BURIED_TREASURE_TERRAIN_IDS
    }

    companion object {
        const val TEMPLE_OVERLAY_ID_START = 728
        const val TEMPLE_OVERLAY_ID_END = 739
        val TEMPLE_OVERLAY_IDS = TEMPLE_OVERLAY_ID_START..TEMPLE_OVERLAY_ID_END
        val BURIED_TREASURE_TERRAIN_IDS = setOf(752, 755)
    }
}
