package com.lis.clash.editor

import com.lis.clash.SaveFormat as F
import kotlin.test.*

class SaveDocumentTest {
    private fun draft() = SaveDocument.newScenario().apply {
        execute(EditCommand.ConfigurePlayer(0, true, true, name = "Arthur"))
        execute(EditCommand.ConfigurePlayer(1, true, false, name = "Enemy"))
    }

    @Test fun blankScenarioHasRecoveredSentinelsAndNoAssets() {
        val doc = SaveDocument.newScenario(); val b = doc.datBytes(); val view = doc.snapshot()
        assertEquals(F.DAT_SIZE, b.size); assertEquals(-1, b.int(F.ACTIVE_MISSION_FILE_OFFSET))
        assertEquals(1, b.u16(F.GAME_TURN_COUNTER_FILE_OFFSET))
        assertEquals(100, view.mapWidth); assertEquals(100, view.mapHeight)
        assertTrue(view.entities.isEmpty()); assertTrue(view.players.none { it.active })
        assertTrue(view.tiles.all { it.terrain == 0 && it.overlay == 65535 && it.road == 65535 && it.occupancy == 65535 && it.trapMask == 0 })
        assertTrue((0 until 500).all { b.signed16(F.ARMY_RECORDS_FILE_OFFSET + 725 * it + 6) == -1 })
        assertTrue((0 until 100).all { b.signed16(F.BUILDING_RECORDS_FILE_OFFSET + 467 * it + 16) == -1 })
        assertTrue(doc.validate(true).count { it.severity == IssueSeverity.ERROR } >= 2)
    }

    @Test fun openBrowseAndNoOpPreserveOpaqueBytesAndFac() {
        val b = SaveDocument.newScenario().datBytes()
        b[2] = 0x81.toByte(); b[9] = 0xFF.toByte(); b[12] = 77
        b[F.TILE_RECORDS_FILE_OFFSET + 12] = 83
        val fac = "; untouched \u00ff\r\n(odd (nested \"a\\\"b\"))\r\n".toByteArray(Charsets.ISO_8859_1)
        val doc = SaveDocument.fromDat(b, fac)
        doc.snapshot(); doc.properties(RecordId(RecordKind.SAVE))
        assertContentEquals(b, doc.datBytes()); assertContentEquals(fac, doc.facBytes())
        assertFalse(doc.dirty); assertFalse(doc.canUndo)
    }

    @Test fun fullFootprintsMoveAndUndoAtomically() {
        val doc = draft()
        val id = doc.execute(EditCommand.CreateBuilding(4, 5, 0))!!
        val before = doc.datBytes(); val beforeFac = doc.facBytes()
        doc.execute(EditCommand.Move(id, 8, 9))
        val after = doc.datBytes()
        for (slot in listOf(405, 406, 505, 506)) assertEquals(65535, after.u16(F.OCCUPANCY_RECORDS_FILE_OFFSET + slot * 2))
        for (slot in listOf(809, 810, 909, 910)) assertEquals(32768, after.u16(F.OCCUPANCY_RECORDS_FILE_OFFSET + slot * 2))
        assertTrue(doc.undo()); assertContentEquals(before, doc.datBytes()); assertContentEquals(beforeFac, doc.facBytes())
        assertTrue(doc.redo()); assertContentEquals(after, doc.datBytes())
    }

    @Test fun collisionsAndInvalidNamesLeaveRevisionBytesHistoryUnchanged() {
        val doc = draft(); doc.execute(EditCommand.CreateArmy(4, 5, 0))
        val before = doc.datBytes(); val revision = doc.revision
        assertFailsWith<IllegalArgumentException> { doc.execute(EditCommand.CreateBuilding(4, 5, 1)) }
        assertFailsWith<IllegalArgumentException> { doc.execute(EditCommand.CreateBuilding(7, 7, 1, name = "Name much too long")) }
        assertContentEquals(before, doc.datBytes()); assertEquals(revision, doc.revision)
    }

