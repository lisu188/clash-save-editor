package com.lis.clash.desktop

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.geometry.Offset
import com.lis.clash.SaveFormat
import com.lis.clash.editor.*
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** Exercises rendered Compose controls and real document commands, without native file dialogs. */
class EditorUiTest {
    @get:Rule val compose = createComposeRule()
    private val actions = EditorActions({}, {}, {}, {}, {}, {})

    private fun show(state: EditorState) {
        compose.setContent { EditorWindow(state, actions, false, {}, {}, {}) }
    }

    @Test fun playerConfigurationDialogUpdatesDocument() {
        val state = EditorState(SaveDocument.newScenario())
        show(state)
        compose.onNodeWithText("Players").performClick()
        compose.onNodeWithText("Players & starting setup").assertExists()
        compose.onAllNodesWithText("Configure").onFirst().performClick()
        compose.onNodeWithText("Configure player 0").assertExists()
        compose.onNodeWithText("Name").performTextReplacement("Amber")
        compose.onNodeWithText("Apply").performClick()
        compose.runOnIdle { assertEquals("Amber", state.snapshot.players.first().name) }
    }

    @Test fun createArmyAndUndoThroughRenderedControls() {
        val document = SaveDocument.newScenario()
        document.execute(EditCommand.ConfigurePlayer(0, true, true, name = "Amber"))
        val state = EditorState(document)
        state.page = WorkspacePage.ARMIES
        show(state)
        compose.onNodeWithText("+ Add army").performClick()
        compose.onNodeWithText("Create army").assertExists()
        compose.onNodeWithText("Apply").performClick()
        compose.runOnIdle {
            assertEquals(1, state.snapshot.entities.size)
            assertEquals(RecordId(RecordKind.ARMY, 0), state.selection)
        }
        compose.onNodeWithText("↶").performClick()
        compose.runOnIdle { assertTrue(state.snapshot.entities.isEmpty()) }
    }

    @Test fun selectingFilteredSparseArmyKeepsPhysicalSlot() {
        val document = SaveDocument.newScenario()
        document.execute(EditCommand.ConfigurePlayer(0, true, true, name = "Amber"))
        val deleted = document.execute(EditCommand.CreateArmy(10, 10, 0))!!
        val surviving = document.execute(EditCommand.CreateArmy(12, 12, 0))!!
        document.execute(EditCommand.Delete(deleted))
        val state = EditorState(document)
        state.page = WorkspacePage.ARMIES
        state.query = "Army 1"
        show(state)
        compose.onNodeWithText("#1").performClick()
        compose.runOnIdle { assertEquals(surviving, state.selection) }
    }

    @Test fun actualCanvasClickResolvesRectangularMapPhysicalTile() {
        val draft = SaveDocument.newScenario()
        draft.execute(EditCommand.ConfigurePlayer(0, true, true, name = "Amber"))
        val bytes = draft.datBytes()
        // The on-disk first dimension is rows; snapshot dimensions are conventional width/height.
        bytes[SaveFormat.MAP_WIDTH_FILE_OFFSET] = 20
        bytes[SaveFormat.MAP_HEIGHT_FILE_OFFSET] = 40
        val state = EditorState(SaveDocument.fromDat(bytes, draft.facBytes()))
        show(state)
        val canvas = compose.onNodeWithContentDescription("World map, 40 columns and 20 rows. Click to select a tile or entity; drag to pan; scroll to zoom.")
        val bounds = canvas.fetchSemanticsNode().boundsInRoot
        val viewport = MapViewport.fit(bounds.width, bounds.height, rows = 20, columns = 40)
        canvas.performTouchInput { click(Offset(viewport.x + 7.5f * viewport.cellSize, viewport.y + 2.5f * viewport.cellSize)) }
        compose.runOnIdle { assertEquals(RecordId(RecordKind.TILE, 207), state.selection) }
    }

    @Test fun malformedCoordinatesStayInDialogWithoutChangingDocument() {
        val document = SaveDocument.newScenario()
        document.execute(EditCommand.ConfigurePlayer(0, true, true, name = "Amber"))
        document.markSaved()
        val dat = document.datBytes()
        val fac = document.facBytes()
        val revision = document.revision
        val state = EditorState(document).also { it.page = WorkspacePage.ARMIES }
        show(state)
        compose.onNodeWithText("+ Add army").performClick()
        compose.onNodeWithText("Row").performTextReplacement("3.5")
        compose.onNodeWithText("Apply").performClick()
        compose.onNodeWithText("Row must be a whole number.").assertExists()
        compose.runOnIdle {
            assertContentEquals(dat, document.datBytes())
            assertContentEquals(fac, document.facBytes())
            assertEquals(revision, document.revision)
            assertFalse(document.dirty)
        }
        compose.onNodeWithText("Row").performTextReplacement("3")
        compose.onNodeWithText("Apply").performClick()
        compose.onNodeWithText("Row must be a whole number.").assertDoesNotExist()
        compose.onNodeWithText("Apply").assertDoesNotExist()
        compose.runOnIdle { assertEquals(3, state.snapshot.entities.single().row) }
    }

