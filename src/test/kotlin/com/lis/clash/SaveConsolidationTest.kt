package com.lis.clash

import com.lis.clash.objects.Castle
import com.lis.clash.objects.Save
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SaveConsolidationTest {
    private val saveSize = SaveFormat.DAT_SIZE
    private val firstCastleOffset = SaveFormat.BUILDING_RECORDS_FILE_OFFSET
    private val firstPlayerOffset = SaveFormat.PLAYER_RECORDS_FILE_OFFSET

    private fun blankSaveBytes(fill: Byte = 0): MutableList<Byte> {
        val bytes = MutableList(saveSize) { fill }
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
        }
        return bytes
    }

    private fun blankParentSave(): Save {
        return Save.parse(blankSaveBytes().toByteArray())
    }

    @Test
    fun `editing fixed-length string zero pads trailing bytes`() {
        val base = blankSaveBytes(42)
        base[firstCastleOffset + 4] = 1
        writeLittleEndian(base, firstCastleOffset + 16, 0, 2)

        val save = Save.parse(base.toByteArray())
        save.castles.first().displayName = "AB"

        val edited = save.bytes
        assertEquals('A'.code.toByte(), edited[firstCastleOffset + 5])
        assertEquals('B'.code.toByte(), edited[firstCastleOffset + 6])
        for (i in 7..15) {
            assertEquals(0.toByte(), edited[firstCastleOffset + i])
        }
    }

    @Test
    fun `editing known field preserves unrelated bytes`() {
        val base = blankSaveBytes()
        base.indices.forEach { index -> base[index] = index.mod(251).toByte() }
        repeat(SaveFormat.ARMY_RECORD_COUNT) { armyIndex ->
            repeat(10) { slotIndex ->
                writeLittleEndian(
                    base,
                    SaveFormat.ARMY_RECORDS_FILE_OFFSET + armyIndex * SaveFormat.ARMY_RECORD_SIZE + 6 + slotIndex * 31,
                    -1,
                    2
                )
            }
        }
        repeat(SaveFormat.BUILDING_RECORD_COUNT) { buildingIndex ->
            val recordBase = SaveFormat.BUILDING_RECORDS_FILE_OFFSET + buildingIndex * SaveFormat.BUILDING_RECORD_SIZE
            writeLittleEndian(base, recordBase + 4, -1, 1)
            writeLittleEndian(base, recordBase + 16, -1, 2)
        }
        base[firstCastleOffset + 4] = 1
        writeLittleEndian(base, firstCastleOffset + 16, 0, 2)

        val save = Save.parse(base.toByteArray())
        val untouchedIndex = firstCastleOffset + 300
        val before = save.bytes[untouchedIndex]

        save.castles.first().satisfaction = 9

        assertEquals(before, save.bytes[untouchedIndex])
    }

    @Test
    fun `castle building flags decode consistently`() {
        val castle = Castle(parent = blankParentSave(), index = firstCastleOffset)
        castle.bytes = MutableList(SaveFormat.BUILDING_RECORD_SIZE) { 0 }
        castle.castleAddonFlags = Castle.BUILDING_HOSPITAL or Castle.BUILDING_SCHOOL

        val names = castle.buildingNames()
        assertEquals(listOf("hospital", "school"), names)
        assertTrue(castle.hasBuilding(Castle.BUILDING_HOSPITAL))
        assertTrue(castle.hasBuilding(Castle.BUILDING_SCHOOL))
    }

    @Test
    fun `stored money uses little endian four byte layout`() {
        val castle = Castle(parent = blankParentSave(), index = firstCastleOffset)
        val bytes = MutableList(SaveFormat.BUILDING_RECORD_SIZE) { 0.toByte() }
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
        val bytes = MutableList(SaveFormat.BUILDING_RECORD_SIZE) { 0.toByte() }
        bytes[4] = 1
        bytes[436] = 0b11000000.toByte()
        castle.bytes = bytes

        castle.taxRate = 17

        assertEquals(0b11010001.toByte(), castle.bytes[436])
        assertEquals(17, castle.taxRate)
    }

    @Test
    fun `players parse from recovered base offset`() {
        val base = blankSaveBytes()
        base[firstPlayerOffset] = 1
        "Drebegen".encodeToByteArray().forEachIndexed { index, byte ->
            base[firstPlayerOffset + 4 + index] = byte
        }

        val save = Save.parse(base.toByteArray())

        assertEquals(1, save.players.first().isActive)
        assertEquals("Drebegen", save.players.first().displayName)
    }

    private fun writeLittleEndian(bytes: MutableList<Byte>, offset: Int, value: Int, length: Int) {
        repeat(length) { index ->
            bytes[offset + index] = ((value ushr (index * 8)) and 0xFF).toByte()
        }
    }
}
