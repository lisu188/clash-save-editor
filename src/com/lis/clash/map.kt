package com.lis.clash

import com.lis.clash.objects.Tile
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel

fun toIndex(tileRow: Int, tileColumn: Int, mapWidth: Int = 100): Int {
    return tileRow * mapWidth + tileColumn
}

fun fromIndex(index: Int, mapWidth: Int = 100): Pair<Int, Int> {
    return index / mapWidth to index % mapWidth
}


class MapPanel : JPanel() {
    var tiles: () -> List<Tile> = { emptyList() }
    var mapWidth: () -> Int = { 100 }
    var mapHeight: () -> Int = { 100 }
    var selectedTileIndex: () -> Int = { -1 }
    var onTileSelected: (Int) -> Unit = {}
    private val tileSize = 5

    init {
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val width = resolvedMapWidth()
                val height = resolvedMapHeight(width)
                val tileRow = e.x / tileSize
                val tileColumn = e.y / tileSize
                if (tileRow in 0 until width && tileColumn in 0 until height) {
                    val tileIndex = toIndex(tileRow, tileColumn, width)
                    if (tileIndex in tiles().indices) {
                        onTileSelected(tileIndex)
                    }
                }
            }
        })
    }

    override fun getPreferredSize(): Dimension {
        val width = resolvedMapWidth()
        val height = resolvedMapHeight(width)
        return Dimension(width * tileSize, height * tileSize)
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val width = resolvedMapWidth()
        val height = resolvedMapHeight(width)
        val highlightedTileIndex = selectedTileIndex()
        tiles().forEachIndexed { index, tile ->
            val (tileRow, tileColumn) = fromIndex(index, width)
            if (tileColumn >= height) {
                return@forEachIndexed
            }

            val x = tileRow * tileSize
            val y = tileColumn * tileSize

            g.color = colorForTerrain(tile.terrainTileId)
            g.fillRect(x, y, tileSize, tileSize)

            if (tile.hasOverlay()) {
                g.color = colorForOverlay(tile.overlayTileId)
                g.fillRect(x + 1, y + 1, (tileSize - 2).coerceAtLeast(1), (tileSize - 2).coerceAtLeast(1))
            }

            if (tile.hasRoadOrBridge()) {
                g.color = colorForRoadOrBridge(tile.roadOrBridgeTileId)
                g.drawLine(x, y + tileSize / 2, x + tileSize - 1, y + tileSize / 2)
                g.drawLine(x + tileSize / 2, y, x + tileSize / 2, y + tileSize - 1)
            }

            if (index == highlightedTileIndex) {
                g.color = Color.RED
                g.drawRect(x, y, tileSize - 1, tileSize - 1)
            }
        }
    }

    private fun resolvedMapWidth(): Int {
        val resolved = mapWidth()
        return if (resolved > 0) resolved else 100
    }

    private fun resolvedMapHeight(width: Int): Int {
        val resolved = mapHeight()
        if (resolved > 0) {
            return resolved
        }
        val tileCount = tiles().size
        return if (tileCount == 0) 1 else (tileCount + width - 1) / width
    }

    private fun colorForTerrain(terrainTileId: Int): Color {
        if (terrainTileId == 0xFFFF) {
            return Color(24, 24, 24)
        }
        val hue = ((terrainTileId * 37) % 360) / 360.0f
        return Color.getHSBColor(hue, 0.35f, 0.82f)
    }

    private fun colorForOverlay(overlayTileId: Int): Color {
        val hue = ((overlayTileId * 53 + 90) % 360) / 360.0f
        return Color.getHSBColor(hue, 0.55f, 0.95f)
    }

    private fun colorForRoadOrBridge(roadOrBridgeTileId: Int): Color {
        val hue = ((roadOrBridgeTileId * 17 + 24) % 60) / 360.0f
        return Color.getHSBColor(hue, 0.85f, 0.9f)
    }
}
