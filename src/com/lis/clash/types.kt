package com.lis.clash

import com.lis.clash.objects.Tile

enum class UNIT {

}

enum class TILE(val type1: Byte, val type2: Byte) {
    GRASS(0, 0),
    TREASURE(-16, 2);

    fun matches(tile: Tile): Boolean {
        return tile.type1 == type1 && tile.type2 == type2
    }
}