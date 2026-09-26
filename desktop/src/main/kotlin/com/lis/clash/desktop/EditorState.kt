package com.lis.clash.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lis.clash.editor.*
import java.nio.file.Path
import java.util.prefs.Preferences

enum class WorkspacePage(val label: String, val symbol: String) {
    MAP("World map", "◈"), ARMIES("Armies", "⚑"), BUILDINGS("Buildings", "▥"),
    PLAYERS("Players", "◉"), SCENARIO("Scenario", "☷")
}

enum class MapTool(val label: String) { SELECT("Select / pan"), TERRAIN("Paint terrain"), OVERLAY("Paint overlay"), ROAD("Paint road"), TRAP("Place traps") }

/** Shared by close, open and new so a cancelled save never performs the pending action. */
internal class UnsavedChangesGuard {
    private var pendingAction by mutableStateOf<(() -> Unit)?>(null)
    val confirmationRequired: Boolean get() = pendingAction != null

    fun request(dirty: Boolean, action: () -> Unit) {
        if (dirty) pendingAction = action else action()
    }

    fun cancel() { pendingAction = null }

    fun discard() {
        val action = pendingAction
        pendingAction = null
        action?.invoke()
    }

    fun saveAndContinue(save: (() -> Unit) -> Unit) {
        val action = pendingAction ?: return
        pendingAction = null
        save(action)
    }
}

class EditorState(initial: SaveDocument? = null) {
    private var currentDocument by mutableStateOf(initial)
    private var currentSnapshot by mutableStateOf(initial?.snapshot())
    val hasDocument: Boolean get() = currentDocument != null
    val document: SaveDocument get() = checkNotNull(currentDocument) { "No document is open" }
    val snapshot: DocumentSnapshot get() = checkNotNull(currentSnapshot) { "No document is open" }
    var selection by mutableStateOf<RecordId?>(null)
    var page by mutableStateOf(WorkspacePage.MAP)
    var query by mutableStateOf("")
    var ownerFilter by mutableStateOf<Int?>(null)
    var tool by mutableStateOf(MapTool.SELECT)
    var paintValue by mutableStateOf("0")
    var projectPath by mutableStateOf<Path?>(null)
    var sourcePath by mutableStateOf<Path?>(null)
    var busy by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var recoveryPath by mutableStateOf<Path?>(null)
    var notice by mutableStateOf("Ready · Open a save or build a new scenario")
    var bottomOpen by mutableStateOf(true)
    var bottomTab by mutableStateOf(0)
    var showGrid by mutableStateOf(false)
    var showTerrain by mutableStateOf(true)
    var showArmies by mutableStateOf(true)
    var showBuildings by mutableStateOf(true)
    var showRoads by mutableStateOf(true)
    var showOccupancy by mutableStateOf(false)
    val showEntities: Boolean get() = showArmies || showBuildings
    var showOverlays by mutableStateOf(true)
    var showTraps by mutableStateOf(true)
    var darkMode by mutableStateOf<Boolean?>(null)
    var recent by mutableStateOf(readRecent())
        private set

    fun refresh(message: String? = null) {
        currentSnapshot = document.snapshot()
        if (message != null) notice = message
    }

    fun install(value: SaveDocument, source: Path? = null, project: Path? = null,
                preparedSnapshot: DocumentSnapshot? = null) {
        currentDocument = value
        sourcePath = source
        projectPath = project
        selection = null
        tool = MapTool.SELECT
        ownerFilter = null
        query = ""
        page = WorkspacePage.MAP
        currentSnapshot = preparedSnapshot ?: value.snapshot()
        error = null
        recoveryPath = null
        notice = if (source != null) "Opened ${source.fileName}" else "New scenario · Place armies and buildings to prepare your world"
        if (source != null) rememberPath(source)
    }

    fun execute(command: EditCommand, message: String = "Edit applied") {
        if (busy || !hasDocument) return
        try {
            val result = document.execute(command)
            if (result != null) selection = result
            refresh(message)
        } catch (failure: Exception) {
            error = failure.message ?: failure.javaClass.simpleName
        }
    }

    fun undo() {
        if (!busy && hasDocument && document.undo()) refresh("Edit undone")
    }

    fun redo() {
        if (!busy && hasDocument && document.redo()) refresh("Edit restored")
    }

    fun select(id: RecordId, navigate: Boolean = false) {
        selection = id
        if (navigate) page = when (id.kind) {
            RecordKind.PLAYER -> WorkspacePage.PLAYERS
            RecordKind.ARMY, RecordKind.ARMY_UNIT -> WorkspacePage.ARMIES
            RecordKind.BUILDING, RecordKind.BUILDING_UNIT -> WorkspacePage.BUILDINGS
            RecordKind.SAVE, RecordKind.OPTIONS -> WorkspacePage.SCENARIO
            else -> WorkspacePage.MAP
        }
    }

    fun rememberPath(path: Path) {
        recent = (listOf(path.toAbsolutePath().toString()) + recent).distinct().take(8)
        runCatching { preferences().put("recent", recent.joinToString("\n")) }
    }

    companion object {
        private fun preferences() = Preferences.userNodeForPackage(EditorState::class.java)
        private fun readRecent(): List<String> = runCatching {
            preferences().get("recent", "").split('\n').filter { it.isNotBlank() }
        }.getOrDefault(emptyList())
    }
}

fun recordLabel(id: RecordId): String = when (id.kind) {
    RecordKind.SAVE -> "Scenario"
    RecordKind.OPTIONS -> "Game options"
    RecordKind.TILE -> "Tile ${id.slot / 100}, ${id.slot % 100}"
    RecordKind.PLAYER -> "Player ${id.slot}"
    RecordKind.ARMY -> "Army #${id.slot}"
    RecordKind.BUILDING -> "Building #${id.slot}"
    RecordKind.ARMY_UNIT -> "Army #${id.slot} · Unit ${id.unitSlot}"
    RecordKind.BUILDING_UNIT -> "Building #${id.slot} · Unit ${id.unitSlot}"
}

fun recordByteRange(id: RecordId?): IntRange = when (id?.kind) {
    RecordKind.OPTIONS -> 147163 until 147190
    RecordKind.TILE -> (16 + id.slot * 14).let { it until it + 14 }
    RecordKind.PLAYER -> (140040 + id.slot * 1423).let { it until it + 1423 }
    RecordKind.ARMY -> (147190 + id.slot * 725).let { it until it + 725 }
    RecordKind.BUILDING -> (509690 + id.slot * 467).let { it until it + 467 }
    RecordKind.ARMY_UNIT -> (147190 + id.slot * 725 + 6 + (id.unitSlot ?: 0) * 31).let { it until it + 31 }
    RecordKind.BUILDING_UNIT -> (509690 + id.slot * 467 + 18 + (id.unitSlot ?: 0) * 31).let { it until it + 31 }
    else -> 0 until 16
}

fun physicalSelectionExists(id: RecordId, snapshot: DocumentSnapshot): Boolean = when (id.kind) {
    RecordKind.SAVE, RecordKind.OPTIONS -> true
    RecordKind.TILE -> id.slot in 0 until 10000
    RecordKind.PLAYER -> snapshot.players.any { it.id == id }
    RecordKind.ARMY, RecordKind.BUILDING -> snapshot.entities.any { it.id == id }
    RecordKind.ARMY_UNIT, RecordKind.BUILDING_UNIT -> snapshot.entities.any { entity -> entity.units.any { it.id == id } }
}
