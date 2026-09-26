package com.lis.clash.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lis.clash.editor.*
import com.lis.clash.UnitTypes
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

private val DarkPalette = darkColorScheme(
    primary = Color(0xFF82D8C8), onPrimary = Color(0xFF073B33),
    secondary = Color(0xFFE5C57E), onSecondary = Color(0xFF3B2F13),
    secondaryContainer = Color(0xFF4D4023), onSecondaryContainer = Color(0xFFF3D995),
    background = Color(0xFF141B1F), surface = Color(0xFF192227),
    surfaceVariant = Color(0xFF263339), onSurfaceVariant = Color(0xFFADBFC4),
    primaryContainer = Color(0xFF23483F), onPrimaryContainer = Color(0xFFACEEDD),
    outline = Color(0xFF44575D), outlineVariant = Color(0xFF2E3D43)
)
private val LightPalette = lightColorScheme(
    primary = Color(0xFF24695B), onPrimary = Color.White,
    secondary = Color(0xFF806227), secondaryContainer = Color(0xFFF0E5C8), onSecondaryContainer = Color(0xFF5C491E), background = Color(0xFFF2F5F3), surface = Color(0xFFFBFCFA),
    surfaceVariant = Color(0xFFE5ECE7), onSurfaceVariant = Color(0xFF526460),
    primaryContainer = Color(0xFFC9EADD), onPrimaryContainer = Color(0xFF174C3E),
    outline = Color(0xFF879A92), outlineVariant = Color(0xFFD6DFD9)
)

private data class ActionRequest(val kind: String, val id: RecordId? = null)

@Composable
fun EditorWindow(
    state: EditorState,
    actions: EditorActions,
    confirmDiscard: Boolean,
    onDiscard: () -> Unit,
    onCancelDiscard: () -> Unit,
    onSaveAndContinue: () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val dark = state.darkMode ?: systemDark
    var sidebarWidth by remember { mutableStateOf(190f) }
    var inspectorWidth by remember { mutableStateOf(300f) }
    var bottomHeight by remember { mutableStateOf(180f) }
    var request by remember { mutableStateOf<ActionRequest?>(null) }
    val create: (String) -> Unit = { request = ActionRequest(it, state.selection) }
    MaterialTheme(colorScheme = if (dark) DarkPalette else LightPalette) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                AppToolbar(state, actions, dark) { state.darkMode = !dark }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp))
                Row(Modifier.weight(1f)) {
                    Sidebar(state, Modifier.width(sidebarWidth.dp).fillMaxHeight())
                    VerticalGrip { sidebarWidth = (sidebarWidth + it).coerceIn(155f, 250f) }
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            when (state.page) {
                                WorkspacePage.MAP -> WorldMap(state, create)
                                WorkspacePage.ARMIES -> EntityTable(state, RecordKind.ARMY, create)
                                WorkspacePage.BUILDINGS -> EntityTable(state, RecordKind.BUILDING, create)
                                WorkspacePage.PLAYERS -> PlayersPage(state) { request = ActionRequest("player", it) }
                                WorkspacePage.SCENARIO -> ScenarioPage(state)
                            }
                        }
                        if (state.bottomOpen) {
                            HorizontalGrip { bottomHeight = (bottomHeight - it).coerceIn(90f, 340f) }
                            DiagnosticsPane(state, Modifier.height(bottomHeight.dp).fillMaxWidth())
                        }
                    }
                    VerticalGrip { inspectorWidth = (inspectorWidth - it).coerceIn(260f, 410f) }
                    Inspector(state, Modifier.width(inspectorWidth.dp).fillMaxHeight()) { kind, id -> request = ActionRequest(kind, id) }
                }
                StatusBar(state)
            }
        }
        if (state.error != null) AlertDialog(
            onDismissRequest = { state.error = null },
            title = { Text("Unable to complete this action") },
            text = { Box(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) { Text(state.error.orEmpty()) } },
            confirmButton = { TextButton(onClick = { state.error = null }) { Text("Understood") } }
        )
        if (confirmDiscard) AlertDialog(
            onDismissRequest = onCancelDiscard,
            title = { Text("Save your changes?") },
            text = { Text("Your changes to “${state.snapshot.name}” have not been saved to an editor project.") },
            confirmButton = { Button(onClick = onSaveAndContinue, modifier = Modifier.semantics { contentDescription = "Save project and continue" }) { Text("Save project") } },
            dismissButton = {
                Row { TextButton(onClick = onCancelDiscard) { Text("Cancel") }; TextButton(onClick = onDiscard) { Text("Discard") } }
            }
        )
        if (state.recoveryPath != null) AlertDialog(
            onDismissRequest = { if (!state.busy) state.recoveryPath = null },
            title = { Text("Recover interrupted game save") },
            text = { Text("An earlier export to ${state.recoveryPath?.fileName} did not finish. Restore both previous DAT and FAC files from their recovery backups before opening this slot.") },
            confirmButton = { Button(actions.recover, enabled = !state.busy) { Text("Restore & open") } },
            dismissButton = { TextButton({ state.recoveryPath = null }, enabled = !state.busy) { Text("Cancel") } }
        )
        request?.let { action -> EditActionDialog(state, action, { request = null }) }
    }
}

