package com.lis.clash

import com.lis.clash.objects.Save
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.reflect.full.functions
import kotlin.reflect.full.hasAnnotation

class SaveFormatParsingTest {
    @Test
    fun parsesRecoveredSaveFields() {
        val save = Save().withBytes<Save>(buildSyntheticSave().toList())

        assertEquals("TEST SAVE", save.name)
        assertEquals(60, save.mapWidthTiles)
        assertEquals(40, save.mapHeightTiles)
        assertEquals(7, save.mapViewLeft)
        assertEquals(9, save.mapViewTop)
        assertEquals(-1, save.activeMissionIndex)
        assertEquals(2, save.turnOwnerPlayerIndex)
        assertEquals(3, save.viewedPlayerIndex)

        val tile = save.tiles.first()
        assertEquals(321, tile.terrainTileId)
        assertEquals(654, tile.overlayTileId)
        assertEquals(872, tile.roadOrBridgeTileId)
        assertTrue(tile.hasOverlay())
        assertTrue(tile.hasRoadOrBridge())

        val player = save.players.first()
        assertEquals("Player One", player.displayName)
        assertEquals(1, player.isActive)
        assertEquals(2, player.queenRelationshipState)
        assertEquals(5, player.queenPortraitIndex)
        assertEquals(321, player.queenNextRelationshipCheckTurn)
        assertEquals(listOf(9, 8, 7, 6, 5, 4), player.prisonerTransferQueueEntries().first().rawBytes.map { byte -> byte.toInt() })

        val army = save.armies.single()
        assertEquals(12, army.tileRow)
        assertEquals(34, army.tileColumn)
        assertEquals(1, army.ownerPlayerIndex)
        assertEquals(6, army.facingDirection)
        assertEquals(2, army.queuedPathWaypointCount)
        assertEquals(1, army.isHiddenOnWorldMap)
        assertEquals(
            listOf(
                QueuedPathWaypoint(12, 34, 5),
                QueuedPathWaypoint(13, 36, 8)
            ),
            army.queuedPathWaypoints()
        )

        val unit = army.units.single()
        assertEquals(34, unit.typeId)
        assertEquals(1, unit.ownerPlayerIndex)
        assertEquals(5, unit.currentActionPoints)
        assertEquals(80, unit.currentHealthPercent)
        assertEquals(90, unit.fatigue)
        assertEquals(4, unit.morale)
        assertEquals(0x12, unit.stanceBits)
        assertEquals(0x0E, unit.stateFlags)
        assertEquals(0x11223344, unit.auxRuntimeState)
        assertEquals(0x03, unit.stateBits2)

        val castle = save.castles.single()
        assertEquals(44, castle.tileRow)
        assertEquals(55, castle.tileColumn)
        assertEquals(2, castle.ownerPlayerIndex)
        assertEquals(3, castle.appearance)
        assertEquals(4, castle.footprintClass)
        assertEquals("CastleOne", castle.displayName)
        assertEquals(0x1D, castle.garrisonOrders().first().rawValue)
        assertEquals(5, castle.garrisonOrders().first().trainCountdown)
        assertEquals(3, castle.garrisonOrders().first().repairCountdown)
        assertEquals(listOf(1, 2, 3, 4, 5, 6), castle.prisonerSlots().first().rawBytes.map { byte -> byte.toInt() })
    }

    @Test
    fun exposesExpandedDebugScriptSet() {
        val scriptNames = Scripts::class.functions
            .filter { it.hasAnnotation<ClashScript>() }
            .map { it.name }
            .toSet()

        assertTrue(scriptNames.size >= 20)
        assertTrue("summarizeTerrainTileIds" in scriptNames)
        assertTrue("listQueuedArmyPaths" in scriptNames)
        assertTrue("summarizeWorldViewState" in scriptNames)
        assertTrue("listUnitsWithStateFlags" in scriptNames)
    }

