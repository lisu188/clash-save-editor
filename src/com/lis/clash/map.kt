package com.lis.clash

import com.lis.clash.objects.Tile
import java.awt.Color
import java.awt.Graphics
import javax.swing.JPanel

fun toIndex(x: Int, y: Int): Int {
    return x * 100 + y
}

fun fromIndex(index: Int): Pair<Int, Int> {
    return index / 100 to index % 100
}

class MapPanel : JPanel() {
    var tiles: () -> List<Tile> = { emptyList() };

    override fun paintComponent(g: Graphics) {
        tiles().forEachIndexed { index, tile ->
            val colorByte = tile.type1.toUByte().toInt()
            val y = fromIndex(index).second
            val x = fromIndex(index).first
            g.color = (Color(colorByte, colorByte, colorByte));
            g.fillRect(x * 5, y * 5, (x + 1) * 5, (y + 1) * 5)
        }
    }
}

