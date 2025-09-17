package com.lis.clash

import com.lis.clash.objects.Save
import com.lis.clash.objects.Army
import com.lis.clash.objects.Castle
import com.lis.clash.objects.Unit
import kotlin.math.sqrt

annotation class ClashScript

object Scripts {
    /**
     * Count how many tiles of each terrainId you have,
     * so you can verify your TERRAIN enum is complete.
     */
    @ClashScript
    fun dumpTerrainCounts(save: Save): Map<Int, Int> =
        save.tiles
            .groupingBy { it.terrainId }
            .eachCount()

    /**
     * Quickly locate every occupied tile,
     * returning (tileIndex, terrainId, occupantId).
     */
    @ClashScript
    fun findOccupiedTiles(save: Save): List<Triple<Int, Int, Int>> =
        save.tiles
            .mapIndexed { idx, t -> Triple(idx, t.terrainId, t.occupantId) }
            .filter { it.third != 0 && it.third != 0xFFFF }


    /**
     * Find the tile‐indices of every unit of a given typeId
     * (e.g. typeId 0x0002 might be “Archer”).
     */
    @ClashScript
    fun findUnitsByType(save: Save, typeId: Int): List<Int> =
        buildList<Unit> {
            save.armies.forEach { addAll(it.units) }
            save.castles.forEach { addAll(it.units) }
        }
            .filter { it.type.toInt() == typeId }
            .map { it.index }  // or some .tileIndex property if you add one

    /**
     * For a given tile‐index, list its 8 neighbors’ terrainIds.
     * Computes width = sqrt(tileCount).
     */
    @ClashScript
    fun listNeighbors(save: Save, centerIdx: Int): List<Pair<Int, Int>> {
        val count = save.tiles.size
        val width = sqrt(count.toDouble()).toInt()
        val height = count / width
        val x = centerIdx % width
        val y = centerIdx / width
        return sequence {
            for (dy in -1..1) for (dx in -1..1) {
                if (dx == 0 && dy == 0) continue
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until width && ny in 0 until height) {
                    val ni = ny * width + nx
                    yield(nx to save.tiles[ni].terrainId)
                }
            }
        }.toList()
    }

    /**
     * Export all castle positions as (tileIndex, castleType).
     */
    @ClashScript
    fun dumpCastles(save: Save): List<Pair<Int, Int>> {
        val count = save.tiles.size
        val width = sqrt(count.toDouble()).toInt()
        return save.castles.map { castle ->
            val idx = castle.y.toInt() * width + castle.x.toInt()
            idx to castle.type.toInt()
        }
    }
}
