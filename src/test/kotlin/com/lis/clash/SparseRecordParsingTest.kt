package com.lis.clash

import com.lis.clash.objects.Save
import kotlin.test.Test
import kotlin.test.assertEquals

class SparseRecordParsingTest {
    @Test
    fun `fixed army and building tables scan past unused slots`() {
        val bytes = ByteArray(Save.EXPECTED_FILE_SIZE)

        repeat(Save.ARMY_COUNT) { armyIndex ->
            writeLittleEndian(bytes, 147190 + armyIndex * 725 + 6, -1, 2)
        }
        repeat(Save.BUILDING_COUNT) { buildingIndex ->
            writeLittleEndian(bytes, 509690 + buildingIndex * 467 + 4, -1, 1)
            writeLittleEndian(bytes, 509690 + buildingIndex * 467 + 16, -1, 2)
        }

        writeLittleEndian(bytes, 147190 + 6, 1, 2)
        writeLittleEndian(bytes, 147190 + 2 * 725 + 6, 2, 2)

        writeLittleEndian(bytes, 509690 + 4, 2, 1)
        writeLittleEndian(bytes, 509690 + 16, 0, 2)
        writeLittleEndian(bytes, 509690 + 2 * 467 + 4, 1, 1)
        writeLittleEndian(bytes, 509690 + 2 * 467 + 16, 0, 2)

        val save = Save().withBytes<Save>(bytes.toList())

        assertEquals(2, save.armies.size)
        assertEquals(147190, save.armies[0].index)
        assertEquals(147190 + 2 * 725, save.armies[1].index)

        assertEquals(2, save.castles.size)
        assertEquals(509690, save.castles[0].index)
        assertEquals(509690 + 2 * 467, save.castles[1].index)
    }

    private fun writeLittleEndian(bytes: ByteArray, offset: Int, value: Int, length: Int) {
        writeLittleEndianInt(value, length).forEachIndexed { index, byte ->
            bytes[offset + index] = byte
        }
    }
}
