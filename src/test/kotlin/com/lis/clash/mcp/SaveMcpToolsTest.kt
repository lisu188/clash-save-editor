package com.lis.clash.mcp

import com.lis.clash.SaveFormat
import com.lis.clash.objects.Save
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SaveMcpToolsTest {
    @Test
    fun `schema and object reads expose complete parsed save data`() {
        val input = writeSyntheticSave()
        val tools = SaveMcpTools()

        val schema = tools.call("save_get_schema", mapOf("entityType" to "Tile"))
        assertFalse(schema.isError)
        val schemaContent = schema.structuredContent.asMap()
        assertEquals(SaveFormat.DAT_SIZE, schemaContent["minimumRepresentedSaveSize"])
        assertEquals(SaveFormat.DAT_SIZE, schemaContent["exactSaveSize"])

        val unitSchema = tools.call("save_get_schema", mapOf("entityType" to "Unit"))
        assertFalse(unitSchema.isError)
        val unitSchemaContent = unitSchema.structuredContent.asMap()
        val unitType = unitSchemaContent["types"].asMap()["unit"].asMap()
        val unitProperties = unitType["simpleProperties"].asList().map { it.asMap() }
        val experience = unitProperties.single { it["name"] == "experienceLevel" }
        assertEquals(12, experience["offset"])
        assertEquals("maskedLittleEndian", experience["encoding"])
        assertEquals(0x03, experience["mask"])
        assertEquals(3, experience["maximum"])

        val objectRead = tools.call(
            "save_read_object",
            mapOf(
                "path" to input.absolutePath,
                "objectPath" to "tiles[0]",
                "includeBytes" to true,
                "byteLimit" to 6
            )
        )

        assertFalse(objectRead.isError)
        val content = objectRead.structuredContent.asMap()
        assertEquals("Tile", content["type"])
        assertEquals(SaveFormat.TILE_RECORDS_FILE_OFFSET, content["absoluteByteIndex"])
        assertEquals(SaveFormat.TILE_RECORD_SIZE, content["byteLength"])
        val properties = content["properties"].asMap()
        assertEquals(321, properties["terrainTileId"])
        assertEquals(654, properties["overlayTileId"])
        val bytes = content["bytes"].asMap()
        assertEquals(6, bytes["retunedLength"])
        assertEquals("0x41 0x01 0x8E 0x02 0x68 0x03", bytes["hex"])

        val occupancyRead = tools.call(
            "save_read_object",
            mapOf(
                "path" to input.absolutePath,
                "objectPath" to "tileOccupancy[1]"
            )
        )
        assertFalse(occupancyRead.isError)
        val occupancy = occupancyRead.structuredContent.asMap()
        assertEquals(7, occupancy["properties"].asMap()["rawValue"])
        assertEquals(7, occupancy["derived"].asMap()["armyStackIndex"])
    }

    @Test
    fun `property edits write a modified exact-size output save`() {
        val input = writeSyntheticSave()
        val output = File(input.parentFile, "edited.dat")
        val tools = SaveMcpTools()

        val result = tools.call(
            "save_set_property",
            mapOf(
                "path" to input.absolutePath,
                "objectPath" to "players[0]",
                "property" to "displayName",
                "value" to "Changed",
                "outputPath" to output.absolutePath
            )
        )

        assertFalse(result.isError)
        assertEquals(SaveFormat.DAT_SIZE, output.length().toInt())
        assertEquals("Player One", Save.parse(input.readBytes()).players.first().displayName)
        assertEquals("Changed", Save.parse(output.readBytes()).players.first().displayName)
        val content = result.structuredContent.asMap()
        assertEquals("Player One", content["before"])
        assertEquals("Changed", content["after"])
    }

    @Test
    fun `experience property edits preserve other unit byte bits`() {
        val input = writeSyntheticSave()
        val output = File(input.parentFile, "experienced.dat")
        val tools = SaveMcpTools()

        val result = tools.call(
            "save_set_property",
            mapOf(
                "path" to input.absolutePath,
                "objectPath" to "armies[0].units[0]",
                "property" to "experienceLevel",
                "value" to 3,
                "outputPath" to output.absolutePath
            )
        )

        assertFalse(result.isError)
        val originalUnit = Save.parse(input.readBytes()).armies.first().units.first()
        val editedUnit = Save.parse(output.readBytes()).armies.first().units.first()
        assertEquals(0x12, originalUnit.stanceBits)
        assertEquals(2, originalUnit.experienceLevel)
        assertEquals(0x13, editedUnit.stanceBits)
        assertEquals(3, editedUnit.experienceLevel)
        val content = result.structuredContent.asMap()
        assertEquals(2, content["before"])
        assertEquals(3, content["after"])
        assertEquals(147208, content["absoluteOffset"])
    }

    @Test
    fun `experience progress edits preserve experience tier`() {
        val input = writeSyntheticSave()
        val output = File(input.parentFile, "progress.dat")
        val tools = SaveMcpTools()

        val result = tools.call(
            "save_set_property",
            mapOf(
                "path" to input.absolutePath,
                "objectPath" to "armies[0].units[0]",
                "property" to "experienceProgress",
                "value" to 3,
                "outputPath" to output.absolutePath
            )
        )

        assertFalse(result.isError)
        val editedUnit = Save.parse(output.readBytes()).armies.first().units.first()
        assertEquals(2, editedUnit.experienceLevel)
        assertEquals(3, editedUnit.experienceProgress)
        assertEquals(0x1E, editedUnit.stanceBits)
    }

    @Test
    fun `raw byte writes preserve length and input`() {
        val input = writeSyntheticSave()
        val output = File(input.parentFile, "raw-edited.dat")
        val tools = SaveMcpTools()

        val result = tools.call(
            "save_write_bytes",
            mapOf(
                "path" to input.absolutePath,
                "offset" to 0,
                "values" to "41 42 43",
                "outputPath" to output.absolutePath
            )
        )

        assertFalse(result.isError)
        assertEquals("TEST SAVE", Save.parse(input.readBytes()).name)
        assertEquals(SaveFormat.DAT_SIZE, output.length().toInt())
        assertEquals(listOf('A'.code.toByte(), 'B'.code.toByte(), 'C'.code.toByte()), output.readBytes().take(3))
    }

    @Test
    fun `server responds to initialize and tool listing`() {
        val server = McpStdioServer()
        val initialize = Json.parse(
            server.handleMessage(
                """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26"}}"""
            ) ?: error("Expected response")
        ).asMap()

        val initializeResult = initialize["result"].asMap()
        assertEquals("2025-03-26", initializeResult["protocolVersion"])
        assertNotNull(initializeResult["serverInfo"])

        val listTools = Json.parse(
            server.handleMessage("""{"jsonrpc":"2.0","id":"tools","method":"tools/list"}""")
                ?: error("Expected response")
        ).asMap()

        val tools = listTools["result"].asMap()["tools"].asList()
        assertTrue(tools.any { it.asMap()["name"] == "save_read_object" })
        assertTrue(tools.any { it.asMap()["name"] == "save_set_property" })
    }

    private fun writeSyntheticSave(): File {
        val directory = createTempDirectory("clash-save-mcp").toFile()
        val file = File(directory, "save.dat")
        file.writeBytes(buildSyntheticSave())
        return file
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
        writeLittleEndian(bytes, SaveFormat.MAP_THEME_FILE_OFFSET, 2, 1)
        writeLittleEndian(bytes, SaveFormat.ACTIVE_MISSION_FILE_OFFSET, -1, 4)

        val playerBase = SaveFormat.PLAYER_RECORDS_FILE_OFFSET
        writeLittleEndian(bytes, playerBase, 1, 4)
        writeString(bytes, playerBase + 4, 11, "Player One")
        writeLittleEndian(bytes, playerBase + 31, 2, 4)
        bytes[playerBase + 57] = 0x0F

        val armyBase = SaveFormat.ARMY_RECORDS_FILE_OFFSET
        writeLittleEndian(bytes, armyBase, 12, 2)
        writeLittleEndian(bytes, armyBase + 2, 34, 2)
        writeLittleEndian(bytes, armyBase + 4, 1, 1)
        writeLittleEndian(bytes, armyBase + 5, 6, 1)
        writeLittleEndian(bytes, armyBase + 6, 34, 2)
        writeLittleEndian(bytes, armyBase + 8, 1, 1)
        writeLittleEndian(bytes, armyBase + 14, 5, 1)
        writeLittleEndian(bytes, armyBase + 15, 80, 1)
        writeLittleEndian(bytes, armyBase + 18, 0x12, 1)
        writeLittleEndian(bytes, armyBase + 19, 0x04, 1)

        val castleBase = SaveFormat.BUILDING_RECORDS_FILE_OFFSET
        writeLittleEndian(bytes, castleBase, 44, 1)
        writeLittleEndian(bytes, castleBase + 1, 55, 1)
        writeLittleEndian(bytes, castleBase + 2, 2, 1)
        writeLittleEndian(bytes, castleBase + 4, 2, 1)
        writeString(bytes, castleBase + 5, 11, "CastleOne")
        writeLittleEndian(bytes, castleBase + 16, 0, 2)
        repeat(12) { slotIndex ->
            writeLittleEndian(bytes, castleBase + 402 + slotIndex, 0xFF, 1)
        }
        writeLittleEndian(bytes, castleBase + 402, 0, 1)
        writeLittleEndian(bytes, castleBase + 403, 2, 1)

        writeLittleEndian(bytes, SaveFormat.OCCUPANCY_RECORDS_FILE_OFFSET + 2, 7, 2)
        writeLittleEndian(bytes, SaveFormat.TRAP_MASK_RECORDS_FILE_OFFSET, 0b00000101, 1)
        writeLittleEndian(bytes, SaveFormat.PORT_ROW_FILE_OFFSET, 14, 4)
        writeLittleEndian(bytes, SaveFormat.PORT_COLUMN_FILE_OFFSET, 22, 4)

        return bytes
    }

    private fun writeString(bytes: ByteArray, offset: Int, length: Int, value: String) {
        val encoded = value.encodeToByteArray()
        repeat(length) { index ->
            bytes[offset + index] = encoded.getOrElse(index) { 0 }
        }
    }

    private fun writeLittleEndian(bytes: ByteArray, offset: Int, value: Int, length: Int) {
        repeat(length) { index ->
            bytes[offset + index] = ((value ushr (index * 8)) and 0xFF).toByte()
        }
    }

    private fun Any?.asMap(): Map<String, Any?> {
        @Suppress("UNCHECKED_CAST")
        return this as? Map<String, Any?> ?: error("Expected map, got ${this?.let { it::class.simpleName }}")
    }

    private fun Any?.asList(): List<Any?> {
        return this as? List<Any?> ?: error("Expected list, got ${this?.let { it::class.simpleName }}")
    }
}
