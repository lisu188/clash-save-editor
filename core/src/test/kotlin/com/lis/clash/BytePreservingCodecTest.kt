package com.lis.clash

import com.lis.clash.objects.Save
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BytePreservingCodecTest {
    @Test
    fun `all reads preserve arbitrary names padding unknown fields and inactive records`() {
        val original = Random(491).nextBytes(SaveFormat.DAT_SIZE)
        val save = Save.parse(original)
        save.name
        save.players.forEach { player ->
            player.displayName
            player.typedPrisonerTransferQueue()
        }
        save.armySlots.forEach { army -> army.unitSlots.forEach { unit ->
            getClassDescriptor(unit::class).getSimpleProperties().forEach { it.get(unit) }
        } }
        save.buildingSlots.forEach { building ->
            getClassDescriptor(building::class).getSimpleProperties().forEach { it.get(building) }
            building.typedPrisonerSlots()
        }
        save.options.musicVolumeRaw
        save.tiles.last().roadOrBridgeTileId
        save.tileOccupancy.last().rawValue
        save.trapOwnerMasks.last().ownerMask
        assertContentEquals(original, save.toByteArray())
        val snapshot = save.toByteArray()
        snapshot[0] = 0
        assertContentEquals(original, save.toByteArray())
    }

    @Test
    fun `physical table views include final slots and sparse holes while units are packed`() {
        val save = blank()
        val army = save.armySlots.last()
        army.unitSlots[0].typeId = 5
        army.unitSlots[2].typeId = 6
        val building = save.buildingSlots.last()
        building.footprintClass = 2
        building.constructionTurnsRemaining = 0
        assertEquals(500, save.armySlots.size)
        assertEquals(100, save.buildingSlots.size)
        assertEquals(499, save.armies.single().physicalSlot)
        assertEquals(99, save.castles.single().physicalSlot)
        assertEquals(1, army.units.size)
        assertEquals(6, army.unitSlots[2].typeId)
        assertEquals(10, army.unitSlots.size)
        assertEquals(12, building.unitSlots.size)
        assertContentEquals(save.toByteArray(), Save.parse(save.toByteArray()).toByteArray())
    }

    @Test
    fun `unsigned money and signed masked growth retain exact boundaries`() {
        val save = blank()
        val building = save.buildingSlots[0]
        building.storedMoney = 0xFFFFFFFFL
        assertEquals(0xFFFFFFFFL, building.storedMoney)
        building.changeByte(433, 0xA0.toByte())
        building.populationGrowthDelta = -2048
        assertEquals(-2048, building.populationGrowthDelta)
        assertEquals(0xA8, building.bytes[433].toInt() and 0xFF)
        building.populationGrowthDelta = 2047
        assertEquals(2047, building.populationGrowthDelta)
        assertEquals(0xA7, building.bytes[433].toInt() and 0xFF)
        building.satisfaction = -128
        assertEquals(-128, building.satisfaction)
        building.lastCollectedGoldIncome = 65535
        assertEquals(65535, building.lastCollectedGoldIncome)
        val before = save.toByteArray()
        assertFailsWith<IllegalArgumentException> { building.populationGrowthDelta = -2049 }
        assertFailsWith<IllegalArgumentException> { building.populationGrowthDelta = 2048 }
        assertFailsWith<IllegalArgumentException> { building.storedMoney = -1L }
        assertFailsWith<IllegalArgumentException> { building.storedMoney = 0x100000000L }
        assertFailsWith<IllegalArgumentException> { building.satisfaction = 128 }
        assertFailsWith<IllegalArgumentException> { building.lastCollectedGoldIncome = 65536 }
        assertContentEquals(before, save.toByteArray())
    }

    @Test
    fun `unit status order and volleys preserve unrelated bits and deprecated aliases`() {
        val unit = blank().armySlots[0].unitSlots[0]
        unit.changeByte(12, 0x80.toByte())
        unit.statusLevel = 3
        unit.orderState = 2
        unit.volleysUsed = 7
        assertEquals(0xFB, unit.stanceBits)
        assertEquals(3, unit.experienceLevel)
        assertEquals(2, unit.experienceProgress)
        unit.experienceProgress = 1
        assertEquals(1, unit.orderState)
        assertEquals(7, unit.volleysUsed)
        unit.changeByte(13, 0xF0.toByte())
        unit.readyForTurn = 1
        unit.spentTurn = 1
        unit.lowMoraleFlag = 1
        unit.plagueFlag = 1
        assertEquals(0xFF, unit.stateFlags)
        val before = unit.toByteArray()
        assertFailsWith<IllegalArgumentException> { unit.volleysUsed = 8 }
        assertFailsWith<IllegalArgumentException> { unit.readyForTurn = -1 }
        assertContentEquals(before, unit.toByteArray())
    }

    @Test
    fun `text decoding is explicit and edits reject truncation and unrepresentable characters`() {
        val save = blank()
        val player = save.players[0]
        player.displayName = "Łódź"
        assertEquals("Łódź", player.displayName)
        assertEquals(Charset.forName("windows-1250").encode("Łódź").get().toInt(), player.bytes[4].toInt())
        player.changeByte(4 + 8, 0x7F)
        val before = save.toByteArray()
        assertEquals("Łódź", player.displayName)
        assertContentEquals(before, save.toByteArray())
        assertFailsWith<IllegalArgumentException> { player.displayName = "01234567890" }
        assertFailsWith<CharacterCodingException> { player.displayName = "城" }
        assertFailsWith<IllegalArgumentException> { player.displayName = "A\u0000B" }
        assertContentEquals(before, save.toByteArray())
        player.displayName = "AB"
        assertTrue(player.bytes.subList(6, 15).all { it == 0.toByte() })
        val utf8 = Save.parse(save.toByteArray(), Charsets.UTF_8)
        utf8.players[0].displayName = "城"
        assertEquals("城", utf8.players[0].displayName)
    }

    @Test
    fun `options prisoners handles and final byte decode from pinned layout`() {
        val save = blank()
        save.options.transitionAnimationsEnabled = 1
        save.options.musicVolumeRaw = -32
        assertEquals(1, save.bytes[SaveFormat.OPTIONS_FILE_OFFSET].toInt())
        assertEquals(-32, save.bytes[SaveFormat.OPTIONS_FILE_OFFSET + 26].toInt())
        val castle = save.buildingSlots[0]
        castle.prisonerSlotsRaw = listOf<Byte>(34, 4, 9, 2, 0x34, 0x12) + List(12) { (-1).toByte() }
        val prisoner = castle.typedPrisonerSlots()[0]
        assertEquals(34, prisoner.prisonerTypeId)
        assertEquals(4, prisoner.capturedOwnerPlayerIndex)
        assertEquals(9, prisoner.turnsHeld)
        assertEquals(2, prisoner.pendingAction)
        assertEquals(0x1234, prisoner.ransomValue)
        val army = save.armySlots[0]
        army.patch(721, byteArrayOf(-1, -1, -1, -1))
        assertEquals(0xFFFFFFFFL, army.armyFactHandle)
        assertFailsWith<IllegalArgumentException> { army.armyFactHandle = 0L }
        assertTrue(getClassDescriptor(army::class).getSimpleProperty("armyFactHandle")!!.isReadOnly())
        save.changeByte(SaveFormat.DAT_SIZE - 1, 0x7F)
        assertEquals(0x7F000000, save.portShorelineVariantFlag)
    }

    @Test
    fun `rectangular visible bounds keep fixed disk row stride`() {
        assertEquals(3934, tileIndexAt(34 * 8, 39 * 8, 8, 60, 40, 10000))
        assertEquals(1234, toIndex(12, 34, 60))
        assertEquals(12 to 34, fromIndex(1234, 60))
    }

    @Test
    fun `editable known flags preserve unknown high bits and diagnostic raw fields reject writes`() {
        val save = blank()
        val player = save.players[0]
        player.changeByte(47, 0xF8.toByte())
        player.changeByte(48, 0xA0.toByte())
        player.techLevel = 7
        player.lastReportedTechLevel = 3
        assertEquals(0xFF, player.bytes[47].toInt() and 255)
        assertEquals(0xA3, player.bytes[48].toInt() and 255)
        val castle = save.buildingSlots[0]
        castle.changeByte(416, 0xE0.toByte())
        castle.changeByte(420, 0xFE.toByte())
        castle.castleAddonFlags = 5
        castle.constructionLockFlags = 1
        assertEquals(0xE5, castle.bytes[416].toInt() and 255)
        assertEquals(0xFF, castle.bytes[420].toInt() and 255)
        val unit = save.armySlots[0].unitSlots[0]
        unit.changeByte(22, 0xA0.toByte())
        unit.defenseBonusFlag = 1
        assertEquals(0xA1, unit.stateBits2)
        val before = save.toByteArray()
        assertFailsWith<IllegalArgumentException> { unit.stateFlags = 0 }
        assertFailsWith<IllegalArgumentException> { unit.stanceBits = 0 }
        assertFailsWith<IllegalArgumentException> { unit.stateBits2 = 0 }
        assertFailsWith<IllegalArgumentException> { unit.auxRuntimeState = 0 }
        assertContentEquals(before, save.toByteArray())
        assertEquals("medium", getClassDescriptor(castle::class).getSimpleProperty("appearance")!!.confidence())
    }
    private fun blank(): Save {
        val bytes = ByteArray(SaveFormat.DAT_SIZE)
        repeat(SaveFormat.ARMY_RECORD_COUNT) { slot ->
            repeat(10) { unit -> write(bytes, SaveFormat.ARMY_RECORDS_FILE_OFFSET + slot * 725 + 6 + unit * 31, -1, 2) }
        }
        repeat(SaveFormat.BUILDING_RECORD_COUNT) { slot ->
            val base = SaveFormat.BUILDING_RECORDS_FILE_OFFSET + slot * 467
            write(bytes, base + 4, -1, 1)
            write(bytes, base + 16, -1, 2)
            repeat(12) { unit -> write(bytes, base + 18 + unit * 31, -1, 2) }
        }
        return Save.parse(bytes)
    }

    private fun write(bytes: ByteArray, offset: Int, value: Int, size: Int) {
        writeLittleEndianInt(value, size).toByteArray().copyInto(bytes, offset)
    }
}
