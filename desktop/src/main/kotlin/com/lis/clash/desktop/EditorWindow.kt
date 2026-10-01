package com.lis.clash.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lis.clash.editor.*
import java.awt.Cursor
import java.nio.file.Path

@Composable
fun EditorWindow(
    state: EditorState,
    actions: EditorActions,
    confirmDiscard: Boolean,
    onDiscard: () -> Unit,
    onCancelDiscard: () -> Unit,
    onSaveAndContinue: () -> Unit
) {
    val dark = state.darkMode ?: isSystemInDarkTheme()
    var inspectorWidth by remember { mutableStateOf(316f) }
    var bottomHeight by remember { mutableStateOf(190f) }
    var request by remember { mutableStateOf<ActionRequest?>(null) }
    val create: (String) -> Unit = { request = ActionRequest(it, state.selection) }
    StudioTheme(dark) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (!state.hasDocument) StartupScreen(state, actions, dark) { state.darkMode = !dark }
            else BoxWithConstraints {
                val compact = maxWidth < 1180.dp
                LaunchedEffect(Unit) {
                    if (compact && state.selection == null) state.inspectorOpen = false
                }
                Column(Modifier.fillMaxSize()) {
                    AppToolbar(state, actions, dark, compact) { state.darkMode = !dark }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp))
                    Row(Modifier.weight(1f)) {
                        Sidebar(state, compact, Modifier.width(if (compact) 70.dp else 196.dp).fillMaxHeight())
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
                                HorizontalGrip { bottomHeight = (bottomHeight - it).coerceIn(120f, 320f) }
                                DiagnosticsPane(state, Modifier.height(bottomHeight.dp).fillMaxWidth())
                            }
                        }
                        if (state.inspectorOpen) {
                            VerticalGrip { inspectorWidth = (inspectorWidth - it).coerceIn(280f, 380f) }
                            Inspector(state, Modifier.width(if (compact) 292.dp else inspectorWidth.dp).fillMaxHeight()) { kind, id ->
                                request = ActionRequest(kind, id)
                            }
                        }
                    }
                    StatusBar(state)
                }
            }
        }
        if (state.error != null) AlertDialog(
            onDismissRequest = { state.error = null },
            icon = { Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Unable to complete this action") },
            text = { Box(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) { Text(state.error.orEmpty()) } },
            confirmButton = { TextButton({ state.error = null }) { Text("Understood") } }
        )
        if (confirmDiscard && state.hasDocument) AlertDialog(
            onDismissRequest = onCancelDiscard,
            title = { Text("Save your changes?") },
            text = { Text("Your changes to “${state.snapshot.name}” have not been saved to an editor project.") },
            confirmButton = { Button(onSaveAndContinue, modifier = Modifier.semantics { contentDescription = "Save project and continue" }) { Text("Save project") } },
            dismissButton = { Row { TextButton(onCancelDiscard) { Text("Cancel") }; TextButton(onDiscard) { Text("Discard") } } }
        )
        if (state.recoveryPath != null) AlertDialog(
            onDismissRequest = { if (!state.busy) state.recoveryPath = null },
            title = { Text("Recover interrupted game save") },
            text = { Text("An earlier export to ${state.recoveryPath?.fileName} did not finish. Restore the previous save and companion file from their recovery backups before opening this slot.") },
            confirmButton = { Button(actions.recover, enabled = !state.busy) { Text("Restore & open") } },
            dismissButton = { TextButton({ state.recoveryPath = null }, enabled = !state.busy) { Text("Cancel") } }
        )
        request?.let { action -> EditActionDialog(state, action) { request = null } }
    }
}

