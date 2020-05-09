package com.lis.clash

import java.awt.Color
import java.awt.Graphics
import javax.swing.JPanel
import kotlin.math.roundToInt

class MapPanel() : JPanel() {
    var tiles: MutableList<Tile> = mutableListOf()

    override fun paintComponent(g: Graphics) {
        tiles.forEachIndexed { index, tile ->
            val colorByte = tile.type.toUByte().toInt()
            val y = index % 100
            val x = index / 100
            g.color = (Color(colorByte, colorByte, colorByte));
            g.fillRect(x * 5, y * 5, (x + 1) * 5, (y + 1) * 5)
        }
    }
}