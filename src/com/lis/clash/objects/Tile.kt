// src/main/kotlin/com/lis/clash/objects/Tile.kt
package com.lis.clash.objects

import com.lis.clash.ClashSimpleProperty
import com.lis.clash.TERRAIN

class Tile(parent: ClashObject, index: Int) : ClashObject(parent, index) {
    @ClashSimpleProperty(0, 1)
     var terrainHigh: Byte by clashProperty(0)
    @ClashSimpleProperty(1, 1)
     var terrainLow:  Byte by clashProperty(1)

    @ClashSimpleProperty(2, 1)
     var occupantHigh: Byte by clashProperty(2)
    @ClashSimpleProperty(3, 1)
     var occupantLow:  Byte by clashProperty(3)

    var terrainId: Int
        get() = (terrainHigh.toUByte().toInt() shl 8) or terrainLow.toUByte().toInt()
        set(v) {
            terrainHigh = (v ushr 8).toByte()
            terrainLow  = (v and 0xFF).toByte()
        }

    var terrain: TERRAIN?
        get() = TERRAIN.fromId(terrainId)
        set(t) { terrainId = t?.id ?: 0 }

    var occupantId: Int
        get() = (occupantHigh.toUByte().toInt() shl 8) or occupantLow.toUByte().toInt()
        set(v) {
            occupantHigh = (v ushr 8).toByte()
            occupantLow  = (v and 0xFF).toByte()
        }
}
