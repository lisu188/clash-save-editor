package com.lis.clash.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lis.clash.UnitTypes
import com.lis.clash.editor.*

@Composable
internal fun Inspector(state: EditorState, modifier: Modifier, action: (String, RecordId) -> Unit) {
    val id = state.selection
    val exists = id?.let { physicalSelectionExists(it, state.snapshot) } ?: false
    val properties = remember(state.document, state.snapshot.revision, id) {
        if (id != null && exists) state.document.properties(id) else emptyList()
    }
    val entity = state.snapshot.entities.find { it.id == id }
    var advanced by remember(id) { mutableStateOf(false) }
    var more by remember(id) { mutableStateOf(false) }
    Column(modifier.background(MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Tune, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Inspector", Modifier.weight(1f).padding(start = 9.dp), style = MaterialTheme.typography.titleSmall)
            StudioIconButton("Close inspector", Icons.Outlined.Close) { state.inspectorOpen = false }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (id == null) {
            GettingStarted(state, action)
        } else if (!exists) {
            EmptyState("Selection removed", "This object is no longer in the scenario. Undo restores it to its original place.", Icons.AutoMirrored.Outlined.Undo,
                action = { OutlinedButton({ state.undo() }, enabled = state.document.canUndo && !state.busy) { Text("Undo removal") } })
        } else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
            if (id.kind in listOf(RecordKind.ARMY_UNIT, RecordKind.BUILDING_UNIT)) TextButton({
                state.select(RecordId(if (id.kind == RecordKind.ARMY_UNIT) RecordKind.ARMY else RecordKind.BUILDING, id.slot))
            }, contentPadding = PaddingValues(0.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, Modifier.size(15.dp)); Text("Back to formation", Modifier.padding(start = 6.dp))
            }
            SectionLabel(when (id.kind) {
                RecordKind.ARMY -> "FORMATION"
                RecordKind.BUILDING -> "SETTLEMENT"
                RecordKind.TILE -> "MAP TILE"
                RecordKind.PLAYER -> "PLAYER"
                else -> "DETAILS"
            })
            Text(entity?.name?.ifBlank { null } ?: recordLabel(id), Modifier.padding(top = 7.dp, bottom = 12.dp),
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (entity != null) {
                Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    PlayerDot(entity.owner)
                    Text(playerName(state, entity.owner), Modifier.weight(1f).padding(start = 7.dp), maxLines = 1,
                        overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                    QuietBadge("${entity.row}, ${entity.column}", MaterialTheme.colorScheme.secondary)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton({ action("move", id) }, enabled = !state.busy, modifier = Modifier.weight(1f), contentPadding = PaddingValues(8.dp)) {
                        Icon(Icons.Outlined.OpenWith, null, Modifier.size(15.dp)); Text("Move", Modifier.padding(start = 5.dp))
                    }
                    OutlinedButton({ state.showOnMap(id) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(8.dp)) {
                        Icon(Icons.Outlined.MyLocation, null, Modifier.size(15.dp)); Text("Locate", Modifier.padding(start = 5.dp))
                    }
                    Box {
                        StudioIconButton("More object actions", Icons.Outlined.MoreHoriz, enabled = !state.busy) { more = true }
                        DropdownMenu(more, { more = false }) {
                            DropdownMenuItem(text = { Text("Duplicate") }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) }, onClick = { more = false; action("clone", id) })
                            DropdownMenuItem(text = { Text("Change player") }, leadingIcon = { Icon(Icons.Outlined.PeopleOutline, null) }, onClick = { more = false; action("owner", id) })
                            HorizontalDivider()
                            DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { more = false; action("delete", id) })
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
            if (id.kind == RecordKind.PLAYER) {
                Button({ action("player", id) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Configure player") }
                Spacer(Modifier.height(16.dp))
            }
            if (entity != null) UnitRoster(state, entity, action)
            val essential = essentialProperties(id.kind)
            val common = properties.filter { it.name in essential }
            if (common.isNotEmpty()) {
                SectionLabel(if (id.kind == RecordKind.TILE) "TERRAIN & FEATURES" else "PROPERTIES", Modifier.padding(top = 14.dp, bottom = 8.dp))
                common.forEach { PropertyEditor(state, id, it) }
            }
            if (id.kind == RecordKind.TILE) {
                Surface(Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f), shape = RoundedCornerShape(8.dp)) {
                    Text("Use the map's paint tools to change terrain and features.", Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider(Modifier.padding(top = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
            TextButton({ advanced = !advanced }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 12.dp)) {
                Text("Advanced properties", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(if (advanced) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, Modifier.size(18.dp))
            }
            if (advanced) {
                QuietBadge("Physical slot ${id.slot}", MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                properties.filterNot { it.name in essential }.forEach { PropertyEditor(state, id, it) }
                TextButton({ state.bottomOpen = true; state.bottomTab = 2 }) {
                    Icon(Icons.Outlined.Code, null, Modifier.size(16.dp)); Text("Inspect source bytes", Modifier.padding(start = 7.dp))
                }
            }
        }
    }
}

@Composable
private fun GettingStarted(state: EditorState, action: (String, RecordId) -> Unit) {
    val active = state.snapshot.players.filter { it.active }
    val isDraft = state.snapshot.entities.isEmpty()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp)) {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(if (isDraft) Icons.Outlined.Explore else Icons.Outlined.TouchApp, null, Modifier.padding(14.dp).size(27.dp),
                tint = MaterialTheme.colorScheme.primary)
        }
        Text(if (isDraft) "Build your scenario" else "Explore your world", Modifier.padding(top = 20.dp, bottom = 10.dp),
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(if (isDraft) "A blank world, ready for your ideas. Start with players, then give them a place to begin."
            else "Select a tile, army or building to inspect and edit its details.", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        if (isDraft) {
            SetupStep("01", "Set up players", "At least two players, with one human.", active.size >= 2 && active.any { it.human }) { state.page = WorkspacePage.PLAYERS }
            SetupStep("02", "Place starting forces", "An army or settlement for each player.", false, active.isNotEmpty()) {
                action("army", RecordId(RecordKind.SAVE))
            }
            SetupStep("03", "Check playability", "Review what is needed before exporting.", false) { state.runExportChecks() }
        } else {
            SectionLabel("WORLD OVERVIEW", Modifier.padding(bottom = 14.dp))
            SummaryLine("Players", active.size.toString())
            SummaryLine("Armies", state.snapshot.entities.count { it.id.kind == RecordKind.ARMY }.toString())
            SummaryLine("Buildings", state.snapshot.entities.count { it.id.kind == RecordKind.BUILDING }.toString())
            SummaryLine("Units", state.snapshot.entities.sumOf { it.units.size }.toString())
        }
        HorizontalDivider(Modifier.padding(vertical = 24.dp), color = MaterialTheme.colorScheme.outlineVariant)
        SectionLabel("QUICK CONTROLS", Modifier.padding(bottom = 12.dp))
        SummaryLine("Pan the map", "Drag")
        SummaryLine("Zoom", "Scroll")
        SummaryLine("Undo", "Ctrl + Z")
        SummaryLine("Save project", "Ctrl + S")
        Spacer(Modifier.height(18.dp))
        Text("Projects can be saved at any stage.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SetupStep(number: String, title: String, detail: String, done: Boolean, enabled: Boolean = true, click: () -> Unit) {
    Surface(onClick = click, enabled = enabled, color = Color.Transparent, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        Row(Modifier.padding(vertical = 9.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (done) Icon(Icons.Outlined.CheckCircleOutline, null, Modifier.size(23.dp), tint = MaterialTheme.colorScheme.primary)
            else Text(number, style = MaterialTheme.typography.titleSmall, color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                Text(detail, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.ChevronRight, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun UnitRoster(state: EditorState, entity: EntitySnapshot, action: (String, RecordId) -> Unit) {
    var expanded by remember(entity.id) { mutableStateOf(true) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton({ expanded = !expanded }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) {
            Text(if (entity.id.kind == RecordKind.ARMY) "Units · ${entity.units.size}" else "Garrison · ${entity.units.size}", Modifier.weight(1f))
            Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, Modifier.size(16.dp))
        }
        StudioIconButton("Add unit", Icons.Outlined.Add, !state.busy) { action("unit", entity.id) }
    }
    if (expanded) {
        if (entity.units.isEmpty()) Text("No units assigned.", Modifier.padding(vertical = 12.dp), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        entity.units.forEach { unit ->
            Surface(onClick = { state.select(unit.id) }, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f),
                shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                Row(Modifier.padding(12.dp, 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(UnitTypes.metadata(unit.type)?.displayName ?: "Unit ${unit.type}", style = MaterialTheme.typography.labelLarge,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${unit.health}% health  ·  ${unit.actionPoints} AP  ·  ${unit.morale} morale", Modifier.padding(top = 4.dp),
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    StudioIconButton("Remove unit ${unit.id.unitSlot}", Icons.Outlined.RemoveCircleOutline, !state.busy) { action("removeUnit", unit.id) }
                }
            }
        }
    }
}

@Composable
internal fun PropertyEditor(state: EditorState, id: RecordId, property: PropertySnapshot) {
    var editing by remember(id, property.name) { mutableStateOf(false) }
    var draft by remember(id, property.name, property.value) { mutableStateOf(property.value) }
    var error by remember(id, property.name) { mutableStateOf<String?>(null) }
    val inputFocus = remember { FocusRequester() }
    LaunchedEffect(editing) {
        if (editing) { withFrameNanos { }; inputFocus.requestFocus() }
    }
    val label = propertyLabel(property.name)
    fun apply() {
        state.execute(EditCommand.SetProperty(id, property.name, draft), "$label updated")
        error = state.error
        state.error = null
        if (error == null) editing = false
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(propertyDisplayValue(property), Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodyMedium,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (property.editable) StudioIconButton("Edit $label", Icons.Outlined.Edit, !state.busy) {
            draft = property.value; error = null; editing = true
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .4f))
    if (editing) AlertDialog(
        onDismissRequest = { editing = false }, title = { Text("Edit $label") },
        text = {
            Column(Modifier.width(340.dp)) {
                OutlinedTextField(draft, { draft = it; error = null }, label = { Text(label) }, singleLine = true, enabled = !state.busy,
                    isError = error != null, modifier = Modifier.fillMaxWidth().focusRequester(inputFocus).onPreviewKeyEvent {
                        if (it.type == KeyEventType.KeyDown && it.key == Key.Enter && !state.busy) { apply(); true }
                        else if (it.type == KeyEventType.KeyDown && it.key == Key.Escape) { editing = false; true } else false
                    })
                if (error != null) Text(error!!, Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Text("Changes take effect when you apply them. You can undo this edit.", Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { Button({ apply() }, enabled = !state.busy && draft != property.value) { Text("Apply") } },
        dismissButton = { TextButton({ editing = false }) { Text("Cancel") } }
    )
}

private fun essentialProperties(kind: RecordKind): Set<String> = when (kind) {
    RecordKind.ARMY -> setOf("facingDirection", "isHiddenOnWorldMap")
    RecordKind.BUILDING -> setOf("displayName", "storedMoney", "peasantCount", "taxRate", "satisfaction", "wallStrength")
    RecordKind.ARMY_UNIT, RecordKind.BUILDING_UNIT -> setOf("currentHealthPercent", "currentActionPoints", "fatigue", "morale", "statusLevel", "orderState")
    RecordKind.PLAYER -> setOf("displayName", "controllerMode", "aiIntelligence", "religionFlag", "techLevel")
    RecordKind.TILE -> setOf("terrainTileId", "overlayTileId", "roadOrBridgeTileId")
    else -> setOf("name", "gameTurnCounter", "mapThemeIndex")
}

internal fun propertyLabel(name: String): String = when (name) {
    "name", "displayName" -> "Name"
    "currentHealthPercent" -> "Health (%)"
    "currentActionPoints" -> "Action points"
    "isHiddenOnWorldMap" -> "Concealed on map"
    "storedMoney" -> "Treasury"
    "peasantCount" -> "Population"
    "constructionTurnsRemaining" -> "Construction work remaining"
    "wallStrength" -> "Wall tier"
    "gameTurnCounter" -> "Current turn"
    "controllerMode" -> "Player control"
    "religionFlag" -> "Religion"
    "terrainTileId" -> "Terrain"
    "overlayTileId" -> "Overlay"
    "roadOrBridgeTileId" -> "Road or bridge"
    else -> name.replace(Regex("([a-z])([A-Z])"), "$1 $2").replaceFirstChar { it.uppercase() }
}

private fun propertyDisplayValue(property: PropertySnapshot): String {
    val value = property.value
    return when (property.name) {
        "controllerMode" -> when (value) { "1" -> "Human"; "0" -> "Computer"; else -> value }
        "religionFlag" -> when (value) { "1" -> "Christian"; "0" -> "Pagan"; else -> value }
        "aiIntelligence" -> when (value) { "0" -> "Easy"; "1" -> "Medium"; "2" -> "Hard"; else -> value }
        "isHiddenOnWorldMap" -> when (value) { "0" -> "No"; "1" -> "Yes"; else -> value }
        "terrainTileId" -> when (value) { "0" -> "Grassland · 0"; "4" -> "Grassland variant · 4"; else -> "Tile $value" }
        "overlayTileId", "roadOrBridgeTileId" -> if (value == "65535") "None" else "Tile $value"
        else -> value.ifBlank { "—" }
    }
}
