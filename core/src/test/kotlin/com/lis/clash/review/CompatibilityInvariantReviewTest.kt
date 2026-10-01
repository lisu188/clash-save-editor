package com.lis.clash.review

import com.lis.clash.SaveFormat
import com.lis.clash.editor.*
import com.lis.clash.objects.Save
import kotlin.test.*

/** Independent compatibility checks for editor operations using synthetic fixtures. */
class CompatibilityInvariantReviewTest {
    private fun configured(): SaveDocument = SaveDocument.newScenario().apply {
        execute(EditCommand.ConfigurePlayer(0, true, true, name = "Human"))
        execute(EditCommand.ConfigurePlayer(1, true, false, name = "Opponent"))
    }

    @Test
    fun `allocation preserves nonempty records hidden after a leading unit sentinel`() {
        val initial = configured()
        val raw = initial.datBytes()
        val p = SaveFormat.ARMY_RECORDS_FILE_OFFSET
        put(raw, p + 6 + 2 * 31, 7, 2)
        raw[p + 6 + 2 * 31 + 9] = 87
        val document = SaveDocument.fromDat(raw, initial.facBytes())
        val result = runCatching { document.execute(EditCommand.CreateArmy(20, 20, 0)) }
        if (result.isFailure) assertContentEquals(raw, document.datBytes())
        else {
            assertNotEquals(0, result.getOrThrow()!!.slot)
            assertContentEquals(raw.copyOfRange(p, p + 725), document.datBytes().copyOfRange(p, p + 725))
        }
    }

    @Test
    fun `removing packed head cannot silently erase units beyond a malformed hole`() {
        val initial = configured()
        val id = initial.execute(EditCommand.CreateArmy(20, 20, 0))!!
        val raw = initial.datBytes()
        put(raw, SaveDocument.byteOffset(id) + 6 + 2 * 31, 7, 2)
        val document = SaveDocument.fromDat(raw, initial.facBytes())
        assertFails { document.execute(EditCommand.RemoveUnit(id, 0)) }
        assertContentEquals(raw, document.datBytes())
        assertEquals(0L, document.revision)
    }

    @Test
    fun `completed building initializes wall integrity as original completion routine`() {
        // A completed building uses seven wall-integrity bytes initialized to 100.
        val document = configured()
        val id = document.execute(EditCommand.CreateBuilding(20, 20, 0, 1, "Fort"))!!
        val castle = Save.parse(document.datBytes()).buildingSlots[id.slot]
        assertEquals(0, castle.constructionTurnsRemaining)
        assertEquals(1, castle.wallStrength)
        assertEquals(List(7) { 100.toByte() }, castle.wallSectionIntegrity)
    }

    @Test
    fun `type zero building with garrison and cargo stack are valid surviving assets`() {
        // A type-zero building with a garrison remains a qualifying player asset.
        val document = configured()
        val building = document.execute(EditCommand.CreateBuilding(20, 20, 0, 0, "Outpost"))!!
        document.execute(EditCommand.AddUnit(building, 1))
        val cargo = document.execute(EditCommand.CreateArmy(70, 70, 1, 31))!!
        document.execute(EditCommand.SetProperty(RecordId(RecordKind.ARMY_UNIT, cargo.slot, 0), "currentHealthPercent", "200"))
        val errors = document.validate(forExport = true).filter { it.severity == IssueSeverity.ERROR }
        assertTrue(errors.isEmpty(), errors.joinToString { it.message })
    }

    @Test
    fun `composition updates invalidate queued paths and reused garrison service timers`() {
        val initial = configured()
        val army = initial.execute(EditCommand.CreateArmy(20, 20, 0))!!
        val building = initial.execute(EditCommand.CreateBuilding(30, 30, 0))!!
        initial.execute(EditCommand.AddUnit(building, 0))
        val raw = initial.datBytes()
        val ap = SaveDocument.byteOffset(army)
        val bp = SaveDocument.byteOffset(building)
        put(raw, ap + 316, 2, 4)
        raw[ap + 320] = 22
        raw[bp + 391] = 0x3F
        val document = SaveDocument.fromDat(raw, initial.facBytes())
        document.execute(EditCommand.AddUnit(army, 5))
        document.execute(EditCommand.AddUnit(building, 5))
        val parsed = Save.parse(document.datBytes())
        assertEquals(0, parsed.armySlots[army.slot].queuedPathWaypointCount)
        assertTrue(parsed.armySlots[army.slot].bytes.subList(316, 720).all { it == 0.toByte() })
        assertEquals(0, parsed.buildingSlots[building.slot].garrisonOrderBytes[1].toInt())
    }

    @Test
    fun `ownership transfer cannot leave a player primary castle owned by another player`() {
        val document = configured()
        val building = document.execute(EditCommand.CreateBuilding(30, 30, 0, 2))!!
        val primary = SaveFormat.PLAYER_RECORDS_FILE_OFFSET + 43
        assertEquals(building.slot, int(document.datBytes(), primary))
        document.execute(EditCommand.ChangeOwner(building, 1))
        assertNotEquals(building.slot, int(document.datBytes(), primary))
    }

    @Test
    fun `site commands emit exact recovered ordered facts with row before column`() {
        val document = configured()
        document.execute(EditCommand.PaintTiles(listOf(1234), terrain = 752, overlay = 728))
        document.execute(EditCommand.PaintTiles(listOf(2234), terrain = 707))
        document.execute(EditCommand.SetTrap(3234, 1))
        val facts = FacDocument.parse(document.facBytes()!!).forms.map { it.atoms }
        assertTrue(listOf("skarb", "12", "34") in facts)
        assertTrue(listOf("swiatynia", "12", "34") in facts)
        assertTrue(listOf("zamek_place", "22", "34") in facts)
        assertTrue(listOf("pulapka", "32", "34") in facts)
    }

    @Test
    fun `raw map width is the row count and raw map height is the column count`() {
        val initial = configured()
        val raw = initial.datBytes()
        put(raw, SaveFormat.MAP_WIDTH_FILE_OFFSET, 40, 4)
        put(raw, SaveFormat.MAP_HEIGHT_FILE_OFFSET, 60, 4)
        val document = SaveDocument.fromDat(raw, initial.facBytes())
        val snapshot = document.snapshot()
        assertEquals(60, snapshot.mapWidth)
        assertEquals(40, snapshot.mapHeight)
        document.execute(EditCommand.CreateArmy(39, 59, 0))
        assertFails { document.execute(EditCommand.CreateArmy(40, 0, 0)) }
        assertFails { document.execute(EditCommand.CreateArmy(0, 60, 0)) }
    }

    private fun put(bytes: ByteArray, offset: Int, value: Int, size: Int) {
        repeat(size) { bytes[offset + it] = (value ushr (it * 8)).toByte() }
    }
    private fun int(bytes: ByteArray, offset: Int): Int = (0..3).fold(0) { value, i ->
        value or ((bytes[offset + i].toInt() and 255) shl (i * 8))
    }
}
