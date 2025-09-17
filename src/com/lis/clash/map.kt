package com.lis.clash

import com.lis.clash.objects.Tile
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import kotlin.math.sqrt

class MapPanel : JPanel() {
    /** Supplier of the current tile list (must be a perfect square) */
    var tilesSupplier: () -> List<Tile> = { emptyList() }

    /** Size, in pixels, of one tile when drawn */
    var tileSize: Int = 5

    init {
        // Print tile coords and index on click
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val x = e.x / tileSize
                val y = e.y / tileSize
                val idx = y * mapWidth + x
                println("Clicked tile: x=$x, y=$y, index=$idx")
            }
        })
    }

    // Backing fields for width/height computed each paint
    private var mapWidth: Int = 0
    private var mapHeight: Int = 0

    override fun getPreferredSize(): Dimension {
        val t = tilesSupplier()
        if (t.isEmpty()) return super.getPreferredSize()
        // assume square map
        val w = sqrt(t.size.toDouble()).toInt()
        return Dimension(w * tileSize, w * tileSize)
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val tiles = tilesSupplier()
        if (tiles.isEmpty()) return

        // assume square
        mapWidth = sqrt(tiles.size.toDouble()).toInt()
        mapHeight = mapWidth

        tiles.forEachIndexed { index, tile ->
            val x = index % mapWidth
            val y = index / mapWidth

            // simple grayscale by terrainId
            val id = tile.terrainId and 0xFF
            g.color = Color(id, id, id)
            g.fillRect(
                x * tileSize,
                y * tileSize,
                tileSize,
                tileSize
            )
        }
    }
}