    @Test fun physicalSlotsSurviveDeleteAndAllocationDoesNotOverwriteCorruptStack() {
        val original = draft(); val first = original.execute(EditCommand.CreateArmy(1, 1, 0))!!
        val second = original.execute(EditCommand.CreateArmy(2, 2, 0))!!
        original.execute(EditCommand.Delete(first))
        assertEquals(second, original.snapshot().entities.single().id)
        val b = original.datBytes(); b.put(F.ARMY_RECORDS_FILE_OFFSET + 6 + 31, 2, 0)
        val doc = SaveDocument.fromDat(b, original.facBytes())
        val created = doc.execute(EditCommand.CreateArmy(3, 3, 1))!!
        assertEquals(2, created.slot)
        assertEquals(0, doc.datBytes().u16(F.ARMY_RECORDS_FILE_OFFSET + 6 + 31))
        val before = doc.datBytes()
        assertFailsWith<IllegalArgumentException> { doc.execute(EditCommand.AddUnit(first, 2)) }
        assertContentEquals(before, doc.datBytes())
        assertTrue(doc.validate().any { "packed sequence" in it.message })
    }

    @Test fun armyMoveClearsPathAndCloningDropsRuntimeFactHandle() {
        val source = draft(); val id = source.execute(EditCommand.CreateArmy(1, 1, 0, 2))!!
        val b = source.datBytes(); val base = SaveDocument.byteOffset(id)
        b.put(base + 316, 4, 3); b[base + 320] = 72; b.put(base + 721, 4, 0x12345678)
        val doc = SaveDocument.fromDat(b, source.facBytes())
        doc.execute(EditCommand.Move(id, 3, 3))
        assertTrue(doc.datBytes().sliceArray(base + 316 until base + 720).all { it == 0.toByte() })
        val clone = doc.execute(EditCommand.Clone(id, 5, 5))!!
        val cloneBase = SaveDocument.byteOffset(clone)
        assertEquals(0, doc.datBytes().int(cloneBase + 721))
        assertEquals(2, doc.datBytes().u16(cloneBase + 6))
    }

    @Test fun unitCompositionCompactsGarrisonAndOwnershipStaysConsistent() {
        val doc = draft(); val id = doc.execute(EditCommand.CreateBuilding(4, 4, 0))!!
        doc.execute(EditCommand.AddUnit(id, 1)); doc.execute(EditCommand.AddUnit(id, 2)); doc.execute(EditCommand.AddUnit(id, 3))
        doc.execute(EditCommand.RemoveUnit(id, 1)); doc.execute(EditCommand.ChangeOwner(id, 1))
        val b = doc.datBytes(); val base = SaveDocument.byteOffset(id)
        assertEquals(1, b.u16(base + 18)); assertEquals(3, b.u16(base + 49)); assertEquals(-1, b.signed16(base + 80))
        assertEquals(1, b.u8(base + 2)); assertEquals(1, b.u8(base + 20)); assertEquals(1, b.u8(base + 51))
        assertTrue(doc.validate().none { it.severity == IssueSeverity.ERROR })
    }

    @Test fun finalArmyUnitDeletionRemovesOccupancy() {
        val doc = draft(); val id = doc.execute(EditCommand.CreateArmy(1, 1, 0))!!
        doc.execute(EditCommand.RemoveUnit(id, 0))
        assertTrue(doc.snapshot().entities.isEmpty())
        assertEquals(65535, doc.datBytes().u16(F.OCCUPANCY_RECORDS_FILE_OFFSET + 202))
    }

    @Test fun unitCapacityAndUnrecoveredUnitTypesFailBeforeMutation() {
        val doc = draft(); val id = doc.execute(EditCommand.CreateArmy(1, 1, 0))!!
        repeat(9) { doc.execute(EditCommand.AddUnit(id, 1)) }
        val before = doc.datBytes(); val rev = doc.revision
        assertFailsWith<IllegalStateException> { doc.execute(EditCommand.AddUnit(id, 2)) }
        assertFailsWith<IllegalArgumentException> { doc.execute(EditCommand.CreateArmy(3, 3, 0, 35)) }
        assertContentEquals(before, doc.datBytes()); assertEquals(rev, doc.revision)
    }

