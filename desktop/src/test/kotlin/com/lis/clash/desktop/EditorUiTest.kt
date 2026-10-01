package com.lis.clash.desktop

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.geometry.Offset
import com.lis.clash.SaveFormat
import com.lis.clash.UnitTypes
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

    @Test fun startupWaitsForChoiceBeforeCreatingDirtyScenario() {
        val state = EditorState()
        val startupActions = actions.copy(newDocument = { state.install(SaveDocument.newScenario()) })
        compose.setContent { EditorWindow(state, startupActions, false, {}, {}, {}) }
        compose.onNodeWithText("Create scenario from scratch").assertIsEnabled()
        compose.onNodeWithText("Open existing save").assertIsEnabled()
        compose.onNodeWithText("World canvas").assertDoesNotExist()
        compose.onNodeWithText("Save project").assertDoesNotExist()
        compose.runOnIdle {
            assertFalse(state.hasDocument)
            state.undo()
            state.redo()
        }
        compose.onNodeWithText("Create scenario from scratch").performClick()
        compose.onNodeWithText("Unofficial Clash Save Editor").assertDoesNotExist()
        compose.onNodeWithText("World canvas").assertExists()
        compose.runOnIdle {
            assertTrue(state.hasDocument)
            assertTrue(state.document.dirty)
            assertTrue(state.snapshot.entities.isEmpty())
        }
    }

    @Test fun cancelledOrFailedOpenKeepsStartupUntilSuccessfulLoad() {
        val state = EditorState()
        var attempts = 0
        val startupActions = actions.copy(open = {
            attempts++
            when (attempts) {
                1 -> Unit // Native chooser cancelled: no document is installed.
                2 -> state.error = "This save could not be read."
                else -> state.install(SaveDocument.newScenario().also { it.markSaved() })
            }
        })
        compose.setContent { EditorWindow(state, startupActions, false, {}, {}, {}) }
        compose.onNodeWithText("Open existing save").performClick()
        compose.onNodeWithText("Unofficial Clash Save Editor").assertExists()
        compose.runOnIdle { assertFalse(state.hasDocument) }
        compose.onNodeWithText("Open existing save").performClick()
        compose.onNodeWithText("This save could not be read.").assertExists()
        compose.onNodeWithText("Understood").performClick()
        compose.onNodeWithText("Unofficial Clash Save Editor").assertExists()
        compose.runOnIdle { assertFalse(state.hasDocument) }
        compose.onNodeWithText("Open existing save").performClick()
        compose.onNodeWithText("World canvas").assertExists()
        compose.runOnIdle { assertFalse(state.document.dirty) }
    }

    @Test fun interruptedSaveCanBeRecoveredBeforeDocumentIsOpen() {
        val state = EditorState()
        val path = java.nio.file.Path.of("interrupted.dat")
        state.recoveryPath = path
        val startupActions = actions.copy(recover = {
            state.install(SaveDocument.newScenario().also { it.markSaved() })
        })
        compose.setContent { EditorWindow(state, startupActions, false, {}, {}, {}) }
        compose.onNodeWithText("Recover interrupted game save").assertExists()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Unofficial Clash Save Editor").assertExists()
        compose.runOnIdle {
            assertFalse(state.hasDocument)
            state.recoveryPath = path
        }
        compose.onNodeWithText("Restore & open").performClick()
        compose.onNodeWithText("Recover interrupted game save").assertDoesNotExist()
        compose.onNodeWithText("World canvas").assertExists()
        compose.runOnIdle { assertTrue(state.hasDocument) }
    }

    @Test fun recentFileChoiceWaitsForSuccessfulInstall() {
        val preferences = java.util.prefs.Preferences.userNodeForPackage(EditorState::class.java)
        val previousRecent = preferences.get("recent", null)
        val path = java.nio.file.Path.of("recent-choice-ui-test.clashproj").toAbsolutePath()
        try {
            // Keep this test independent of the user's recent files and restore their list afterwards.
            preferences.put("recent", path.toString())
            val state = EditorState()
            var requestedPath: String? = null
            val startupActions = actions.copy(openRecent = { requestedPath = it })
            compose.setContent { EditorWindow(state, startupActions, false, {}, {}, {}) }
            compose.onNodeWithText(path.fileName.toString()).performClick()
            compose.onNodeWithText("Unofficial Clash Save Editor").assertExists()
            compose.runOnIdle {
                assertEquals(path.toString(), requestedPath)
                assertFalse(state.hasDocument)
                state.install(SaveDocument.newScenario().also { it.markSaved() }, source = path)
            }
            compose.onNodeWithText("Unofficial Clash Save Editor").assertDoesNotExist()
            compose.onNodeWithText("World canvas").assertExists()
            compose.runOnIdle {
                assertTrue(state.hasDocument)
                assertFalse(state.document.dirty)
            }
        } finally {
            if (previousRecent == null) preferences.remove("recent") else preferences.put("recent", previousRecent)
        }
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

    @Test fun playerSwitchRowsAndNamedChoicesConfigureComputerPlayer() {
        val state = EditorState(SaveDocument.newScenario())
        show(state)
        compose.onNodeWithText("Players").performClick()
        compose.onAllNodesWithText("Configure").onFirst().performClick()
        compose.onNodeWithText("Name").performTextReplacement("Azure")
        compose.onNodeWithText("Active").assertIsOff().performClick().assertIsOn()
        compose.onNodeWithText("Human").assertIsOn().performClick().assertIsOff()
        compose.onNodeWithContentDescription("Intelligence: Easy").performScrollTo().performClick()
        compose.onNodeWithText("Hard").performClick()
        compose.onNodeWithContentDescription("Religion: Christian").performScrollTo().performClick()
        compose.onNodeWithText("Pagan").performClick()
        compose.onNodeWithText("Apply").performClick()
        compose.runOnIdle {
            val player = state.snapshot.players.first()
            assertEquals("Azure", player.name)
            assertTrue(player.active)
            assertFalse(player.human)
            assertEquals(2, player.intelligence)
            assertEquals(0, player.religion)
        }
    }

    @Test fun namedArmySelectorsUseActivePlayerAndUnitIds() {
        val document = SaveDocument.newScenario()
        document.execute(EditCommand.ConfigurePlayer(0, true, true, name = "Amber"))
        document.execute(EditCommand.ConfigurePlayer(1, true, true, name = "Azure"))
        document.execute(EditCommand.ConfigurePlayer(2, false, false, name = "Dormant"))
        val state = EditorState(document).also { it.page = WorkspacePage.ARMIES }
        show(state)
        compose.onNodeWithText("+ Add army").performClick()
        compose.onNodeWithContentDescription("Player: Amber · Player 0").performClick()
        compose.onNodeWithText("Dormant · Player 2").assertDoesNotExist()
        compose.onNodeWithText("Azure · Player 1").performClick()
        val firstUnitName = requireNotNull(UnitTypes.metadata(0)).displayName
        val chosenUnitName = requireNotNull(UnitTypes.metadata(1)).displayName
        compose.onNodeWithContentDescription("Unit type: $firstUnitName").performClick()
        compose.onNodeWithText(chosenUnitName).performClick()
        compose.onNodeWithText("Apply").performClick()
        compose.runOnIdle {
            val army = state.snapshot.entities.single()
            assertEquals(RecordId(RecordKind.ARMY, 0), army.id)
            assertEquals(1, army.owner)
            assertEquals(1, army.units.single().type)
        }
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
        compose.onNodeWithContentDescription("Undo").performClick()
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

    @Test fun clearFiltersRestoresRowsWithoutLosingPhysicalSelection() {
        val document = SaveDocument.newScenario()
        document.execute(EditCommand.ConfigurePlayer(0, true, true, name = "Amber"))
        document.execute(EditCommand.ConfigurePlayer(1, true, true, name = "Azure"))
        document.execute(EditCommand.CreateArmy(10, 10, 0))
        val selected = document.execute(EditCommand.CreateArmy(20, 20, 1))!!
        document.markSaved()
        val revision = document.revision
        val state = EditorState(document).also {
            it.page = WorkspacePage.ARMIES
            it.selection = selected
            it.ownerFilter = 0
            it.query = "no matching formation"
        }
        show(state)
        compose.onNodeWithText("No matching armies").assertExists()
        compose.onNodeWithText("Clear filters").performClick()
        compose.onNodeWithText("#0").assertExists()
        compose.onNodeWithText("#1").assertExists()
        compose.onNodeWithText("No matching armies").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals("", state.query)
            assertEquals(null, state.ownerFilter)
            assertEquals(selected, state.selection)
            assertEquals(revision, document.revision)
            assertFalse(document.dirty)
        }
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

    @Test fun showOnMapCentersDistantArmyForActualCanvasSelection() {
        val document = SaveDocument.newScenario()
        document.execute(EditCommand.ConfigurePlayer(0, true, true, name = "Amber"))
        val army = document.execute(EditCommand.CreateArmy(80, 80, 0))!!
        val state = EditorState(document).also { it.page = WorkspacePage.ARMIES }
        show(state)
        compose.runOnIdle { state.showOnMap(army) }
        compose.onNodeWithText("World canvas").assertExists()
        compose.runOnIdle {
            assertTrue(state.inspectorOpen)
            // Clear the selection without another focus request so the click must resolve its target.
            state.selection = null
        }
        val canvas = compose.onNodeWithContentDescription("World map, 100 columns and 100 rows. Click to select a tile or entity; drag to pan; scroll to zoom.")
        val bounds = canvas.fetchSemanticsNode().boundsInRoot
        canvas.performTouchInput { click(Offset(bounds.width / 2, bounds.height / 2)) }
        compose.runOnIdle { assertEquals(army, state.selection) }
    }

    @Test fun trapBrushDefaultsToTheOnlyActivePlayer() {
        val document = SaveDocument.newScenario()
        document.execute(EditCommand.ConfigurePlayer(1, true, true, name = "Azure"))
        document.markSaved()
        val state = EditorState(document).also { it.inspectorOpen = false }
        show(state)
        compose.onNodeWithText("Paint").performClick()
        compose.onNodeWithText("Place traps").performClick()
        compose.onNodeWithContentDescription("Trap player: Azure").assertExists()
        compose.runOnIdle {
            assertEquals(MapTool.TRAP, state.tool)
            assertEquals("2", state.paintValue)
        }
        val canvas = compose.onNodeWithContentDescription("World map, 100 columns and 100 rows. Click to select a tile or entity; drag to pan; scroll to zoom.")
        val bounds = canvas.fetchSemanticsNode().boundsInRoot
        val viewport = MapViewport.fit(bounds.width, bounds.height, rows = 100, columns = 100)
        val row = 40
        val column = 50
        canvas.performTouchInput {
            click(Offset(viewport.x + (column + .5f) * viewport.cellSize, viewport.y + (row + .5f) * viewport.cellSize))
        }
        compose.runOnIdle {
            val slot = row * 100 + column
            assertEquals(2, document.datBytes()[SaveFormat.TRAP_MASK_RECORDS_FILE_OFFSET + slot].toInt() and 255)
            assertEquals(RecordId(RecordKind.TILE, slot), state.selection)
            assertEquals(null, state.error)
            assertTrue(document.dirty)
        }
    }

    @Test fun propertyModalCancelPreservesDocumentAndApplyCommits() {
        val document = SaveDocument.newScenario("Before")
        document.markSaved()
        val originalDat = document.datBytes()
        val originalFac = document.facBytes()
        val revision = document.revision
        val state = EditorState(document).also { it.page = WorkspacePage.SCENARIO }
        show(state)
        compose.onNodeWithContentDescription("Edit Name").performScrollTo().performClick()
        compose.onNode(hasText("Name") and hasSetTextAction()).performTextReplacement("Discarded draft")
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle {
            assertContentEquals(originalDat, document.datBytes())
            assertContentEquals(originalFac, document.facBytes())
            assertEquals(revision, document.revision)
            assertEquals("Before", state.snapshot.name)
            assertFalse(document.dirty)
        }
        compose.onNodeWithContentDescription("Edit Name").performScrollTo().performClick()
        compose.onNode(hasText("Name") and hasSetTextAction()).assertTextContains("Before")
            .performTextReplacement("Northern Reach")
        compose.onNodeWithText("Apply").performClick()
        compose.onNodeWithText("Apply").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals("Northern Reach", state.snapshot.name)
            assertEquals(revision + 1, document.revision)
            assertTrue(document.dirty)
            assertFalse(originalDat.contentEquals(document.datBytes()))
            assertContentEquals(originalFac, document.facBytes())
        }
    }

    @Test fun exportChecksRemainVisibleUntilDocumentChanges() {
        val document = SaveDocument.newScenario().also { it.markSaved() }
        val state = EditorState(document).also { it.bottomOpen = true }
        show(state)
        compose.onNodeWithText("Document structure").assertExists()
        compose.onNodeWithText("Check playable save").performClick()
        compose.onNodeWithText("Playable save checks").assertExists()
        val issue = "Playable export requires at least two active players"
        compose.onNodeWithText(issue).assertIsDisplayed()
        compose.runOnIdle {
            val checks = requireNotNull(state.exportIssues)
            assertTrue(checks.any { it.message == issue })
            assertTrue(state.snapshot.issues.none { it.message == issue })
            assertFalse(document.dirty)
            val checkedRevision = document.revision
            document.markSaved()
            state.refresh("Project saved")
            assertEquals(checkedRevision, document.revision)
            assertEquals(checks, state.exportIssues)
        }
        compose.onNodeWithText("Playable save checks").assertExists()
        compose.onNodeWithText(issue).assertIsDisplayed()
        compose.onNodeWithText("Report").performClick()
        compose.onNodeWithContentDescription("Diagnostics tab").performClick()
        compose.onNodeWithText(issue).assertIsDisplayed()
        compose.runOnIdle {
            state.execute(EditCommand.SetProperty(RecordId(RecordKind.SAVE), "name", "Changed world"))
            assertEquals(null, state.exportIssues)
            assertTrue(document.dirty)
        }
        compose.onNodeWithText("Document structure").assertExists()
        compose.onNodeWithText("Playable save checks").assertDoesNotExist()
        compose.onNodeWithText(issue).assertDoesNotExist()
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