@Composable
private fun AppToolbar(state: EditorState, actions: EditorActions, dark: Boolean, toggleTheme: () -> Unit) {
    var recentOpen by remember { mutableStateOf(false) }
    BoxWithConstraints {
    val compact = maxWidth < 1200.dp
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(10.dp)) {
            Text("C", Modifier.padding(12.dp, 7.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.width(if (compact) 100.dp else 130.dp)) {
            Text("CLASH STUDIO", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
            if (!compact) Text("SAVE & SCENARIO EDITOR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        VerticalDivider(Modifier.height(30.dp).padding(horizontal = 5.dp), color = MaterialTheme.colorScheme.outlineVariant)
        Column(Modifier.weight(1f)) {
            Text(state.snapshot.name.ifBlank { "Untitled scenario" }, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
            Text(if (state.snapshot.dirty) "●  Unsaved changes" else if (state.projectPath == null && state.sourcePath == null) "New draft" else "All changes saved", style = MaterialTheme.typography.labelSmall, color = if (state.snapshot.dirty) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(actions.newDocument, enabled = !state.busy) { Text("New") }
        TextButton(actions.open, enabled = !state.busy) { Text("Open") }
        Box {
            TextButton({ recentOpen = true }, enabled = !state.busy) { Text(if (compact) "⋯" else "Recent ▾") }
            DropdownMenu(recentOpen, { recentOpen = false }) {
                if (state.recent.isEmpty()) DropdownMenuItem(text = { Text("No recent files") }, onClick = {}, enabled = false)
                state.recent.forEach { path -> DropdownMenuItem(text = { Column { Text(java.nio.file.Path.of(path).fileName.toString()); Text(path, style = MaterialTheme.typography.labelSmall, maxLines = 1) } }, onClick = { recentOpen = false; actions.openRecent(path) }) }
            }
        }
        TextButton({ state.undo() }, enabled = !state.busy && state.document.canUndo, modifier = Modifier.semantics { contentDescription = "Undo" }) { Text("↶") }
        TextButton({ state.redo() }, enabled = !state.busy && state.document.canRedo, modifier = Modifier.semantics { contentDescription = "Redo" }) { Text("↷") }
        OutlinedButton(actions.saveProject, enabled = !state.busy) { Text(if (compact) "Save" else "Save project") }
        Button(actions.export, enabled = !state.busy) { Text(if (compact) "Export" else "Export game save") }
        TextButton(toggleTheme, modifier = Modifier.semantics { contentDescription = if (dark) "Use light theme" else "Use dark theme" }) { Text(if (dark) "☀" else "☾") }
    }
    }
}

@Composable
private fun Sidebar(state: EditorState, modifier: Modifier) {
    Column(modifier.background(MaterialTheme.colorScheme.surface).padding(14.dp)) {
        Text("WORKSPACE", Modifier.padding(8.dp, 14.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        WorkspacePage.entries.forEach { page ->
            val selected = state.page == page
            val count: String? = when (page) {
                WorkspacePage.ARMIES -> state.snapshot.entities.count { it.id.kind == RecordKind.ARMY }.toString()
                WorkspacePage.BUILDINGS -> state.snapshot.entities.count { it.id.kind == RecordKind.BUILDING }.toString()
                WorkspacePage.PLAYERS -> state.snapshot.players.count { it.active }.toString()
                else -> null
            }
            Surface(
                onClick = { state.page = page; if (page == WorkspacePage.SCENARIO) state.selection = RecordId(RecordKind.SAVE) },
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                shape = RoundedCornerShape(9.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp)
            ) {
                Row(Modifier.padding(11.dp, 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(page.symbol, Modifier.width(26.dp), color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(page.label, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                    if (count != null) Text(count, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text("PLAYER FILTER", Modifier.padding(8.dp, 17.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().clickable { state.ownerFilter = null }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (state.ownerFilter == null) "◉" else "○", color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(25.dp))
            Text("All players", style = MaterialTheme.typography.bodySmall)
        }
        state.snapshot.players.filter { it.active }.forEach { player ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(if (state.ownerFilter == player.id.slot) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent).clickable { state.ownerFilter = player.id.slot }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(playerColors[player.id.slot.mod(5)]))
                Spacer(Modifier.width(12.dp))
                Text(player.name.ifBlank { "Player ${player.id.slot}" }, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.weight(1f))
        Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .65f), shape = RoundedCornerShape(10.dp)) {
            Column(Modifier.padding(13.dp)) {
                Text("A world of your own", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))
                Text("Save a project while you work. Export a DAT + FAC pair when your scenario is ready to play.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("LOCAL FILES · OFFLINE", Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EntityTable(state: EditorState, kind: RecordKind, create: (String) -> Unit) {
    val entities = state.snapshot.entities.filter { entity -> entity.id.kind == kind && (state.ownerFilter == null || entity.owner == state.ownerFilter) &&
        (state.query.isBlank() || "${entity.name} ${entity.id.slot} ${entity.owner} ${entity.row} ${entity.column}".contains(state.query, true)) }
    Column(Modifier.fillMaxSize().padding(22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (kind == RecordKind.ARMY) "Armies" else "Buildings", style = MaterialTheme.typography.headlineSmall)
                Text("${entities.size} records · Stable game slot IDs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button({ create(if (kind == RecordKind.ARMY) "army" else "building") }, enabled = !state.busy && state.snapshot.players.any { it.active }) { Text(if (kind == RecordKind.ARMY) "+ Add army" else "+ Add building") }
        }
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(state.query, { state.query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Search name, slot, player or coordinates…") }, leadingIcon = { Text("⌕", style = MaterialTheme.typography.titleLarge) })
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth().padding(12.dp, 10.dp)) {
            TableText("SLOT", .6f); TableText("NAME", 2f); TableText("PLAYER", .8f); TableText("ROW / COL", 1f); TableText("UNITS", .6f)
        }
        HorizontalDivider()
        if (entities.isEmpty()) EmptyState(if (state.query.isBlank()) "Your world is waiting" else "No matching records", if (state.query.isBlank()) "Place ${if (kind == RecordKind.ARMY) "an army" else "a building"} on the map to get started." else "Try another search or player filter.")
        else LazyColumn(Modifier.fillMaxSize()) {
            items(entities, key = { it.id.slot }) { entity ->
                val selected = state.selection == entity.id
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(7.dp)).background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent).clickable { state.select(entity.id) }.padding(12.dp, 15.dp), verticalAlignment = Alignment.CenterVertically) {
                    TableText("#${entity.id.slot}", .6f)
                    Text(entity.name.ifBlank { recordLabel(entity.id) }, Modifier.weight(2f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                    Row(Modifier.weight(.8f), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(8.dp).background(playerColors.getOrElse(entity.owner) { Color.Gray }, RoundedCornerShape(3.dp))); Spacer(Modifier.width(6.dp)); Text(entity.owner.toString(), style = MaterialTheme.typography.bodySmall) }
                    TableText("${entity.row} / ${entity.column}", 1f)
                    TableText(entity.units.size.toString(), .6f)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f))
            }
        }
    }
}

@Composable
private fun RowScope.TableText(value: String, weight: Float) = Text(value, Modifier.weight(weight), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun PlayersPage(state: EditorState, edit: (RecordId) -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Players & starting setup", style = MaterialTheme.typography.headlineSmall)
        Text("Choose who takes part in this world.", Modifier.padding(top = 6.dp, bottom = 24.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(state.snapshot.players, key = { it.id.slot }) { player ->
                Surface(onClick = { state.select(player.id) }, shape = RoundedCornerShape(12.dp), color = if (state.selection == player.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = playerColors[player.id.slot.mod(5)].copy(alpha = .22f), shape = RoundedCornerShape(12.dp)) {
                            Text(player.id.slot.toString(), Modifier.padding(16.dp, 12.dp), color = playerColors[player.id.slot.mod(5)], style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f).padding(start = 18.dp)) {
                            Text(player.name.ifBlank { "Player ${player.id.slot}" }, style = MaterialTheme.typography.titleMedium)
                            Text(if (!player.active) "Inactive slot" else if (player.human) "Human player" else "Computer · Intelligence ${player.intelligence}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton({ edit(player.id) }, enabled = !state.busy) { Text("Configure") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScenarioPage(state: EditorState) {
    var encodingMenu by remember { mutableStateOf(false) }
    val properties = remember(state.document, state.snapshot.revision) { state.document.properties(RecordId(RecordKind.SAVE)) }
    val options = remember(state.document, state.snapshot.revision) { state.document.properties(RecordId(RecordKind.OPTIONS)) }
    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("Scenario settings", style = MaterialTheme.typography.headlineSmall)
        Text("Your world, its session state and game options.", Modifier.padding(top = 6.dp, bottom = 20.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("Play through the Load Game menu", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text("New scenarios are sandbox save slots. Configure players, place their starting forces, then export a DAT and its matching FAC file to a game save slot.", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Name encoding", style = MaterialTheme.typography.titleSmall)
                Text("Controls how names are displayed. Source bytes change only when you edit a name.", Modifier.padding(top = 4.dp, end = 15.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box {
                OutlinedButton({ encodingMenu = true }, enabled = !state.busy) { Text("${state.document.encoding} ▾") }
                DropdownMenu(encodingMenu, { encodingMenu = false }) {
                    listOf("windows-1250" to "Windows-1250 · Central European", "windows-1252" to "Windows-1252 · Western European", "IBM852" to "IBM852 · DOS Central European").forEach { (encoding, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = {
                            try { state.document.setEncoding(encoding); state.refresh("Name encoding set to $encoding") }
                            catch (failure: Exception) { state.error = failure.message }
                            encodingMenu = false
                        })
                    }
                }
            }
        }
        properties.forEach { property -> PropertyEditor(state, RecordId(RecordKind.SAVE), property) }
        HorizontalDivider(Modifier.padding(vertical = 18.dp))
        Text("Game options", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 14.dp))
        options.forEach { property -> PropertyEditor(state, RecordId(RecordKind.OPTIONS), property) }
    }
}

@Composable
private fun Inspector(state: EditorState, modifier: Modifier, action: (String, RecordId) -> Unit) {
    val id = state.selection
    val exists = id?.let { physicalSelectionExists(it, state.snapshot) } ?: false
    val properties = remember(state.document, state.snapshot.revision, id) { if (id != null && exists) state.document.properties(id) else emptyList() }
    val entity = state.snapshot.entities.find { it.id == id }
    Column(modifier.background(MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(18.dp, 19.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("INSPECTOR", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (id != null) TextButton({ state.selection = null }) { Text("×") }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (id == null) {
            Column(Modifier.fillMaxWidth().padding(22.dp)) {
                Text("Select something\nto make it yours.", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(14.dp))
                Text("Click a tile, army or building on the map. Its details and editing tools will appear here.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))
                Text("QUICK GUIDE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                listOf("Scroll to zoom the world", "Drag to move the canvas", "Choose terrain in Brushes", "Ctrl+Z to undo an edit", "Ctrl+S to save your project").forEach { Text("·  $it", Modifier.padding(top = 13.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        } else if (!exists) {
            Column(Modifier.padding(22.dp)) { Text(recordLabel(id), style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(12.dp)); Text("This record has been removed. Undo restores the same game slot.", style = MaterialTheme.typography.bodyMedium) }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
                Text(recordLabel(id), style = MaterialTheme.typography.titleLarge)
                Text("Physical slot ${id.slot}", Modifier.padding(top = 4.dp, bottom = 15.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (entity != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton({ action("move", id) }, enabled = !state.busy, contentPadding = PaddingValues(10.dp, 6.dp)) { Text("Move") }
                        OutlinedButton({ action("clone", id) }, enabled = !state.busy, contentPadding = PaddingValues(10.dp, 6.dp)) { Text("Clone") }
                        TextButton({ action("delete", id) }, enabled = !state.busy) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton({ action("owner", id) }, enabled = !state.busy) { Text("Change player") }
                        TextButton({ state.page = WorkspacePage.MAP }) { Text("Show on map") }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                if (id.kind == RecordKind.PLAYER) Button({ action("player", id) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Configure player") }
                properties.forEach { property -> PropertyEditor(state, id, property) }
                if (entity != null) {
                    HorizontalDivider(Modifier.padding(vertical = 16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("UNITS  ${entity.units.size}", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                        TextButton({ action("unit", id) }, enabled = !state.busy) { Text("+ Add") }
                    }
                    entity.units.forEach { unit ->
                        Surface(onClick = { state.select(unit.id) }, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f), shape = RoundedCornerShape(7.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("${UnitTypes.metadata(unit.type)?.displayName ?: "Unit ${unit.type}"} · Slot ${unit.id.unitSlot}", style = MaterialTheme.typography.labelLarge)
                                    Text("HP ${unit.health} · AP ${unit.actionPoints} · Morale ${unit.morale}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                TextButton({ action("removeUnit", unit.id) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.width(28.dp), enabled = !state.busy) { Text("−") }
                            }
                        }
                    }
                }
                if (id.kind == RecordKind.ARMY_UNIT || id.kind == RecordKind.BUILDING_UNIT) {
                    TextButton({ state.select(RecordId(if (id.kind == RecordKind.ARMY_UNIT) RecordKind.ARMY else RecordKind.BUILDING, id.slot)) }) { Text("← Back to formation") }
                }
                TextButton({ state.bottomOpen = true; state.bottomTab = 2 }) { Text("Inspect source bytes") }
            }
        }
    }
}

@Composable
private fun PropertyEditor(state: EditorState, id: RecordId, property: PropertySnapshot) {
    var draft by remember(id, property.name, property.value) { mutableStateOf(property.value) }
    val label = property.name.replace(Regex("([a-z])([A-Z])"), "$1 $2").replaceFirstChar { it.uppercase() }
    if (property.editable) {
        Row(Modifier.fillMaxWidth().padding(bottom = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(draft, { draft = it }, label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }, singleLine = true, modifier = Modifier.weight(1f), enabled = !state.busy)
            if (draft != property.value) TextButton({ state.execute(EditCommand.SetProperty(id, property.name, draft), "$label updated") }, enabled = !state.busy, contentPadding = PaddingValues(horizontal = 7.dp)) { Text("Apply") }
        }
    } else {
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(property.value, Modifier.padding(top = 3.dp), style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DiagnosticsPane(state: EditorState, modifier: Modifier) {
    val issues = state.snapshot.issues
    Column(modifier.background(MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf("Diagnostics${if (issues.isNotEmpty()) " · ${issues.size}" else ""}", "Report", "Source bytes").forEachIndexed { index, label ->
                TextButton({ state.bottomTab = index }, colors = ButtonDefaults.textButtonColors(contentColor = if (state.bottomTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)) { Text(label) }
            }
            Spacer(Modifier.weight(1f))
            TextButton({ state.bottomOpen = false }) { Text("⌄") }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        when (state.bottomTab) {
            0 -> if (issues.isEmpty()) Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("✓", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                Text("  No structural issues found. Export checks run before writing a game slot.", style = MaterialTheme.typography.bodySmall)
            } else LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                items(issues) { issue -> Row(Modifier.fillMaxWidth().clickable(enabled = issue.recordId != null) { issue.recordId?.let { state.select(it, navigate = true) } }.padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
                    Text(if (issue.severity.name == "ERROR") "!" else "△", Modifier.width(25.dp), color = if (issue.severity.name == "ERROR") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary)
                    Text(issue.message, style = MaterialTheme.typography.bodySmall)
                } }
            }
            1 -> {
                val report = buildString {
                    appendLine("${state.snapshot.name}  |  ${state.snapshot.mapWidth} columns × ${state.snapshot.mapHeight} rows")
                    appendLine("${state.snapshot.players.count { it.active }} active players  ·  ${state.snapshot.entities.count { it.id.kind == RecordKind.ARMY }} armies  ·  ${state.snapshot.entities.count { it.id.kind == RecordKind.BUILDING }} buildings  ·  ${state.snapshot.entities.sumOf { it.units.size }} units")
                    appendLine("${state.document.datBytes().size} DAT bytes  ·  FAC ${state.document.facBytes()?.size ?: 0} bytes  ·  Revision ${state.snapshot.revision}")
                    state.snapshot.players.filter { it.active }.forEach { player -> appendLine("Player ${player.id.slot}: ${player.name} · ${if (player.human) "Human" else "Computer"} · ${state.snapshot.entities.count { it.owner == player.id.slot }} entities") }
                }
                Row(Modifier.fillMaxSize().padding(16.dp)) {
                    Text(report, Modifier.weight(1f).verticalScroll(rememberScrollState()), style = MaterialTheme.typography.bodySmall)
                    TextButton({ Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(report), null); state.notice = "Report copied" }) { Text("Copy") }
                }
            }
            2 -> {
                val bytes = remember(state.document, state.snapshot.revision) { state.document.datBytes() }
                val range = recordByteRange(state.selection)
                val dump = remember(bytes, range) { range.toList().chunked(16).joinToString("\n") { positions -> "%06X  ".format(positions.first()) + positions.joinToString(" ") { "%02X".format(bytes[it].toInt() and 255) } } }
                Text(dump, Modifier.fillMaxSize().verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState()).padding(16.dp), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StatusBar(state: EditorState) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 17.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).background(if (state.busy) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp)))
        Spacer(Modifier.width(9.dp))
        Text(state.notice, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (state.selection != null) Text(recordLabel(state.selection!!), Modifier.padding(horizontal = 10.dp), style = MaterialTheme.typography.labelSmall)
        TextButton({ state.bottomOpen = !state.bottomOpen }, contentPadding = PaddingValues(4.dp)) { Text("${if (state.bottomOpen) "⌄" else "⌃"} Diagnostics", style = MaterialTheme.typography.labelSmall) }
    }
}

@Composable
private fun VerticalGrip(onDrag: (Float) -> Unit) {
    Box(Modifier.fillMaxHeight().width(5.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = .3f)).pointerInput(Unit) { detectHorizontalDragGestures { change, amount -> change.consume(); onDrag(amount) } })
}

@Composable
private fun HorizontalGrip(onDrag: (Float) -> Unit) {
    Box(Modifier.fillMaxWidth().height(5.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = .4f)).pointerInput(Unit) { detectVerticalDragGestures { change, amount -> change.consume(); onDrag(amount) } })
}

@Composable
private fun EmptyState(title: String, text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("◇", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp)); Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp)); Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EditActionDialog(state: EditorState, request: ActionRequest, dismiss: () -> Unit) {
    val id = request.id
    val entity = state.snapshot.entities.find { it.id == id }
    val tile = if (id?.kind == RecordKind.TILE) state.snapshot.tiles.find { it.slot == id.slot } else null
    val player = state.snapshot.players.find { it.id == id }
    val initialRow = entity?.row ?: tile?.row ?: 10
    val initialCol = entity?.column ?: tile?.column ?: 10
    val owner = entity?.owner ?: state.ownerFilter ?: state.snapshot.players.firstOrNull { it.active }?.id?.slot ?: 0
    val initial = remember(request) {
        when (request.kind) {
            "army" -> linkedMapOf("Row" to initialRow.toString(), "Column" to initialCol.toString(), "Player" to owner.toString(), "Unit type" to "0")
            "building" -> linkedMapOf("Name" to "Castle", "Row" to initialRow.toString(), "Column" to initialCol.toString(), "Player" to owner.toString(), "Building type" to "2")
            "move", "clone" -> linkedMapOf("Row" to (initialRow + if (request.kind == "clone") 2 else 0).toString(), "Column" to (initialCol + if (request.kind == "clone") 2 else 0).toString())
            "owner" -> linkedMapOf("Player" to owner.toString())
            "unit" -> linkedMapOf("Unit type" to "0")
            "player" -> linkedMapOf("Name" to (player?.name ?: "Player"), "Active" to (player?.active ?: false).toString(), "Human" to (player?.human ?: false).toString(), "Intelligence" to (player?.intelligence ?: 0).toString(), "Religion" to (player?.religion ?: 1).toString())
            else -> linkedMapOf()
        }
    }
    var values by remember(request) { mutableStateOf(initial.toMap()) }
    var formError by remember(request) { mutableStateOf<String?>(null) }
    var unitMenu by remember(request) { mutableStateOf(false) }
    val title = when (request.kind) {
        "army" -> "Create army"
        "building" -> "Create building"
        "move" -> "Move ${id?.let(::recordLabel)}"
        "clone" -> "Clone ${id?.let(::recordLabel)}"
        "delete" -> "Delete ${id?.let(::recordLabel)}?"
        "removeUnit" -> "Remove unit?"
        "unit" -> "Add a unit"
        "owner" -> "Change player"
        "player" -> "Configure player ${id?.slot}"
        else -> "Edit"
    }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.width(380.dp).heightIn(max = 470.dp).verticalScroll(rememberScrollState())) {
                if (request.kind == "delete") Text("The record and its contained units will be removed. This edit can be undone.", Modifier.padding(bottom = 12.dp))
                if (request.kind == "removeUnit") Text("The unit will be removed from its formation. This edit can be undone.", Modifier.padding(bottom = 12.dp))
                if (request.kind in listOf("army", "building", "clone", "move")) Text("Choose a free position within the map. Linked occupancy and supported facts are updated together.", Modifier.padding(bottom = 15.dp), style = MaterialTheme.typography.bodySmall)
                values.forEach { (label, value) ->
                    if (label == "Active" || label == "Human") Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(value.toBoolean(), { values = values + (label to it.toString()) }); Text(label)
                    } else if (label == "Unit type") {
                        Box(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                            OutlinedTextField(value, { values = values + (label to it) }, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                                supportingText = { Text(UnitTypes.metadata(value.toIntOrNull() ?: -1)?.displayName ?: "Choose a unit type") },
                                trailingIcon = { TextButton({ unitMenu = true }) { Text("Choose ▾") } })
                            DropdownMenu(unitMenu, { unitMenu = false }, modifier = Modifier.heightIn(max = 300.dp)) {
                                UnitTypes.all.forEach { unit -> DropdownMenuItem(text = { Text("${unit.id} · ${unit.displayName}") }, onClick = { values = values + (label to unit.id.toString()); unitMenu = false }) }
                            }
                        }
                    } else OutlinedTextField(value, { values = values + (label to it) }, label = { Text(label) }, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), singleLine = true,
                        supportingText = when (label) {
                            "Building type" -> ({ Text("0: small · 1: fortress · 2: castle") })
                            "Player" -> ({ Text("Player slot 0–4") })
                            "Intelligence" -> ({ Text("0: easy · 1: medium · 2: hard") })
                            "Religion" -> ({ Text("0 or 1") })
                            else -> null
                        })
                }
                if (formError != null) Text(formError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { Button(onClick = {
            formError = null
            try {
                fun number(key: String): Int = values[key]?.trim()?.toIntOrNull() ?: throw IllegalArgumentException("$key must be a whole number.")
                val command: EditCommand = when (request.kind) {
                    "army" -> EditCommand.CreateArmy(number("Row"), number("Column"), number("Player"), number("Unit type"))
                    "building" -> EditCommand.CreateBuilding(number("Row"), number("Column"), number("Player"), number("Building type"), values["Name"].orEmpty())
                    "move" -> EditCommand.Move(requireNotNull(id), number("Row"), number("Column"))
                    "clone" -> EditCommand.Clone(requireNotNull(id), number("Row"), number("Column"))
                    "delete" -> EditCommand.Delete(requireNotNull(id))
                    "owner" -> EditCommand.ChangeOwner(requireNotNull(id), number("Player"))
                    "unit" -> EditCommand.AddUnit(requireNotNull(id), number("Unit type"))
                    "removeUnit" -> EditCommand.RemoveUnit(RecordId(if (id?.kind == RecordKind.ARMY_UNIT) RecordKind.ARMY else RecordKind.BUILDING, requireNotNull(id).slot), requireNotNull(id.unitSlot))
                    "player" -> EditCommand.ConfigurePlayer(requireNotNull(id).slot, values["Active"].toBoolean(), values["Human"].toBoolean(), number("Intelligence"), number("Religion"), values["Name"].orEmpty())
                    else -> throw IllegalArgumentException("Unknown operation")
                }
                state.execute(command, title.removeSuffix("?"))
                val commandError = state.error
                if (commandError == null) dismiss() else {
                    // Keep validation beside the inputs instead of opening a dialog behind this one.
                    formError = commandError
                    state.error = null
                }
            } catch (failure: Exception) { formError = failure.message ?: "Please check the values." }
        }, enabled = !state.busy) { Text(if (request.kind in listOf("delete", "removeUnit")) "Remove" else "Apply") } },
        dismissButton = { TextButton(dismiss) { Text("Cancel") } }
    )
}