    @Test fun rectangularVisibleBoundsUseOriginalRowDimensionAndFixedStorageStride() {
        val original = draft(); val b = original.datBytes()
        b.put(F.MAP_WIDTH_FILE_OFFSET, 4, 4); b.put(F.MAP_HEIGHT_FILE_OFFSET, 4, 3)
        val doc = SaveDocument.fromDat(b, original.facBytes())
        val id = doc.execute(EditCommand.CreateArmy(3, 2, 0))!!
        assertEquals(3, doc.snapshot().mapWidth); assertEquals(4, doc.snapshot().mapHeight)
        assertEquals(id.slot, doc.datBytes().u16(F.OCCUPANCY_RECORDS_FILE_OFFSET + 302 * 2))
        assertFailsWith<IllegalArgumentException> { doc.execute(EditCommand.Move(id, 4, 2)) }
        assertFailsWith<IllegalArgumentException> { doc.execute(EditCommand.Move(id, 3, 3)) }
    }

    @Test fun brushAndTrapBatchProduceSingleUndoWithPairedFacts() {
        val doc = draft(); val before = doc.datBytes(); val beforeFac = doc.facBytes(); val revision = doc.revision
        doc.execute(EditCommand.PaintTiles(listOf(102, 103, 104), overlay = 728))
        assertEquals(revision + 1, doc.revision)
        val text = doc.facBytes()!!.toString(Charsets.ISO_8859_1)
        assertContains(text, "(swiatynia 1 2)"); assertContains(text, "(swiatynia 1 4)")
        doc.undo(); assertContentEquals(before, doc.datBytes()); assertContentEquals(beforeFac, doc.facBytes())
        doc.execute(EditCommand.SetTraps(listOf(102, 103), 3))
        assertEquals(3, doc.datBytes().u8(F.TRAP_MASK_RECORDS_FILE_OFFSET + 103))
        assertContains(doc.facBytes()!!.toString(Charsets.ISO_8859_1), "(pulapka 1 3)")
        doc.undo(); assertContentEquals(before, doc.datBytes()); assertContentEquals(beforeFac, doc.facBytes())
    }

    @Test fun trapOwnersMustBeActiveAndRejectedCommandsAreAtomic() {
        val doc = draft()
        val before = doc.datBytes(); val beforeFac = doc.facBytes(); val revision = doc.revision
        for (command in listOf(EditCommand.SetTrap(101, 16), EditCommand.SetTraps(listOf(102, 103), 5))) {
            assertFailsWith<IllegalArgumentException> { doc.execute(command) }
        }
        assertContentEquals(before, doc.datBytes()); assertContentEquals(beforeFac, doc.facBytes()); assertEquals(revision, doc.revision)
        doc.execute(EditCommand.SetTraps(listOf(102, 103), 3))
        assertEquals(3, doc.datBytes().u8(F.TRAP_MASK_RECORDS_FILE_OFFSET + 102))
        doc.execute(EditCommand.SetTraps(listOf(102, 103), 0))
        assertEquals(0, doc.datBytes().u8(F.TRAP_MASK_RECORDS_FILE_OFFSET + 102))
    }

