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
}