@Composable
private fun AppToolbar(state: EditorState, actions: EditorActions, dark: Boolean, compact: Boolean, toggleTheme: () -> Unit) {
    var fileMenu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).height(68.dp).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(9.dp)) {
            Icon(Icons.Outlined.Map, null, Modifier.padding(9.dp).size(24.dp), tint = MaterialTheme.colorScheme.onPrimary)
        }
        if (!compact) Column(Modifier.width(110.dp)) {
            Text("UNOFFICIAL CLASH SAVE EDITOR", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text("Scenario & save editor", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box {
            TextButton({ fileMenu = true }, enabled = !state.busy) {
                Text("File"); Icon(Icons.Outlined.ExpandMore, null, Modifier.padding(start = 4.dp).size(16.dp))
            }
            DropdownMenu(fileMenu, { fileMenu = false }) {
                DropdownMenuItem(text = { Text("New scenario") }, leadingIcon = { Icon(Icons.Outlined.Add, null) },
                    trailingIcon = { Text("Ctrl+N", style = MaterialTheme.typography.labelSmall) }, onClick = { fileMenu = false; actions.newDocument() })
                DropdownMenuItem(text = { Text("Open save or project") }, leadingIcon = { Icon(Icons.Outlined.FolderOpen, null) },
                    trailingIcon = { Text("Ctrl+O", style = MaterialTheme.typography.labelSmall) }, onClick = { fileMenu = false; actions.open() })
                HorizontalDivider()
                SectionLabel("RECENT FILES", Modifier.padding(16.dp, 10.dp))
                if (state.recent.isEmpty()) DropdownMenuItem(text = { Text("No recent files") }, enabled = false, onClick = {})
                state.recent.forEach { path ->
                    DropdownMenuItem(text = { Column(Modifier.widthIn(max = 360.dp)) {
                        Text(Path.of(path).fileName.toString(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(path, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } }, onClick = { fileMenu = false; actions.openRecent(path) })
                }
            }
        }
        VerticalDivider(Modifier.height(28.dp), color = MaterialTheme.colorScheme.outlineVariant)
        Column(Modifier.weight(1f).padding(start = 6.dp)) {
            Text(state.snapshot.name.ifBlank { "Untitled scenario" }, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(5.dp).background(if (state.snapshot.dirty) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp)))
                Text(if (state.snapshot.dirty) "Unsaved changes" else "All changes saved", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row {
            StudioIconButton("Undo", Icons.AutoMirrored.Outlined.Undo, !state.busy && state.document.canUndo) { state.undo() }
            StudioIconButton("Redo", Icons.AutoMirrored.Outlined.Redo, !state.busy && state.document.canRedo) { state.redo() }
        }
        OutlinedButton(actions.saveProject, enabled = !state.busy, contentPadding = PaddingValues(14.dp, 9.dp)) {
            Icon(Icons.Outlined.Save, null, Modifier.size(17.dp)); Spacer(Modifier.width(7.dp)); Text("Save project")
        }
        Button(actions.export, enabled = !state.busy, contentPadding = PaddingValues(14.dp, 9.dp)) {
            Icon(Icons.Outlined.FileUpload, null, Modifier.size(17.dp)); Spacer(Modifier.width(7.dp)); Text("Export save")
        }
        StudioIconButton(if (state.inspectorOpen) "Hide inspector" else "Show inspector", Icons.AutoMirrored.Outlined.ViewSidebar) { state.inspectorOpen = !state.inspectorOpen }
        StudioIconButton(if (dark) "Use light theme" else "Use dark theme", if (dark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode, onClick = toggleTheme)
    }
}

@Composable
private fun Sidebar(state: EditorState, compact: Boolean, modifier: Modifier) {
    Column(modifier.background(NavigationInk).padding(horizontal = if (compact) 8.dp else 12.dp, vertical = 20.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        if (!compact) Text("WORKSPACE", Modifier.padding(12.dp, 0.dp, 0.dp, 12.dp),
            style = MaterialTheme.typography.labelSmall, color = NavigationMuted)
        WorkspacePage.entries.forEach { page ->
            val active = state.page == page
            val count = when (page) {
                WorkspacePage.ARMIES -> state.snapshot.entities.count { it.id.kind == RecordKind.ARMY }
                WorkspacePage.BUILDINGS -> state.snapshot.entities.count { it.id.kind == RecordKind.BUILDING }
                WorkspacePage.PLAYERS -> state.snapshot.players.count { it.active }
                else -> null
            }
            Surface(onClick = { state.page = page; if (page == WorkspacePage.SCENARIO) state.selection = null },
                modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp).semantics { selected = active; contentDescription = page.label },
                color = if (active) Color(0xFF294C4D) else Color.Transparent, shape = RoundedCornerShape(8.dp)) {
                if (compact) Column(Modifier.padding(vertical = 11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(page.icon(), null, Modifier.size(21.dp), tint = if (active) Color(0xFFAFE4D0) else NavigationMuted)
                    Text(if (page == WorkspacePage.MAP) "Map" else page.label, Modifier.padding(top = 5.dp),
                        color = if (active) Color.White else NavigationMuted, style = MaterialTheme.typography.labelSmall)
                } else Row(Modifier.padding(12.dp, 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(page.icon(), null, Modifier.size(19.dp), tint = if (active) Color(0xFFAFE4D0) else NavigationMuted)
                    Text(page.label, Modifier.weight(1f).padding(start = 12.dp), style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal, color = if (active) Color.White else Color(0xFFCAD8D8))
                    if (count != null) Text(count.toString(), color = if (active) Color(0xFFAFE4D0) else NavigationMuted, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        if (!compact && state.page in listOf(WorkspacePage.MAP, WorkspacePage.ARMIES, WorkspacePage.BUILDINGS)) {
            HorizontalDivider(Modifier.padding(vertical = 20.dp), color = Color.White.copy(alpha = .10f))
            Text("PLAYER FILTER", Modifier.padding(12.dp, 0.dp, 0.dp, 10.dp), style = MaterialTheme.typography.labelSmall, color = NavigationMuted)
            SidebarFilter("All players", state.ownerFilter == null, null) { state.ownerFilter = null }
            state.snapshot.players.filter { it.active }.forEach { player ->
                SidebarFilter(player.name.ifBlank { "Player ${player.id.slot}" }, state.ownerFilter == player.id.slot, player.id.slot) {
                    state.ownerFilter = player.id.slot
                }
            }
            if (state.snapshot.players.none { it.active }) Text("No players configured", Modifier.padding(12.dp, 8.dp),
                color = NavigationMuted, style = MaterialTheme.typography.bodySmall)
        }
        }
        if (!compact) {
            Column(Modifier.padding(12.dp)) {
                Text("${state.snapshot.mapWidth} × ${state.snapshot.mapHeight}", color = Color(0xFFCAD8D8), style = MaterialTheme.typography.titleMedium)
                Text("WORLD SIZE · TILES", color = NavigationMuted, style = MaterialTheme.typography.labelSmall)
            }
            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = Color.White.copy(alpha = .10f))
            Row(Modifier.padding(12.dp, 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Computer, null, Modifier.size(15.dp), tint = NavigationMuted)
                Text("Local workspace", Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelSmall, color = NavigationMuted)
            }
        }
    }
}

@Composable
private fun SidebarFilter(label: String, active: Boolean, slot: Int?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(7.dp)).background(if (active) Color.White.copy(alpha = .07f) else Color.Transparent)
        .clickable(onClick = onClick).semantics { selected = active }.padding(12.dp, 10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (slot == null) Icon(Icons.Outlined.PeopleOutline, null, Modifier.size(13.dp), tint = NavigationMuted) else PlayerDot(slot)
        Text(label, Modifier.weight(1f).padding(start = 10.dp), maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall, color = if (active) Color.White else NavigationMuted)
        if (active) Icon(Icons.Outlined.Check, null, Modifier.size(14.dp), tint = Color(0xFFAFE4D0))
    }
}

@Composable
private fun StatusBar(state: EditorState) {
    val issues = state.exportIssues ?: state.snapshot.issues
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).height(34.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(5.dp).background(if (state.busy) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp)))
        Text(state.notice, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (state.selection != null) Text(recordLabel(state.selection!!), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton({ state.bottomOpen = !state.bottomOpen }, contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.height(32.dp)) {
            Icon(if (issues.isEmpty()) Icons.AutoMirrored.Outlined.FactCheck else Icons.Outlined.ErrorOutline, null, Modifier.size(15.dp))
            Text("Diagnostics${if (issues.isEmpty()) "" else " · ${issues.size}"}", Modifier.padding(horizontal = 7.dp), style = MaterialTheme.typography.labelSmall)
            Icon(if (state.bottomOpen) Icons.Outlined.ExpandMore else Icons.Outlined.ExpandLess, null, Modifier.size(15.dp))
        }
    }
}

@Composable
private fun VerticalGrip(onDrag: (Float) -> Unit) {
    val density = LocalDensity.current.density
    Box(Modifier.fillMaxHeight().width(5.dp)
        .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)))
        .pointerInput(density) { detectHorizontalDragGestures { change, amount -> change.consume(); onDrag(amount / density) } }) {
        Box(Modifier.align(Alignment.Center).fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
    }
}

@Composable
private fun HorizontalGrip(onDrag: (Float) -> Unit) {
    val density = LocalDensity.current.density
    Box(Modifier.fillMaxWidth().height(5.dp)
        .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR)))
        .pointerInput(density) { detectVerticalDragGestures { change, amount -> change.consume(); onDrag(amount / density) } }) {
        Box(Modifier.align(Alignment.Center).fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
    }
}