    @Test fun importedInvalidTrapOwnershipAndOutOfBoundsTrapsCannotExport() {
        val doc = draft()
        doc.execute(EditCommand.CreateArmy(1, 1, 0)); doc.execute(EditCommand.CreateArmy(2, 2, 1))
        doc.execute(EditCommand.SetTrap(303, 1))
        assertTrue(doc.validate(true).none { it.severity == IssueSeverity.ERROR })
        for (mask in listOf(16, 128)) {
            val bytes = doc.datBytes(); bytes[F.TRAP_MASK_RECORDS_FILE_OFFSET + 303] = mask.toByte()
            val imported = SaveDocument.fromDat(bytes, doc.facBytes())
            assertTrue(imported.validate(true).any { it.severity == IssueSeverity.ERROR && "Trap owner mask" in it.message })
            assertContentEquals(bytes, imported.datBytes())
        }
        val bytes = doc.datBytes(); bytes.put(F.MAP_WIDTH_FILE_OFFSET, 4, 3)
        val imported = SaveDocument.fromDat(bytes, doc.facBytes())
        assertTrue(imported.validate(true).any { it.severity == IssueSeverity.ERROR && "Trap is outside" in it.message })
    }

    @Test fun campaignOrUnknownFacDependenciesBlockStructuralButAllowIndependentFields() {
        val source = draft(); val id = source.execute(EditCommand.CreateArmy(1, 1, 0))!!
        for (fac in listOf(null, "(initial-fact)\n(unknown 2 3)\n".toByteArray(), "(broken".toByteArray())) {
            val doc = SaveDocument.fromDat(source.datBytes(), fac)
            assertFails { doc.execute(EditCommand.Move(id, 2, 2)) }
            doc.execute(EditCommand.SetProperty(RecordId(RecordKind.SAVE), "name", "Changed"))
            assertEquals("Changed", doc.snapshot().name)
            assertContentEquals(fac, doc.facBytes())
        }
        val campaignBytes = source.datBytes(); campaignBytes.put(F.ACTIVE_MISSION_FILE_OFFSET, 4, 3)
        val campaign = SaveDocument.fromDat(campaignBytes, source.facBytes())
        assertFailsWith<IllegalArgumentException> { campaign.execute(EditCommand.Delete(id)) }
    }

    @Test fun independentScalarEditsPreserveOtherPackedBitsAndUnrelatedBytes() {
        val source = draft(); val id = source.execute(EditCommand.CreateArmy(1, 1, 0))!!
        val b = source.datBytes(); val p = SaveDocument.byteOffset(id) + 6; b[p + 12] = 0xA4.toByte()
        val doc = SaveDocument.fromDat(b)
        doc.execute(EditCommand.SetProperty(RecordId(RecordKind.ARMY_UNIT, id.slot, 0), "experienceLevel", "3"))
        val result = doc.datBytes(); assertEquals(0xA7, result.u8(p + 12))
        assertEquals(listOf(p + 12), b.indices.filter { b[it] != result[it] })
        assertFails { doc.execute(EditCommand.SetProperty(id, "ownerPlayerIndex", "1")) }
        assertFails { doc.execute(EditCommand.SetProperty(RecordId(RecordKind.SAVE), "name", "\uD83D\uDE80")) }
    }

    @Test fun playableSandboxRequiresRosterAndConsistentFacButAllowsCargoQuantity() {
        val doc = draft(); val first = doc.execute(EditCommand.CreateArmy(1, 1, 0))!!
        assertTrue(doc.validate(true).any { "Player 1 needs" in it.message })
        doc.execute(EditCommand.CreateArmy(8, 8, 1, 31))
        val b = doc.datBytes(); b[F.ARMY_RECORDS_FILE_OFFSET + 725 + 6 + 9] = 200.toByte()
        val cargo = SaveDocument.fromDat(b, doc.facBytes())
        assertTrue(cargo.validate(true).none { it.severity == IssueSeverity.ERROR })
        val badFac = SaveDocument.fromDat(b, "(initial-fact)\n(misja -1)\n".toByteArray())
        assertTrue(badFac.validate(true).any { "gameinfo" in it.message })
        val before = doc.datBytes()
        assertFails { doc.execute(EditCommand.ConfigurePlayer(0, false, true)) }
        assertContentEquals(before, doc.datBytes()); assertEquals(0, first.slot)
    }

