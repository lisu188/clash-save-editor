package com.lis.clash

import com.lis.clash.objects.Save

annotation class ClashScript

object Scripts {
    @ClashScript
    fun setTile(save: Save) {
        var x = 14
        var y = 23
        countTiles(save).stream().limit(100).forEach {
            if (y == 33) {
                y = 23
                x++
            }
            val index = toIndex(x, y++)
            save.tiles[index].type1 = 0
            save.tiles[index].type2 = 0
            save.tiles[index].type3 = it.first
            save.tiles[index].type4 = it.second
        }

    }

    @ClashScript
    fun findTreasures(save: Save): List<Pair<Int, Int>> {
        return save.tiles.mapIndexed { index, tile ->
            index to tile
        }.filter { TILE.TREASURE.matches(it.second) }
            .map { fromIndex(it.first) }
    }

    @ClashScript
    fun countTiles(save: Save): Set<Pair<Byte, Byte>> {
        return save.tiles.map { it.type3 to it.type4 }
            .toSortedSet(compareBy<Pair<Byte, Byte>> { it.first }.thenBy { it.second })
    }

    @ClashScript
    fun exploreAll(save: Save) {
        return save.players.forEach {
            it.explored = List(1300) { Integer.valueOf(255).toByte() }
        }
    }
}
