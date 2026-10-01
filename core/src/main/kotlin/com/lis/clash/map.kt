package com.lis.clash

import com.lis.clash.objects.Save
import com.lis.clash.objects.Tile
import java.awt.Color

const val DEFAULT_MAP_WIDTH = 100
const val DEFAULT_TILE_SIZE = 5
const val EMPTY_TILE_ID = 0xFFFF

val EMPTY_TILE_COLOR: Color = Color(24, 24, 24)

fun toIndex(tileRow: Int, tileColumn: Int, mapWidth: Int = DEFAULT_MAP_WIDTH): Int {
    return tileRow * SaveFormat.MAP_STORAGE_STRIDE + tileColumn
}

fun fromIndex(index: Int, mapWidth: Int = DEFAULT_MAP_WIDTH): Pair<Int, Int> {
    return index / SaveFormat.MAP_STORAGE_STRIDE to index % SaveFormat.MAP_STORAGE_STRIDE
}

fun tileIndexAt(
    pixelX: Int,
    pixelY: Int,
    tileSize: Int,
    mapWidth: Int,
    mapHeight: Int,
    tileCount: Int
): Int? {
    if (pixelX < 0 || pixelY < 0 || tileSize <= 0 || mapWidth !in 1..100 || mapHeight !in 1..100) {
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
    val hidden: Boolean = false,
    val physicalSlot: Int = -1
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
    // The on-disk MAP_WIDTH field bounds rows; MAP_HEIGHT bounds columns.
    val width = resolvedMapWidth(save.mapHeightTiles)
    val height = resolvedMapHeight(save.mapWidthTiles, width, save.tiles.size)
    val selected = selectedTileIndex.takeIf {
        val (row, column) = fromIndex(it)
        it in save.tiles.indices && row in 0 until height && column in 0 until width
    } ?: -1

    val armies = save.armies.mapNotNull { army ->
        if (army.tileRow !in 0 until height || army.tileColumn !in 0 until width) {
            return@mapNotNull null
        }

        MapObjectMarker(
            type = MapMarkerType.ARMY,
            tileRow = army.tileRow,
            tileColumn = army.tileColumn,
            ownerPlayerIndex = army.ownerPlayerIndex,
            label = "Army ${army.physicalSlot}",
            physicalSlot = army.physicalSlot,
            hidden = army.isHiddenOnWorldMap != 0
        )
    }

    val castles = save.castles.mapNotNull { castle ->
        if (castle.tileRow !in 0 until height || castle.tileColumn !in 0 until width) {
            return@mapNotNull null
        }

        MapObjectMarker(
            type = MapMarkerType.CASTLE,
            tileRow = castle.tileRow,
            tileColumn = castle.tileColumn,
            ownerPlayerIndex = castle.ownerPlayerIndex,
            label = castle.displayName.ifBlank { "Castle ${castle.physicalSlot}" },
            physicalSlot = castle.physicalSlot
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
    return rawMapWidth.takeIf { it in 1..SaveFormat.MAP_STORAGE_STRIDE } ?: DEFAULT_MAP_WIDTH
}

fun resolvedMapHeight(rawMapHeight: Int, mapWidth: Int, tileCount: Int): Int {
    if (rawMapHeight in 1..SaveFormat.MAP_STORAGE_STRIDE) {
        return rawMapHeight
    }

    val width = SaveFormat.MAP_STORAGE_STRIDE
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