    private fun buildSyntheticSave(): ByteArray {
        val bytes = ByteArray(514360)

        repeat(500) { armyIndex ->
            writeLittleEndian(bytes, 147190 + armyIndex * 725 + 6, 0xFFFF, 2)
        }
        repeat(10) { castleIndex ->
            writeLittleEndian(bytes, 509690 + castleIndex * 467 + 4, 0xFF, 1)
        }

        writeString(bytes, 0, 16, "TEST SAVE")

        writeLittleEndian(bytes, 16, 321, 2)
        writeLittleEndian(bytes, 18, 654, 2)
        writeLittleEndian(bytes, 20, 872, 2)

        writeLittleEndian(bytes, 140000, 60, 4)
        writeLittleEndian(bytes, 140004, 40, 4)
        writeLittleEndian(bytes, 140008, 7, 4)
        writeLittleEndian(bytes, 140012, 9, 4)
        writeLittleEndian(bytes, 140017, -1, 4)

        writeLittleEndian(bytes, 147139, 2, 4)
        writeLittleEndian(bytes, 147143, 3, 4)

        val playerBase = 140040
        writeLittleEndian(bytes, playerBase, 1, 4)
        writeString(bytes, playerBase + 4, 11, "Player One")
        writeLittleEndian(bytes, playerBase + 15, 11, 4)
        writeLittleEndian(bytes, playerBase + 19, 22, 4)
        writeLittleEndian(bytes, playerBase + 23, 1, 4)
        writeLittleEndian(bytes, playerBase + 27, 2, 4)
        writeLittleEndian(bytes, playerBase + 39, 1, 4)
        writeLittleEndian(bytes, playerBase + 47, 4, 1)
        writeLittleEndian(bytes, playerBase + 48, 3, 1)
        writeLittleEndian(bytes, playerBase + 49, 1, 4)
        writeLittleEndian(bytes, playerBase + 53, 2, 4)
        bytes[playerBase + 57] = 0x0F
        listOf(9, 8, 7, 6, 5, 4).forEachIndexed { index, value ->
            writeLittleEndian(bytes, playerBase + 1357 + index, value, 1)
        }
        writeLittleEndian(bytes, playerBase + 1419, 2, 1)
        writeLittleEndian(bytes, playerBase + 1420, 5, 1)
        writeLittleEndian(bytes, playerBase + 1421, 321, 2)

        val armyBase = 147190
        repeat(10) { slotIndex ->
            writeLittleEndian(bytes, armyBase + 6 + slotIndex * 31, 0xFFFF, 2)
        }
        writeLittleEndian(bytes, armyBase + 0, 12, 2)
        writeLittleEndian(bytes, armyBase + 2, 34, 2)
        writeLittleEndian(bytes, armyBase + 4, 1, 1)
        writeLittleEndian(bytes, armyBase + 5, 6, 1)
        writeLittleEndian(bytes, armyBase + 316, 2, 4)
        writeLittleEndian(bytes, armyBase + 320, 12, 1)
        writeLittleEndian(bytes, armyBase + 321, 34, 1)
        writeLittleEndian(bytes, armyBase + 322, 5, 2)
        writeLittleEndian(bytes, armyBase + 324, 13, 1)
        writeLittleEndian(bytes, armyBase + 325, 36, 1)
        writeLittleEndian(bytes, armyBase + 326, 8, 2)
        writeLittleEndian(bytes, armyBase + 720, 1, 1)

        val armyUnitBase = armyBase + 6
        writeLittleEndian(bytes, armyUnitBase + 0, 34, 2)
        writeLittleEndian(bytes, armyUnitBase + 2, 1, 1)
        writeLittleEndian(bytes, armyUnitBase + 8, 5, 1)
        writeLittleEndian(bytes, armyUnitBase + 9, 80, 1)
        writeLittleEndian(bytes, armyUnitBase + 10, 90, 1)
        writeLittleEndian(bytes, armyUnitBase + 11, 4, 1)
        writeLittleEndian(bytes, armyUnitBase + 12, 0x12, 1)
        writeLittleEndian(bytes, armyUnitBase + 13, 0x0E, 1)
        writeLittleEndian(bytes, armyUnitBase + 18, 0x11223344, 4)
        writeLittleEndian(bytes, armyUnitBase + 22, 0x03, 1)

        val castleBase = 509690
        repeat(12) { slotIndex ->
            writeLittleEndian(bytes, castleBase + 18 + slotIndex * 31, 0xFFFF, 2)
        }
        writeLittleEndian(bytes, castleBase + 0, 44, 1)
        writeLittleEndian(bytes, castleBase + 1, 55, 1)
        writeLittleEndian(bytes, castleBase + 2, 2, 1)
        writeLittleEndian(bytes, castleBase + 3, 3, 1)
        writeLittleEndian(bytes, castleBase + 4, 4, 1)
        writeString(bytes, castleBase + 5, 10, "CastleOne")
        writeLittleEndian(bytes, castleBase + 18, 5, 2)
        writeLittleEndian(bytes, castleBase + 20, 2, 1)
        writeLittleEndian(bytes, castleBase + 26, 7, 1)
        writeLittleEndian(bytes, castleBase + 27, 100, 1)
        writeLittleEndian(bytes, castleBase + 28, 10, 1)
        writeLittleEndian(bytes, castleBase + 29, 9, 1)
        writeLittleEndian(bytes, castleBase + 30, 0x05, 1)
        writeLittleEndian(bytes, castleBase + 31, 0x08, 1)
        writeLittleEndian(bytes, castleBase + 36, 0x12345678.toInt(), 4)
        writeLittleEndian(bytes, castleBase + 40, 0x01, 1)
        writeLittleEndian(bytes, castleBase + 390, 0x1D, 1)
        writeLittleEndian(bytes, castleBase + 414, -1, 1)
        writeLittleEndian(bytes, castleBase + 416, 0x13, 1)
        writeLittleEndian(bytes, castleBase + 420, 0x01, 1)
        writeLittleEndian(bytes, castleBase + 421, 9, 1)
        writeLittleEndian(bytes, castleBase + 429, 4, 1)
        writeLittleEndian(bytes, castleBase + 430, 245, 2)
        writeLittleEndian(bytes, castleBase + 434, 77, 1)
        writeLittleEndian(bytes, castleBase + 435, 3, 1)
        writeLittleEndian(bytes, castleBase + 436, 12, 1)
        writeLittleEndian(bytes, castleBase + 438, 54321, 4)
        writeLittleEndian(bytes, castleBase + 444, 5, 1)
        listOf(1, 2, 3, 4, 5, 6).forEachIndexed { index, value ->
            writeLittleEndian(bytes, castleBase + 445 + index, value, 1)
        }
        writeLittleEndian(bytes, castleBase + 463, 77, 4)

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
