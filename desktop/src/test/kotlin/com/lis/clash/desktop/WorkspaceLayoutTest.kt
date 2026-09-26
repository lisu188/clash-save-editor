package com.lis.clash.desktop

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.*
import com.lis.clash.SaveFormat
import com.lis.clash.editor.EditCommand
import com.lis.clash.editor.RecordId
import com.lis.clash.editor.RecordKind
import com.lis.clash.editor.SaveDocument
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.math.sin
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Renders the real workspace in a Skiko Compose scene, without opening or capturing native windows.
 * Set the test JVM property clash.preview.dir to retain PNG evidence of each viewport and theme.
 * Fixtures use synthetic map bytes and document commands, with no original game assets or saves.
 */
@OptIn(ExperimentalTestApi::class)
@Suppress("DEPRECATION")
@RunWith(Parameterized::class)
class WorkspaceLayoutTest(private val width: Int, private val height: Int, private val dark: Boolean) {
    @Test fun welcomeMapPlayersAndDialogKeepTheirActionsVisible() = runDesktopComposeUiTest(width = width, height = height) {
        val state = EditorState().apply { darkMode = dark }
        var requestedOpen = false
        var requestedSave = false
        var requestedExport = false
        val actions = EditorActions(
            newDocument = { state.install(SaveDocument.newScenario()) },
            open = { requestedOpen = true }, openRecent = {},
            saveProject = { requestedSave = true }, export = { requestedExport = true }, close = {}
        )
        setContent { EditorWindow(state, actions, false, {}, {}, {}) }

        assertActionVisible(onNodeWithText("Create scenario from scratch"))
        assertActionVisible(onNodeWithText("Open existing save"))
        savePreview("welcome", captureToImage())
        onNodeWithText("Open existing save").performClick()
        runOnIdle { assertTrue(requestedOpen); assertFalse(state.hasDocument) }
        onNodeWithText("Create scenario from scratch").performClick()
        runOnIdle { assertTrue(state.hasDocument) }

        val fixture = previewDocument()
        runOnIdle {
            state.install(fixture)
            state.select(RecordId(RecordKind.ARMY, 0))
        }
        assertActionVisible(onNode(hasText("Save project") and hasClickAction()))
        assertActionVisible(onNodeWithText("Export save"))
        assertActionVisible(onNodeWithText("Move"))
        assertActionVisible(onNodeWithText("Locate"))
        onNodeWithContentDescription("World map, 100 columns and 100 rows.", substring = true).assertIsDisplayed()
        savePreview("map-selected", captureToImage())
        onNode(hasText("Save project") and hasClickAction()).performClick()
        onNodeWithText("Export save").performClick()
        runOnIdle {
            assertTrue(requestedSave)
            assertTrue(requestedExport)
            assertEquals(RecordId(RecordKind.ARMY, 0), state.selection)
        }

        onNodeWithText("Players").performClick()
        onNodeWithText("Players & starting setup").assertIsDisplayed()
        assertActionVisible(onAllNodesWithText("Configure").onFirst())
        savePreview("players", captureToImage())
        onAllNodesWithText("Configure").onFirst().performClick()
        onNodeWithText("Configure player 0").assertIsDisplayed()
        assertActionVisible(onNodeWithText("Apply"))
        assertActionVisible(onNodeWithText("Cancel"))
        savePreview("player-dialog", captureToImage())
        onNodeWithText("Cancel").performClick()
        onNodeWithText("Configure player 0").assertDoesNotExist()
    }

    @Test fun explicitInspectorToggleWorksWithoutASelection() = runDesktopComposeUiTest(width = width, height = height) {
        val state = EditorState(SaveDocument.newScenario()).apply { darkMode = dark }
        val actions = EditorActions({}, {}, {}, {}, {}, {})
        setContent { EditorWindow(state, actions, false, {}, {}, {}) }
        runOnIdle { assertEquals(null, state.selection) }

        if (width < 1180) {
            onNodeWithText("Build your scenario").assertDoesNotExist()
            onNodeWithContentDescription("Show inspector").performClick()
        }
        onNodeWithText("Build your scenario").assertIsDisplayed()
        assertActionVisible(onNodeWithContentDescription("Hide inspector"))
        onNodeWithContentDescription("Hide inspector").performClick()
        onNodeWithText("Build your scenario").assertDoesNotExist()
        assertActionVisible(onNodeWithContentDescription("Show inspector"))
        onNodeWithContentDescription("Show inspector").performClick()
        onNodeWithText("Build your scenario").assertIsDisplayed()
        assertActionVisible(onNode(hasText("Save project") and hasClickAction()))
        assertActionVisible(onNodeWithText("Export save"))
        runOnIdle { assertEquals(null, state.selection) }
        savePreview("draft-inspector", captureToImage())
    }

