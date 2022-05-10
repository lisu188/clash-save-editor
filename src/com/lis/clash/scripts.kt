package com.lis.clash

import com.lis.clash.objects.Save

annotation class ClashScript

object Scripts {
    @ClashScript
    fun setTiles(save: Save) {
        save.tiles.stream().forEach {
            it.type1 = 0
            it.type2 = 1
            it.type3 = -1
            it.type4 = -1
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
