package com.lis.clash

import com.lis.clash.objects.Save
import kotlin.reflect.full.functions
import kotlin.reflect.full.hasAnnotation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SaveFormatParsingTest {
    @Test
    fun parsesCompleteRecoveredSaveLayout() {
        val save = Save.parse(buildSyntheticSave())

        assertEquals(SaveFormat.DAT_SIZE, save.bytes.size)
        assertEquals("TEST SAVE", save.name)
        assertEquals(60, save.mapWidthTiles)
        assertEquals(40, save.mapHeightTiles)
        assertEquals(7, save.mapViewLeft)
        assertEquals(9, save.mapViewTop)
        assertEquals(2, save.mapThemeIndex)
        assertEquals(-1, save.activeMissionIndex)
        assertEquals(1, save.missionFailureFlag)
        assertEquals(321, save.gameTurnCounter)
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
        assertEquals(2, player.aiIntelligence)
        assertEquals(-1, player.queenRelationshipState)
        assertEquals(5, player.queenPortraitIndex)
        assertEquals(321, player.queenNextRelationshipCheckTurn)
        assertEquals(listOf(9, 8, 7, 6, 5, 4), player.prisonerTransferQueueEntries().first().rawBytes.map(Byte::toInt))

        val army = save.armies.single()
        assertEquals(SaveFormat.ARMY_RECORDS_FILE_OFFSET + 2 * SaveFormat.ARMY_RECORD_SIZE, army.index)
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
        assertEquals(2, unit.experienceLevel)
        assertEquals(0, unit.experienceProgress)
        assertEquals(0x0E, unit.stateFlags)
        assertEquals(1, unit.lowMoraleFlag)
        assertTrue(unit.hasLowMoraleFlag())
        assertEquals("low", unit.moraleBand())
        assertEquals("exhausted", unit.fatigueBand())
        assertEquals(0x11223344, unit.auxRuntimeState)
        assertEquals(0x03, unit.stateBits2)

        val castle = save.castles.single()
        assertEquals(SaveFormat.BUILDING_RECORDS_FILE_OFFSET + 3 * SaveFormat.BUILDING_RECORD_SIZE, castle.index)
        assertEquals(44, castle.tileRow)
        assertEquals(55, castle.tileColumn)
        assertEquals(2, castle.ownerPlayerIndex)
        assertEquals(3, castle.appearance)
        assertEquals(2, castle.footprintClass)
        assertEquals("CastleOne", castle.displayName)
        assertEquals(0, castle.constructionTurnsRemaining)
        assertEquals(listOf("Peasant", "Heavy infantry", "Pikeman"), castle.unitLicenceTypeNames())
        assertEquals(-1, castle.activeProductionLicenceSlotIndex)
        assertEquals(6, castle.productionTurnsRemaining)
        assertEquals(0x1D, castle.garrisonOrders().first().rawValue)
        assertEquals(5, castle.garrisonOrders().first().trainCountdown)
        assertEquals(3, castle.garrisonOrders().first().repairCountdown)
        assertEquals(listOf(1, 2, 3, 4, 5, 6), castle.prisonerSlots().first().rawBytes.map(Byte::toInt))

        assertTrue(save.tileOccupancy[0].isEmpty())
        assertEquals(7, save.tileOccupancy[1].armyStackIndex())
        assertNull(save.tileOccupancy[1].buildingIndex())
        assertEquals(3, save.tileOccupancy[2].buildingIndex())
        assertNull(save.tileOccupancy[2].armyStackIndex())

        assertEquals(0b00000101, save.trapOwnerMasks.first().ownerMask)
        assertTrue(save.trapOwnerMasks.first().isKnownToPlayer(0))
        assertFalse(save.trapOwnerMasks.first().isKnownToPlayer(1))
        assertTrue(save.trapOwnerMasks.first().isKnownToPlayer(2))

        assertEquals(14, save.portTileRow)
        assertEquals(22, save.portTileColumn)
        assertEquals(500, save.portNextReinforcementTurn)
        assertEquals(1, save.portReinforcementReadyFlag)
        assertEquals(8, save.portPendingReinforcementUnitCount)
        assertEquals(1, save.portShorelineVariantFlag)
    }

    @Test
    fun rejectsFilesThatAreNotExactDatSize() {
        assertFailsWith<IllegalArgumentException> {
            Save.parse(ByteArray(SaveFormat.DAT_SIZE - 1))
        }
        assertFailsWith<IllegalArgumentException> {
            Save.parse(ByteArray(SaveFormat.DAT_SIZE + 1))
        }
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
        assertTrue("summarizeUnitExperience" in scriptNames)
        assertTrue("listShrines" in scriptNames)
        assertTrue("listBuriedTreasure" in scriptNames)
    }

    @Test
    fun unitExperienceLevelPreservesOtherStanceBits() {
        val unit = Save.parse(buildSyntheticSave()).armies.single().units.single()

        assertEquals(0x12, unit.stanceBits)
        assertEquals(2, unit.experienceLevel)

        unit.experienceLevel = 3
        assertEquals(0x13, unit.stanceBits)
        assertEquals(3, unit.experienceLevel)
        assertEquals(0, unit.experienceProgress)
        assertTrue(unit.hasMaximumExperience())

        unit.experienceProgress = 3
        assertEquals(0x1F, unit.stanceBits)
        assertEquals(3, unit.experienceLevel)
        assertEquals(3, unit.experienceProgress)

        unit.experienceLevel = 0
        assertEquals(0x1C, unit.stanceBits)
        assertEquals(0, unit.experienceLevel)
        assertEquals(3, unit.experienceProgress)
    }

    @Test
    fun unitLowMoraleFlagPreservesOtherStateFlags() {
        val unit = Save.parse(buildSyntheticSave()).armies.single().units.single()

        assertEquals(0x0E, unit.stateFlags)
        assertEquals(1, unit.lowMoraleFlag)

        unit.lowMoraleFlag = 0
        assertEquals(0x0A, unit.stateFlags)
        assertEquals(0, unit.lowMoraleFlag)
        assertFalse(unit.hasLowMoraleFlag())
    }

    @Test
    fun templeOverlayHelpersIdentifyShrines() {
        val tile = Save.parse(buildSyntheticSave()).tiles.first()

        assertFalse(tile.isTemple())
        assertEquals(null, tile.templeVariant())
        assertEquals(null, tile.templeVisitedOrEmpty())

        tile.overlayTileId = 731
        assertTrue(tile.isTemple())
        assertEquals(1, tile.templeVariant())
        assertEquals(true, tile.templeVisitedOrEmpty())

        tile.terrainTileId = 752
        assertTrue(tile.isBuriedTreasure())
    }

    private fun buildSyntheticSave(): ByteArray {
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

        writeString(bytes, 0, SaveFormat.LABEL_SIZE, "TEST SAVE")
        writeLittleEndian(bytes, SaveFormat.TILE_RECORDS_FILE_OFFSET, 321, 2)
        writeLittleEndian(bytes, SaveFormat.TILE_RECORDS_FILE_OFFSET + 2, 654, 2)
        writeLittleEndian(bytes, SaveFormat.TILE_RECORDS_FILE_OFFSET + 4, 872, 2)

        writeLittleEndian(bytes, SaveFormat.MAP_WIDTH_FILE_OFFSET, 60, 4)
        writeLittleEndian(bytes, SaveFormat.MAP_HEIGHT_FILE_OFFSET, 40, 4)
        writeLittleEndian(bytes, SaveFormat.MAP_VIEW_LEFT_FILE_OFFSET, 7, 4)
        writeLittleEndian(bytes, SaveFormat.MAP_VIEW_TOP_FILE_OFFSET, 9, 4)
        writeLittleEndian(bytes, SaveFormat.MAP_THEME_FILE_OFFSET, 2, 1)
        writeLittleEndian(bytes, SaveFormat.ACTIVE_MISSION_FILE_OFFSET, -1, 4)
        writeLittleEndian(bytes, SaveFormat.MISSION_FAILURE_FILE_OFFSET, 1, 1)
        writeLittleEndian(bytes, SaveFormat.GAME_TURN_COUNTER_FILE_OFFSET, 321, 2)
        writeLittleEndian(bytes, SaveFormat.TURN_OWNER_FILE_OFFSET, 2, 4)
        writeLittleEndian(bytes, SaveFormat.VIEWED_PLAYER_FILE_OFFSET, 3, 4)

        val playerBase = SaveFormat.PLAYER_RECORDS_FILE_OFFSET
        writeLittleEndian(bytes, playerBase, 1, 4)
        writeString(bytes, playerBase + 4, 11, "Player One")
        writeLittleEndian(bytes, playerBase + 15, 11, 4)
        writeLittleEndian(bytes, playerBase + 19, 22, 4)
        writeLittleEndian(bytes, playerBase + 23, 1, 4)
        writeLittleEndian(bytes, playerBase + 27, 1, 4)
        writeLittleEndian(bytes, playerBase + 31, 2, 4)
        writeLittleEndian(bytes, playerBase + 39, 1, 4)
        writeLittleEndian(bytes, playerBase + 47, 4, 1)
        writeLittleEndian(bytes, playerBase + 48, 3, 1)
        writeLittleEndian(bytes, playerBase + 49, 1, 4)
        writeLittleEndian(bytes, playerBase + 53, 2, 4)
        bytes[playerBase + 57] = 0x0F
        listOf(9, 8, 7, 6, 5, 4).forEachIndexed { index, value ->
            writeLittleEndian(bytes, playerBase + 1357 + index, value, 1)
        }
        writeLittleEndian(bytes, playerBase + 1419, -1, 1)
        writeLittleEndian(bytes, playerBase + 1420, 5, 1)
        writeLittleEndian(bytes, playerBase + 1421, 321, 2)

        val armyBase = SaveFormat.ARMY_RECORDS_FILE_OFFSET + 2 * SaveFormat.ARMY_RECORD_SIZE
        writeLittleEndian(bytes, armyBase, 12, 2)
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
        writeLittleEndian(bytes, armyUnitBase, 34, 2)
        writeLittleEndian(bytes, armyUnitBase + 2, 1, 1)
        writeLittleEndian(bytes, armyUnitBase + 8, 5, 1)
        writeLittleEndian(bytes, armyUnitBase + 9, 80, 1)
        writeLittleEndian(bytes, armyUnitBase + 10, 90, 1)
        writeLittleEndian(bytes, armyUnitBase + 11, 4, 1)
        writeLittleEndian(bytes, armyUnitBase + 12, 0x12, 1)
        writeLittleEndian(bytes, armyUnitBase + 13, 0x0E, 1)
        writeLittleEndian(bytes, armyUnitBase + 18, 0x11223344, 4)
        writeLittleEndian(bytes, armyUnitBase + 22, 0x03, 1)

        val castleBase = SaveFormat.BUILDING_RECORDS_FILE_OFFSET + 3 * SaveFormat.BUILDING_RECORD_SIZE
        writeLittleEndian(bytes, castleBase, 44, 1)
        writeLittleEndian(bytes, castleBase + 1, 55, 1)
        writeLittleEndian(bytes, castleBase + 2, 2, 1)
        writeLittleEndian(bytes, castleBase + 3, 3, 1)
        writeLittleEndian(bytes, castleBase + 4, 2, 1)
        writeString(bytes, castleBase + 5, 11, "CastleOne")
        writeLittleEndian(bytes, castleBase + 16, 0, 2)
        writeLittleEndian(bytes, castleBase + 18, 5, 2)
        writeLittleEndian(bytes, castleBase + 20, 2, 1)
        writeLittleEndian(bytes, castleBase + 26, 7, 1)
        writeLittleEndian(bytes, castleBase + 27, 100, 1)
        writeLittleEndian(bytes, castleBase + 28, 10, 1)
        writeLittleEndian(bytes, castleBase + 29, 9, 1)
        writeLittleEndian(bytes, castleBase + 30, 0x05, 1)
        writeLittleEndian(bytes, castleBase + 31, 0x08, 1)
        writeLittleEndian(bytes, castleBase + 36, 0x12345678, 4)
        writeLittleEndian(bytes, castleBase + 40, 0x01, 1)
        writeLittleEndian(bytes, castleBase + 390, 0x1D, 1)
        repeat(12) { slotIndex ->
            writeLittleEndian(bytes, castleBase + 402 + slotIndex, 0xFF, 1)
        }
        writeLittleEndian(bytes, castleBase + 402, 0, 1)
        writeLittleEndian(bytes, castleBase + 403, 2, 1)
        writeLittleEndian(bytes, castleBase + 404, 3, 1)
        writeLittleEndian(bytes, castleBase + 414, -1, 1)
        writeLittleEndian(bytes, castleBase + 415, 6, 1)
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

        writeLittleEndian(bytes, SaveFormat.OCCUPANCY_RECORDS_FILE_OFFSET + 2, 7, 2)
        writeLittleEndian(
            bytes,
            SaveFormat.OCCUPANCY_RECORDS_FILE_OFFSET + 4,
            SaveFormat.OCCUPANCY_BUILDING_INDEX_BASE + 3,
            2
        )
        writeLittleEndian(bytes, SaveFormat.TRAP_MASK_RECORDS_FILE_OFFSET, 0b00000101, 1)

        writeLittleEndian(bytes, SaveFormat.PORT_ROW_FILE_OFFSET, 14, 4)
        writeLittleEndian(bytes, SaveFormat.PORT_COLUMN_FILE_OFFSET, 22, 4)
        writeLittleEndian(bytes, SaveFormat.PORT_NEXT_REINFORCEMENT_TURN_FILE_OFFSET, 500, 4)
        writeLittleEndian(bytes, SaveFormat.PORT_REINFORCEMENT_READY_FILE_OFFSET, 1, 4)
        writeLittleEndian(bytes, SaveFormat.PORT_REINFORCEMENT_UNIT_COUNT_FILE_OFFSET, 8, 4)
        writeLittleEndian(bytes, SaveFormat.PORT_SHORE_VARIANT_FILE_OFFSET, 1, 4)

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