    private fun assertActionVisible(node: SemanticsNodeInteraction) {
        node.assertIsDisplayed().assertIsEnabled().assertHasClickAction()
        // assertIsDisplayed alone permits partial clipping. These un-clipped root bounds also
        // require every action to fit wholly inside the explicit test viewport at density 1.
        val bounds = node.getUnclippedBoundsInRoot()
        assertTrue(bounds.left.value >= -0.5f && bounds.top.value >= -0.5f,
            "Action begins outside $width x $height: $bounds")
        assertTrue(bounds.right.value <= width + 0.5f && bounds.bottom.value <= height + 0.5f,
            "Action extends outside $width x $height: $bounds")
    }

    private fun savePreview(screen: String, bitmap: ImageBitmap) {
        assertEquals(width, bitmap.width, "The screenshot must match the requested offscreen viewport")
        assertEquals(height, bitmap.height, "The screenshot must match the requested offscreen viewport")
        val destination = System.getProperty("clash.preview.dir")?.takeIf { it.isNotBlank() } ?: return
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.readPixels(pixels)
        val image = BufferedImage(bitmap.width, bitmap.height, BufferedImage.TYPE_INT_ARGB)
        image.setRGB(0, 0, bitmap.width, bitmap.height, pixels, 0, bitmap.width)
        val directory = Path.of(destination).toAbsolutePath()
        Files.createDirectories(directory)
        val target = directory.resolve("$screen-${width}x$height-${if (dark) "dark" else "light"}.png")
        check(ImageIO.write(image, "png", target.toFile())) { "No PNG writer was available" }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}x{1}, dark={2}")
        fun viewports(): List<Array<Any>> = listOf(
            arrayOf(980, 700, false), arrayOf(980, 700, true),
            arrayOf(1440, 960, false), arrayOf(1440, 960, true)
        )

        private fun previewDocument(): SaveDocument {
            val blank = SaveDocument.newScenario("Verdant March")
            val importedBytes = blank.datBytes()
            val river = (0 until 100).flatMap { row ->
                val col = 52 + (sin(row / 12.0) * 5).toInt()
                (col..col + 2).map { row * 100 + it }
            }
            // Exercise rendering of imported water. Creating water through brushes is deliberately
            // unsupported; this synthetic DAT fixture does not bypass that production policy.
            river.forEach { slot ->
                val offset = SaveFormat.TILE_RECORDS_FILE_OFFSET + slot * SaveFormat.TILE_RECORD_SIZE
                importedBytes[offset] = (603 and 255).toByte()
                importedBytes[offset + 1] = (603 ushr 8).toByte()
            }
            return SaveDocument.fromDat(importedBytes, blank.facBytes()).apply {
                val names = listOf("Copper", "Azure", "Northland", "Violet", "Greenvale")
                names.forEachIndexed { slot, name -> execute(EditCommand.ConfigurePlayer(slot, true, slot < 2, name = name)) }
                execute(EditCommand.PaintTiles((10..31).map { 29 * 100 + it }, road = 872))
                execute(EditCommand.CreateArmy(22, 20, 0, 1))
                execute(EditCommand.CreateArmy(35, 70, 1, 2))
                execute(EditCommand.CreateArmy(65, 30, 2, 3))
                execute(EditCommand.CreateArmy(72, 72, 3, 4))
                execute(EditCommand.CreateArmy(45, 32, 4, 5))
                execute(EditCommand.CreateBuilding(28, 33, 0, 2, "Oakkeep"))
                execute(EditCommand.CreateBuilding(35, 64, 1, 2, "Azurekeep"))
                markSaved()
            }
        }
    }
}