    @Test fun savedStateTracksUndoRedoAndDefensiveBytes() {
        val doc = SaveDocument.newScenario(); doc.markSaved()
        val leaked = doc.datBytes(); leaked[0] = 0
        assertEquals("New scenario", doc.snapshot().name)
        doc.execute(EditCommand.SetProperty(RecordId(RecordKind.SAVE), "name", "Edited"))
        assertTrue(doc.dirty); doc.undo(); assertFalse(doc.dirty); doc.redo(); assertTrue(doc.dirty)
        doc.markSaved(); assertFalse(doc.dirty)
    }

    @Test fun coordinateOverflowInvalidTileIdsAndSitesOnOccupiedGroundFailAtomically() {
        val doc = draft(); val army = doc.execute(EditCommand.CreateArmy(2, 2, 0))!!
        val before = doc.datBytes(); val revision = doc.revision
        for ((row, column) in listOf(Int.MAX_VALUE to 1, 1 to Int.MAX_VALUE, -1 to 1, 100 to 1)) {
            assertFailsWith<IllegalArgumentException> { doc.execute(EditCommand.Move(army, row, column)) }
        }
        for (terrain in listOf(65535, 716, 40000)) assertFailsWith<IllegalArgumentException> {
            doc.execute(EditCommand.PaintTiles(listOf(0), terrain = terrain))
        }
        assertFailsWith<IllegalArgumentException> { doc.execute(EditCommand.PaintTiles(listOf(202), overlay = 728)) }
        assertFailsWith<IllegalArgumentException> { doc.execute(EditCommand.SetTrap(202, 1)) }
        assertContentEquals(before, doc.datBytes()); assertEquals(revision, doc.revision)
    }

    @Test fun invalidIndependentGameRangesAndProductionCouplingAreRejected() {
        val doc = draft(); val army = doc.execute(EditCommand.CreateArmy(2, 2, 0))!!
        val unit = RecordId(RecordKind.ARMY_UNIT, army.slot, 0)
        val building = doc.execute(EditCommand.CreateBuilding(8, 8, 0))!!
        val before = doc.datBytes()
        for ((name, value) in listOf("currentHealthPercent" to "101", "fatigue" to "101", "morale" to "21")) {
            assertFailsWith<IllegalArgumentException> { doc.execute(EditCommand.SetProperty(unit, name, value)) }
        }
        assertFailsWith<IllegalArgumentException> { doc.execute(EditCommand.SetProperty(building, "activeProductionLicenceSlotIndex", "10")) }
        assertContentEquals(before, doc.datBytes())
    }

    @Test fun malformedAndStaleFacReferencesArePreservedButCannotExport() {
        val doc = draft(); doc.execute(EditCommand.CreateArmy(2, 2, 0)); doc.execute(EditCommand.CreateArmy(8, 8, 1))
        for (fact in listOf("(swiatynia 012 34)", "(zamek_place 100 2)", "(gameinfo gracz 4 komputer 0 inteligencja 0 chrzesc 1)", "(schemat 0 99 2)")) {
            val fac = doc.facBytes()!! + "$fact\n".toByteArray()
            val invalid = SaveDocument.fromDat(doc.datBytes(), fac)
            assertTrue(invalid.validate(true).any { it.severity == IssueSeverity.ERROR }, fact)
            assertContentEquals(fac, invalid.facBytes())
        }
    }

    @Test fun encodingChoiceIsUndoableMetadataAndNeverTranscodesUntouchedBytes() {
        val doc = SaveDocument.newScenario(); doc.markSaved()
        val before = doc.datBytes(); val beforeFac = doc.facBytes()
        doc.setEncoding("windows-1252")
        assertEquals("windows-1252", doc.encoding); assertTrue(doc.dirty)
        assertContentEquals(before, doc.datBytes()); assertContentEquals(beforeFac, doc.facBytes())
        assertTrue(doc.undo()); assertEquals("windows-1250", doc.encoding); assertFalse(doc.dirty)
        assertTrue(doc.redo()); assertEquals("windows-1252", doc.encoding)
    }
}