    @Test fun rejectedFootprintKeepsInputsAvailableForCorrection() {
        val document = SaveDocument.newScenario()
        document.execute(EditCommand.ConfigurePlayer(0, true, true, name = "Amber"))
        val dat = document.datBytes()
        val fac = document.facBytes()
        val revision = document.revision
        val state = EditorState(document).also { it.page = WorkspacePage.ARMIES }
        show(state)
        compose.onNodeWithText("+ Add army").performClick()
        compose.onNodeWithText("Row").performTextReplacement("100")
        compose.onNodeWithText("Apply").performClick()
        compose.onNodeWithText("Footprint is outside the map").assertExists()
        compose.onNodeWithText("Unable to complete this action").assertDoesNotExist()
        compose.runOnIdle {
            assertContentEquals(dat, document.datBytes())
            assertContentEquals(fac, document.facBytes())
            assertEquals(revision, document.revision)
        }
        compose.onNodeWithText("Row").performTextReplacement("99")
        compose.onNodeWithText("Apply").performClick()
        compose.runOnIdle { assertEquals(99, state.snapshot.entities.single().row) }
    }

    @Test fun unsavedCloseCanBeCancelledOrExplicitlyDiscarded() {
        val state = EditorState(SaveDocument.newScenario())
        val guard = UnsavedChangesGuard()
        var closed = false
        guard.request(state.document.dirty) { closed = true }
        compose.setContent {
            EditorWindow(state, actions, guard.confirmationRequired, guard::discard, guard::cancel, {})
        }
        compose.onNodeWithText("Save your changes?").assertExists()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle {
            assertFalse(closed)
            assertFalse(guard.confirmationRequired)
            assertTrue(state.document.dirty)
            guard.request(state.document.dirty) { closed = true }
        }
        compose.onNodeWithText("Discard").performClick()
        compose.onNodeWithText("Save your changes?").assertDoesNotExist()
        compose.runOnIdle {
            assertTrue(closed)
            assertTrue(state.document.dirty)
        }
    }

    @Test fun unsavedCloseWaitsForSuccessfulSaveContinuation() {
        val state = EditorState(SaveDocument.newScenario())
        val guard = UnsavedChangesGuard()
        var closed = false
        var saveSucceeded = false
        var saveAttempts = 0
        guard.request(state.document.dirty) { closed = true }
        compose.setContent {
            EditorWindow(state, actions, guard.confirmationRequired, guard::discard, guard::cancel, {
                guard.saveAndContinue { continueAction ->
                    saveAttempts++
                    if (saveSucceeded) {
                        state.document.markSaved()
                        state.refresh()
                        continueAction()
                    }
                }
            })
        }
        compose.onNodeWithContentDescription("Save project and continue").performClick()
        compose.runOnIdle {
            assertEquals(1, saveAttempts)
            assertFalse(closed, "Cancelling or failing the save must keep the document open")
            assertTrue(state.document.dirty)
            saveSucceeded = true
            guard.request(state.document.dirty) { closed = true }
        }
        compose.onNodeWithContentDescription("Save project and continue").performClick()
        compose.runOnIdle {
            assertEquals(2, saveAttempts)
            assertTrue(closed)
            assertFalse(state.document.dirty)
        }
    }

    @Test fun encodingChooserReinterpretsNamesAndUndoPreservesSourceBytes() {
        val document = SaveDocument.newScenario()
        document.execute(EditCommand.ConfigurePlayer(0, true, true, name = "Łódź"))
        document.markSaved()
        val dat = document.datBytes()
        val fac = document.facBytes()
        val state = EditorState(document).also { it.page = WorkspacePage.SCENARIO }
        show(state)
        compose.onNodeWithText("windows-1250 ▾").performClick()
        compose.onNodeWithText("Windows-1252 · Western European").performClick()
        compose.runOnIdle {
            assertEquals("windows-1252", document.encoding)
            assertNotEquals("Łódź", state.snapshot.players.first().name)
            assertContentEquals(dat, document.datBytes())
            assertContentEquals(fac, document.facBytes())
            assertTrue(document.dirty)
        }
        compose.onNodeWithContentDescription("Undo").performClick()
        compose.runOnIdle {
            assertEquals("windows-1250", document.encoding)
            assertEquals("Łódź", state.snapshot.players.first().name)
            assertContentEquals(dat, document.datBytes())
            assertContentEquals(fac, document.facBytes())
            assertFalse(document.dirty)
        }
    }
}
