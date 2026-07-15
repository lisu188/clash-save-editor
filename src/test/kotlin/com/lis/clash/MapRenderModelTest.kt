package com.lis.clash

import com.lis.clash.objects.Save
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MapRenderModelTest {
    @Test
    fun coordinateConversionUsesRowsForYAndColumnsForX() {
        val tileIndex = toIndex(tileRow = 12, tileColumn = 34, mapWidth = 60)

        assertEquals(754, tileIndex)
        assertEquals(12 to 34, fromIndex(tileIndex, 60))
        assertEquals(754, tileIndexAt(pixelX = 34 * 8 + 2, pixelY = 12 * 8 + 2, tileSize = 8, mapWidth = 60, mapHeight = 40, tileCount = 2400))
        assertNull(tileIndexAt(pixelX = 60 * 8, pixelY = 0, tileSize = 8, mapWidth = 60, mapHeight = 40, tileCount = 2400))
        assertNull(tileIndexAt(pixelX = -1, pixelY = 0, tileSize = 8, mapWidth = 60, mapHeight = 40, tileCount = 2400))
        assertNull(tileIndexAt(pixelX = 0, pixelY = 0, tileSize = 0, mapWidth = 60, mapHeight = 40, tileCount = 2400))
    }

    @Test
    fun renderModelExtractsValidArmiesAndCastles() {
        val save = Save.parse(buildSyntheticMapSave())
        val selectedTileIndex = toIndex(tileRow = 12, tileColumn = 34, mapWidth = 60)

        val model = buildMapRenderModel(save, selectedTileIndex)

        assertEquals(60, model.mapWidth)
        assertEquals(40, model.mapHeight)
        assertEquals(selectedTileIndex, model.selectedTileIndex)

        assertEquals(1, model.armies.size)
        assertEquals(MapMarkerType.ARMY, model.armies.single().type)
        assertEquals(12, model.armies.single().tileRow)
        assertEquals(34, model.armies.single().tileColumn)
        assertEquals(2, model.armies.single().ownerPlayerIndex)
        assertEquals(true, model.armies.single().hidden)

        assertEquals(1, model.castles.size)
        assertEquals(MapMarkerType.CASTLE, model.castles.single().type)
        assertEquals(3, model.castles.single().tileRow)
        assertEquals(4, model.castles.single().tileColumn)
        assertEquals(1, model.castles.single().ownerPlayerIndex)
        assertEquals("Keep", model.castles.single().label)
    }

    @Test
    fun renderModelPreservesValidSelectionAndHandlesEmptyTiles() {
        val save = Save.parse(buildSyntheticMapSave())
        val selectedTileIndex = toIndex(tileRow = 3, tileColumn = 4, mapWidth = 60)

        val model = buildMapRenderModel(save, selectedTileIndex)

        assertEquals(selectedTileIndex, model.selectedTileIndex)
        assertEquals(EMPTY_TILE_ID, model.tiles.first().terrainTileId)
        assertEquals(EMPTY_TILE_COLOR, terrainColorFor(EMPTY_TILE_ID))
        assertEquals(-1, buildMapRenderModel(save, Int.MAX_VALUE).selectedTileIndex)
    }

    private fun buildSyntheticMapSave(): ByteArray {
        val bytes = ByteArray(SaveFormat.DAT_SIZE)

        repeat(SaveFormat.ARMY_RECORD_COUNT) { armyIndex ->
            repeat(10) { slotIndex ->
                writeLittleEndian(
                    bytes,
                    SaveFormat.ARMY_RECORDS_FILE_OFFSET + armyIndex * SaveFormat.ARMY_RECORD_SIZE + 6 + slotIndex * 31,
                    -1,
                    2
                )
            }
        }
        repeat(SaveFormat.BUILDING_RECORD_COUNT) { buildingIndex ->
            val base = SaveFormat.BUILDING_RECORDS_FILE_OFFSET + buildingIndex * SaveFormat.BUILDING_RECORD_SIZE
            writeLittleEndian(bytes, base + 4, -1, 1)
            writeLittleEndian(bytes, base + 16, -1, 2)
            repeat(12) { slotIndex ->
                writeLittleEndian(bytes, base + 18 + slotIndex * 31, -1, 2)
            }
        }
        repeat(SaveFormat.OCCUPANCY_RECORD_COUNT) { index ->
            writeLittleEndian(
                bytes,
                SaveFormat.OCCUPANCY_RECORDS_FILE_OFFSET + index * SaveFormat.OCCUPANCY_RECORD_SIZE,
                SaveFormat.EMPTY_OCCUPANCY,
                2
            )
        }

        writeLittleEndian(bytes, SaveFormat.TILE_RECORDS_FILE_OFFSET, EMPTY_TILE_ID, 2)
        writeLittleEndian(bytes, SaveFormat.TILE_RECORDS_FILE_OFFSET + 2, EMPTY_TILE_ID, 2)
        writeLittleEndian(bytes, SaveFormat.TILE_RECORDS_FILE_OFFSET + 4, EMPTY_TILE_ID, 2)
        writeLittleEndian(bytes, SaveFormat.MAP_WIDTH_FILE_OFFSET, 60, 4)
        writeLittleEndian(bytes, SaveFormat.MAP_HEIGHT_FILE_OFFSET, 40, 4)

        val firstArmy = SaveFormat.ARMY_RECORDS_FILE_OFFSET
        writeLittleEndian(bytes, firstArmy, 12, 2)
        writeLittleEndian(bytes, firstArmy + 2, 34, 2)
        writeLittleEndian(bytes, firstArmy + 4, 2, 1)
        writeLittleEndian(bytes, firstArmy + 6, 5, 2)
        writeLittleEndian(bytes, firstArmy + 720, 1, 1)

        val outOfBoundsArmy = firstArmy + SaveFormat.ARMY_RECORD_SIZE
        writeLittleEndian(bytes, outOfBoundsArmy, 45, 2)
        writeLittleEndian(bytes, outOfBoundsArmy + 2, 1, 2)
        writeLittleEndian(bytes, outOfBoundsArmy + 4, 3, 1)
        writeLittleEndian(bytes, outOfBoundsArmy + 6, 6, 2)

        val firstCastle = SaveFormat.BUILDING_RECORDS_FILE_OFFSET
        writeLittleEndian(bytes, firstCastle, 3, 1)
        writeLittleEndian(bytes, firstCastle + 1, 4, 1)
        writeLittleEndian(bytes, firstCastle + 2, 1, 1)
        writeLittleEndian(bytes, firstCastle + 4, 2, 1)
        writeString(bytes, firstCastle + 5, 11, "Keep")
        writeLittleEndian(bytes, firstCastle + 16, 0, 2)

        val outOfBoundsCastle = firstCastle + SaveFormat.BUILDING_RECORD_SIZE
        writeLittleEndian(bytes, outOfBoundsCastle, 99, 1)
        writeLittleEndian(bytes, outOfBoundsCastle + 1, 4, 1)
        writeLittleEndian(bytes, outOfBoundsCastle + 2, 4, 1)
        writeLittleEndian(bytes, outOfBoundsCastle + 4, 3, 1)
        writeLittleEndian(bytes, outOfBoundsCastle + 16, 0, 2)

        return bytes
    }

    private fun writeString(bytes: ByteArray, offset: Int, length: Int, value: String) {
        val encoded = value.encodeToByteArray()
        repeat(length) { index ->
            bytes[offset + index] = encoded.getOrElse(index) { 0 }
        }
    }

    private fun writeLittleEndian(bytes: ByteArray, offset: Int, value: Int, length: Int) {
        writeLittleEndianInt(value, length).forEachIndexed { index, byte ->
            bytes[offset + index] = byte
        }
    }
}
