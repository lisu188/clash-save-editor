package com.lis.clash

import com.lis.clash.objects.OccupancyEntry
import com.lis.clash.objects.Save
import kotlin.reflect.full.functions
import kotlin.reflect.full.hasAnnotation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SaveFormatParsingTest {
    @Test
    fun parsesRecoveredSaveFields() {
        val save = Save().withBytes<Save>(buildSyntheticSave().toList())

        assertEquals(Save.EXPECTED_FILE_SIZE, save.bytes.size)
        assertEquals("TEST SAVE", save.name)
        assertEquals(60, save.mapWidthTiles)
        assertEquals(40, save.mapHeightTiles)
        assertEquals(7, save.mapViewLeft)
        assertEquals(9, save.mapViewTop)
        assertEquals(2, save.mapThemeId)
        assertEquals(-1, save.activeMissionIndex)
        assertEquals(1, save.missionFailureFlag)
        assertEquals(17, save.turnCounter)
        assertEquals(2, save.turnOwnerPlayerIndex)
        assertEquals(3, save.viewedPlayerIndex)

        assertEquals(1, save.transitionAnimationsEnabled)
        assertEquals(1, save.gridOverlayEnabled)
        assertEquals(0, save.statusOverlayEnabled)
        assertEquals(1, save.fastMovementAnimationsEnabled)
        assertEquals(1, save.musicEnabled)
        assertEquals(1, save.soundEffectsEnabled)
        assertEquals(8, save.scrollSpeedRaw)
        assertEquals(9, save.soundVolumeRaw)
        assertEquals(-3, save.musicVolumeRaw)

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
        assertEquals(0x11223344, army.armyFactHandle)
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
        assertEquals(0x9E, unit.stanceBits)
        assertEquals(2, unit.statusLevel)
        assertEquals(3, unit.orderState)
        assertEquals(1, unit.volleysUsed)
        assertEquals(2, unit.remainingVolleys())
        assertEquals(0xAF, unit.stateFlags)
        assertEquals(1, unit.readyForTurnFlag)
        assertEquals(1, unit.spentTurnFlag)
        assertEquals(1, unit.lowMoraleFlag)
        assertEquals(1, unit.plagueFlag)
        assertTrue(unit.hasLowMoraleFlag())
        assertTrue(unit.hasPlagueFlag())
        assertEquals("low", unit.moraleBand())
        assertEquals("exhausted", unit.fatigueBand())
        assertEquals(0x55667788, unit.auxRuntimeState)
        assertEquals(0x03, unit.stateBits2)

        val castle = save.castles.single()
        assertEquals(44, castle.tileRow)
        assertEquals(55, castle.tileColumn)
        assertEquals(2, castle.ownerPlayerIndex)
        assertEquals(3, castle.appearance)
        assertEquals(2, castle.buildingType)
        assertEquals("CastleNameX", castle.displayName)
        assertEquals(300, castle.constructionWorkRemaining)
        assertEquals(listOf("Peasant", "Heavy infantry", "Builder"), castle.unitLicenceTypeNames())
        assertEquals(-1, castle.activeProductionLicenceSlotIndex)
        assertEquals(7, castle.productionTurnsRemaining)
        assertEquals(0x1D, castle.garrisonServiceStates().first().rawValue)
        assertEquals(5, castle.garrisonServiceStates().first().trainCountdown)
        assertEquals(3, castle.garrisonServiceStates().first().repairCountdown)
        assertEquals(9, castle.wallStrength)
        assertEquals(listOf(100, 90, 80, 70, 60, 50, 40), castle.wallSectionIntegrity.map { it.toInt() and 0xFF })
        assertEquals(245, castle.peasantCount)
        assertEquals(-5, castle.populationGrowthDelta())
        assertEquals(77, castle.satisfaction)
        assertEquals(321, castle.lastCollectedGoldIncome)
        assertEquals(77, castle.castleFactHandle)
        assertEquals(0x1234, castle.decodedPrisonerSlots().first().ransomValue)

        assertEquals(OccupancyEntry.Army(0), save.occupancyAt(0, 0))
        assertEquals(OccupancyEntry.Building(0), save.occupancyAt(0, 1))
        assertEquals(OccupancyEntry.Empty, save.occupancyAt(0, 2))
        assertEquals(4, save.trapOwnerMask(1, 2))
        assertEquals(8, save.portTileRow)
        assertEquals(95, save.portTileColumn)
        assertEquals(8, save.portNextReinforcementTurn)
        assertEquals(1, save.portReinforcementReadyFlag)
        assertEquals(5, save.portReinforcementUnitCount)
        assertEquals(1, save.portShorelineVariantFlag)
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
    fun unitPackedStateEditsPreserveOtherBits() {
        val save = Save().withBytes<Save>(buildSyntheticSave().toList())
        val unit = save.armies.single().units.single()

        assertEquals(0x9E, unit.stanceBits)

        unit.statusLevel = 3
        assertEquals(0x9F, unit.stanceBits)

        unit.orderState = 1
        assertEquals(0x97, unit.stanceBits)

        unit.volleysUsed = 5
        assertEquals(0xD7, unit.stanceBits)
        assertEquals(0, unit.remainingVolleys())
    }

    @Test
    fun unitKnownFlagsPreserveUnknownUpperBits() {
        val save = Save().withBytes<Save>(buildSyntheticSave().toList())
        val unit = save.armies.single().units.single()

        assertEquals(0xAF, unit.stateFlags)
        assertEquals(1, unit.lowMoraleFlag)

        unit.lowMoraleFlag = 0
        assertEquals(0xAB, unit.stateFlags)
        assertEquals(0, unit.lowMoraleFlag)
        assertFalse(unit.hasLowMoraleFlag())

        unit.plagueFlag = 0
        assertEquals(0xA3, unit.stateFlags)
        assertFalse(unit.hasPlagueFlag())
    }

    @Test
    fun templeOverlayHelpersIdentifyShrines() {
        val save = Save().withBytes<Save>(buildSyntheticSave().toList())
        val tile = save.tiles.first()

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
        val bytes = ByteArray(Save.EXPECTED_FILE_SIZE)

        repeat(Save.ARMY_COUNT) { armyIndex ->
            writeLittleEndian(bytes, 147190 + armyIndex * 725 + 6, 0xFFFF, 2)
        }
        repeat(Save.BUILDING_COUNT) { castleIndex ->
            writeLittleEndian(bytes, 509690 + castleIndex * 467 + 4, 0xFF, 1)
        }
        repeat(Save.MAP_TILE_COUNT) { tileIndex ->
            writeLittleEndian(bytes, 556390 + tileIndex * 2, Save.OCCUPANCY_EMPTY, 2)
        }

        writeString(bytes, 0, 16, "TEST SAVE")

        writeLittleEndian(bytes, 16, 321, 2)
        writeLittleEndian(bytes, 18, 654, 2)
        writeLittleEndian(bytes, 20, 872, 2)

        writeLittleEndian(bytes, 140016, 60, 4)
        writeLittleEndian(bytes, 140020, 40, 4)
        writeLittleEndian(bytes, 140024, 7, 4)
        writeLittleEndian(bytes, 140028, 9, 4)
        writeLittleEndian(bytes, 140032, 2, 1)
        writeLittleEndian(bytes, 140033, -1, 4)
        writeLittleEndian(bytes, 140037, 1, 1)
        writeLittleEndian(bytes, 140038, 17, 2)

        writeLittleEndian(bytes, 147155, 2, 4)
        writeLittleEndian(bytes, 147159, 3, 4)
        writeLittleEndian(bytes, 147163, 1, 4)
        writeLittleEndian(bytes, 147167, 1, 4)
        writeLittleEndian(bytes, 147171, 0, 4)
        writeLittleEndian(bytes, 147175, 1, 4)
        writeLittleEndian(bytes, 147179, 1, 4)
        writeLittleEndian(bytes, 147183, 1, 4)
        writeLittleEndian(bytes, 147187, 8, 1)
        writeLittleEndian(bytes, 147188, 9, 1)
        writeLittleEndian(bytes, 147189, -3, 1)

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
        writeLittleEndian(bytes, armyBase + 721, 0x11223344, 4)

        val armyUnitBase = armyBase + 6
        writeLittleEndian(bytes, armyUnitBase, 34, 2)
        writeLittleEndian(bytes, armyUnitBase + 2, 1, 1)
        writeLittleEndian(bytes, armyUnitBase + 8, 5, 1)
        writeLittleEndian(bytes, armyUnitBase + 9, 80, 1)
        writeLittleEndian(bytes, armyUnitBase + 10, 90, 1)
        writeLittleEndian(bytes, armyUnitBase + 11, 4, 1)
        writeLittleEndian(bytes, armyUnitBase + 12, 0x9E, 1)
        writeLittleEndian(bytes, armyUnitBase + 13, 0xAF, 1)
        writeLittleEndian(bytes, armyUnitBase + 18, 0x55667788, 4)
        writeLittleEndian(bytes, armyUnitBase + 22, 0x03, 1)

        val castleBase = 509690
        repeat(12) { slotIndex ->
            writeLittleEndian(bytes, castleBase + 18 + slotIndex * 31, 0xFFFF, 2)
            writeLittleEndian(bytes, castleBase + 402 + slotIndex, 0xFF, 1)
        }
        writeLittleEndian(bytes, castleBase, 44, 1)
        writeLittleEndian(bytes, castleBase + 1, 55, 1)
        writeLittleEndian(bytes, castleBase + 2, 2, 1)
        writeLittleEndian(bytes, castleBase + 3, 3, 1)
        writeLittleEndian(bytes, castleBase + 4, 2, 1)
        writeString(bytes, castleBase + 5, 11, "CastleNameX")
        writeLittleEndian(bytes, castleBase + 16, 300, 2)
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
        writeLittleEndian(bytes, castleBase + 402, 0, 1)
        writeLittleEndian(bytes, castleBase + 403, 2, 1)
        writeLittleEndian(bytes, castleBase + 404, 17, 1)
        writeLittleEndian(bytes, castleBase + 414, -1, 1)
        writeLittleEndian(bytes, castleBase + 415, 7, 1)
        writeLittleEndian(bytes, castleBase + 416, 0x13, 1)
        writeLittleEndian(bytes, castleBase + 420, 0x01, 1)
        writeLittleEndian(bytes, castleBase + 421, 9, 1)
        listOf(100, 90, 80, 70, 60, 50, 40).forEachIndexed { index, value ->
            writeLittleEndian(bytes, castleBase + 422 + index, value, 1)
        }
        writeLittleEndian(bytes, castleBase + 429, 4, 1)
        writeLittleEndian(bytes, castleBase + 430, 245, 2)
        writeLittleEndian(bytes, castleBase + 432, 0xAFFB, 2)
        writeLittleEndian(bytes, castleBase + 434, 77, 1)
        writeLittleEndian(bytes, castleBase + 435, 3, 1)
        writeLittleEndian(bytes, castleBase + 436, 12, 1)
        writeLittleEndian(bytes, castleBase + 438, 54321, 4)
        writeLittleEndian(bytes, castleBase + 442, 321, 2)
        writeLittleEndian(bytes, castleBase + 444, 5, 1)
        listOf(1, 2, 3, 4, 0x34, 0x12).forEachIndexed { index, value ->
            writeLittleEndian(bytes, castleBase + 445 + index, value, 1)
        }
        writeLittleEndian(bytes, castleBase + 463, 77, 4)

        writeLittleEndian(bytes, 556390, 0, 2)
        writeLittleEndian(bytes, 556392, Save.OCCUPANCY_BUILDING_BASE, 2)
        writeLittleEndian(bytes, 576390 + 102, 4, 1)

        writeLittleEndian(bytes, 586390, 8, 4)
        writeLittleEndian(bytes, 586394, 95, 4)
        writeLittleEndian(bytes, 586398, 8, 4)
        writeLittleEndian(bytes, 586402, 1, 4)
        writeLittleEndian(bytes, 586406, 5, 4)
        writeLittleEndian(bytes, 586410, 1, 4)

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
