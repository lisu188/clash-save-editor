package com.lis.clash

import com.lis.clash.objects.Castle
import com.lis.clash.objects.Save
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SaveConsolidationTest {
    private val saveSize = 514360
    private val firstCastleOffset = 509690

    @Test
    fun `editing fixed-length string preserves trailing bytes`() {
        val base = MutableList(saveSize) { 42.toByte() }
        base[firstCastleOffset + 4] = 1

        val save = Save().withBytes<Save>(base)
        save.castles.first().name = "AB"

        val edited = save.bytes
        assertEquals('A'.code.toByte(), edited[firstCastleOffset + 5])
        assertEquals('B'.code.toByte(), edited[firstCastleOffset + 6])

        // Remaining bytes in the fixed-width field should stay unchanged.
        for (i in 7..14) {
            assertEquals(42.toByte(), edited[firstCastleOffset + i])
        }
    }

    @Test
    fun `editing known field preserves unrelated bytes`() {
        val base = MutableList(saveSize) { it.mod(251).toByte() }
        base[firstCastleOffset + 4] = 1

        val save = Save().withBytes<Save>(base)
        val untouchedIndex = firstCastleOffset + 300
        val before = save.bytes[untouchedIndex]

        save.castles.first().happiness = 9

        assertEquals(before, save.bytes[untouchedIndex])
    }

    @Test
    fun `castle building flags decode consistently`() {
        val castle = Castle(parent = Save(), index = 0)
        castle.bytes = MutableList(467) { 0 }
        castle.building = (Castle.BUILDING_HOSPITAL or Castle.BUILDING_SCHOOL).toByte()

        val names = castle.buildingNames()
        assertEquals(listOf("hospital", "school"), names)
        assertTrue(castle.hasBuilding(Castle.BUILDING_HOSPITAL))
        assertTrue(castle.hasBuilding(Castle.BUILDING_SCHOOL))
    }
}
