package com.lis.clash

import com.lis.clash.objects.Save
import com.lis.clash.objects.Tile
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel

const val DEFAULT_MAP_WIDTH = 100
const val DEFAULT_TILE_SIZE = 5
const val EMPTY_TILE_ID = 0xFFFF

val EMPTY_TILE_COLOR: Color = Color(24, 24, 24)

fun toIndex(tileRow: Int, tileColumn: Int, mapWidth: Int = DEFAULT_MAP_WIDTH): Int {
    val width = mapWidth.takeIf { it > 0 } ?: DEFAULT_MAP_WIDTH
    return tileRow * width + tileColumn
}

fun fromIndex(index: Int, mapWidth: Int = DEFAULT_MAP_WIDTH): Pair<Int, Int> {
    val width = mapWidth.takeIf { it > 0 } ?: DEFAULT_MAP_WIDTH
    return index / width to index % width
}

fun tileIndexAt(
    pixelX: Int,
    pixelY: Int,
    tileSize: Int,
    mapWidth: Int,
    mapHeight: Int,
    tileCount: Int
): Int? {
    if (pixelX < 0 || pixelY < 0 || tileSize <= 0 || mapWidth <= 0 || mapHeight <= 0) {
        return null
    }

    val tileColumn = pixelX / tileSize
    val tileRow = pixelY / tileSize
    if (tileRow !in 0 until mapHeight || tileColumn !in 0 until mapWidth) {
        return null
    }

    val tileIndex = toIndex(tileRow, tileColumn, mapWidth)
    return tileIndex.takeIf { it in 0 until tileCount }
}

enum class MapMarkerType {
    ARMY,
    CASTLE
}

data class MapObjectMarker(
    val type: MapMarkerType,
    val tileRow: Int,
    val tileColumn: Int,
    val ownerPlayerIndex: Int,
    val label: String,
    val hidden: Boolean = false
)

data class MapRenderModel(
    val mapWidth: Int,
    val mapHeight: Int,
    val tiles: List<Tile>,
    val selectedTileIndex: Int = -1,
    val armies: List<MapObjectMarker> = emptyList(),
    val castles: List<MapObjectMarker> = emptyList()
) {
    fun markersAt(tileRow: Int, tileColumn: Int): List<MapObjectMarker> {
        return (armies + castles).filter { it.tileRow == tileRow && it.tileColumn == tileColumn }
    }
}

fun buildMapRenderModel(save: Save, selectedTileIndex: Int = -1): MapRenderModel {
    val width = resolvedMapWidth(save.mapWidthTiles)
    val height = resolvedMapHeight(save.mapHeightTiles, width, save.tiles.size)
    val selected = selectedTileIndex.takeIf { it in save.tiles.indices } ?: -1

    val armies = save.armies.mapIndexedNotNull { index, army ->
        if (army.tileRow !in 0 until height || army.tileColumn !in 0 until width) {
            return@mapIndexedNotNull null
        }

        MapObjectMarker(
            type = MapMarkerType.ARMY,
            tileRow = army.tileRow,
            tileColumn = army.tileColumn,
            ownerPlayerIndex = army.ownerPlayerIndex,
            label = "Army $index",
            hidden = army.isHiddenOnWorldMap != 0
        )
    }

    val castles = save.castles.mapIndexedNotNull { index, castle ->
        if (castle.tileRow !in 0 until height || castle.tileColumn !in 0 until width) {
            return@mapIndexedNotNull null
        }

        MapObjectMarker(
            type = MapMarkerType.CASTLE,
            tileRow = castle.tileRow,
            tileColumn = castle.tileColumn,
            ownerPlayerIndex = castle.ownerPlayerIndex,
            label = castle.displayName.ifBlank { "Castle $index" }
        )
    }

    return MapRenderModel(
        mapWidth = width,
        mapHeight = height,
        tiles = save.tiles,
        selectedTileIndex = selected,
        armies = armies,
        castles = castles
    )
}

fun resolvedMapWidth(rawMapWidth: Int): Int {
    return rawMapWidth.takeIf { it > 0 } ?: DEFAULT_MAP_WIDTH
}

fun resolvedMapHeight(rawMapHeight: Int, mapWidth: Int, tileCount: Int): Int {
    if (rawMapHeight > 0) {
        return rawMapHeight
    }

    val width = resolvedMapWidth(mapWidth)
    return if (tileCount == 0) 1 else (tileCount + width - 1) / width
}

fun terrainColorFor(terrainTileId: Int): Color {
    if (terrainTileId == EMPTY_TILE_ID) {
        return EMPTY_TILE_COLOR
    }

    val palette = listOf(
        Color(68, 121, 63),
        Color(92, 145, 74),
        Color(139, 130, 81),
        Color(74, 112, 132),
        Color(123, 108, 86),
        Color(98, 122, 101),
        Color(151, 148, 112),
        Color(84, 95, 74)
    )
    return palette[Math.floorMod(terrainTileId / 32, palette.size)]
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
