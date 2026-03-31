package com.lis.clash

import com.lis.clash.objects.Castle
import com.lis.clash.objects.Save
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SaveConsolidationTest {
    private val saveSize = 514360
    private val firstCastleOffset = 509690
    private val firstPlayerOffset = 140040

    private fun blankParentSave(): Save {
        return Save().withBytes<Save>(MutableList(saveSize) { 0.toByte() })
    }

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

        save.castles.first().satisfaction = 9

        assertEquals(before, save.bytes[untouchedIndex])
    }

    @Test
    fun `castle building flags decode consistently`() {
        val castle = Castle(parent = blankParentSave(), index = firstCastleOffset)
        castle.bytes = MutableList(467) { 0 }
        castle.castleAddonFlags = Castle.BUILDING_HOSPITAL or Castle.BUILDING_SCHOOL

        val names = castle.buildingNames()
        assertEquals(listOf("hospital", "school"), names)
        assertTrue(castle.hasBuilding(Castle.BUILDING_HOSPITAL))
        assertTrue(castle.hasBuilding(Castle.BUILDING_SCHOOL))
    }

    @Test
    fun `stored money uses little endian four byte layout`() {
        val castle = Castle(parent = blankParentSave(), index = firstCastleOffset)
        val bytes = MutableList(467) { 0.toByte() }
        bytes[4] = 1
        bytes[438] = 0x78.toByte()
        bytes[439] = 0x56.toByte()
        bytes[440] = 0x34.toByte()
        bytes[441] = 0x12.toByte()
        castle.bytes = bytes

        assertEquals(0x12345678, castle.storedMoney)

        castle.storedMoney = 0x01020304
        assertEquals(0x04.toByte(), castle.bytes[438])
        assertEquals(0x03.toByte(), castle.bytes[439])
        assertEquals(0x02.toByte(), castle.bytes[440])
        assertEquals(0x01.toByte(), castle.bytes[441])
    }

    @Test
    fun `masked tax rate preserves unrelated high bits`() {
        val castle = Castle(parent = blankParentSave(), index = firstCastleOffset)
        val bytes = MutableList(467) { 0.toByte() }
        bytes[4] = 1
        bytes[436] = 0b11000000.toByte()
        castle.bytes = bytes

        castle.taxRate = 17

        assertEquals(0b11010001.toByte(), castle.bytes[436])
        assertEquals(17, castle.taxRate)
    }

    @Test
    fun `players parse from recovered base offset`() {
        val base = MutableList(saveSize) { 0.toByte() }
        base[firstPlayerOffset] = 1
        "Drebegen".encodeToByteArray().forEachIndexed { index, byte ->
            base[firstPlayerOffset + 4 + index] = byte
        }
        base[firstCastleOffset + 4] = 1

        val save = Save().withBytes<Save>(base)

        assertEquals(1, save.players.first().isActive)
        assertEquals("Drebegen\u0000\u0000\u0000", save.players.first().displayName)
    }
}
