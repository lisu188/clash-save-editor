package com.lis.clash.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lis.clash.editor.*

@Composable
internal fun EntityTable(state: EditorState, kind: RecordKind, create: (String) -> Unit) {
    val all = state.snapshot.entities.filter { it.id.kind == kind }
    val entities = all.filter { entity -> (state.ownerFilter == null || entity.owner == state.ownerFilter) &&
        (state.query.isBlank() || "${entity.name} ${entity.id.slot} ${playerName(state, entity.owner)} ${entity.owner} ${entity.row} ${entity.column}".contains(state.query, true)) }
    val armies = kind == RecordKind.ARMY
    val noun = if (armies) "armies" else "buildings"
    val hasPlayers = state.snapshot.players.any { it.active }
    var playerMenu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        PageHeading(if (armies) "Armies" else "Buildings", if (armies) "Manage formations and their starting positions." else "Place settlements and prepare their garrisons.") {
            Button({ create(if (armies) "army" else "building") }, enabled = !state.busy && hasPlayers) {
                Text(if (armies) "+ Add army" else "+ Add building")
            }
        }
        if (!hasPlayers) Surface(Modifier.fillMaxWidth().padding(top = 18.dp), color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(8.dp)) {
            Row(Modifier.padding(14.dp, 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.PeopleOutline, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Text("Set up a player before placing ${if (armies) "an army" else "a building"}.", Modifier.weight(1f).padding(horizontal = 10.dp),
                    style = MaterialTheme.typography.bodySmall)
                TextButton({ state.page = WorkspacePage.PLAYERS }) { Text("Set up players") }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(state.query, { state.query = it }, modifier = Modifier.weight(1f), singleLine = true,
                placeholder = { Text("Search $noun…") }, leadingIcon = { Icon(Icons.Outlined.Search, null, Modifier.size(19.dp)) },
                trailingIcon = { if (state.query.isNotEmpty()) StudioIconButton("Clear search", Icons.Outlined.Close) { state.query = "" } })
            Box {
                OutlinedButton({ playerMenu = true }) {
                    Icon(Icons.Outlined.FilterList, null, Modifier.size(17.dp)); Spacer(Modifier.width(6.dp))
                    Text(state.ownerFilter?.let { playerName(state, it) } ?: "All players", maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 95.dp))
                    Icon(Icons.Outlined.ExpandMore, null, Modifier.size(17.dp))
                }
                DropdownMenu(playerMenu, { playerMenu = false }) {
                    DropdownMenuItem(text = { Text("All players") }, onClick = { state.ownerFilter = null; playerMenu = false })
                    state.snapshot.players.filter { it.active }.forEach { player ->
                        DropdownMenuItem(text = { Text(playerName(state, player.id.slot)) }, leadingIcon = { PlayerDot(player.id.slot) },
                            onClick = { state.ownerFilter = player.id.slot; playerMenu = false })
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().height(30.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${entities.size} of ${all.size} $noun", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (state.ownerFilter != null || state.query.isNotEmpty()) TextButton({ state.ownerFilter = null; state.query = "" },
                contentPadding = PaddingValues(horizontal = 4.dp)) { Text("Clear filters", style = MaterialTheme.typography.labelSmall) }
        }
        Spacer(Modifier.height(10.dp))
        Surface(Modifier.weight(1f).fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Column {
                Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f)).padding(16.dp, 12.dp)) {
                    TableText("ID", .55f); TableText("${if (armies) "FORMATION" else "SETTLEMENT"}", 2.3f)
                    TableText("PLAYER", 1.3f); TableText("POSITION", 1f); TableText("UNITS", .55f)
                }
                if (entities.isEmpty()) EmptyState(
                    if (all.isEmpty()) "No $noun yet" else "No matching $noun",
                    if (all.isEmpty()) "${if (armies) "Create a formation" else "Add a settlement"} to give your players a place to begin."
                    else "Try another name or clear the current filters.",
                    if (armies) Icons.Outlined.Flag else Icons.Outlined.Castle,
                    action = { if (all.isEmpty() && hasPlayers) OutlinedButton({ create(if (armies) "army" else "building") }) { Text(if (armies) "Create first army" else "Create first building") } }
                ) else LazyColumn {
                    items(entities, key = { it.id }) { entity ->
                        val chosen = state.selection == entity.id || (state.selection?.slot == entity.id.slot &&
                            state.selection?.kind == if (armies) RecordKind.ARMY_UNIT else RecordKind.BUILDING_UNIT)
                        Row(Modifier.fillMaxWidth().background(if (chosen) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { state.select(entity.id) }.semantics { selected = chosen }.padding(16.dp, 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            TableText("#${entity.id.slot}", .55f)
                            Column(Modifier.weight(2.3f).padding(end = 8.dp)) {
                                Text(entity.name.ifBlank { recordLabel(entity.id) }, style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (!armies) Text(when (entity.type) { 0 -> "Small building"; 1 -> "Fortress"; 2 -> "Castle"; else -> "Building" },
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(Modifier.weight(1.3f).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                PlayerDot(entity.owner); Text(playerName(state, entity.owner), Modifier.padding(start = 6.dp),
                                    maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                            }
                            TableText("${entity.row}, ${entity.column}", 1f)
                            TableText(entity.units.size.toString(), .55f)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.TableText(value: String, weight: Float) {
    Text(value, Modifier.weight(weight), style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
internal fun PlayersPage(state: EditorState, edit: (RecordId) -> Unit) {
    val active = state.snapshot.players.filter { it.active }
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        PageHeading("Players & starting setup", "Choose who takes part and prepare their starting forces.")
        Row(Modifier.padding(vertical = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuietBadge("${active.size} / 5 active")
            QuietBadge("${active.count { it.human }} human", MaterialTheme.colorScheme.secondary)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (active.size < 2 || active.none { it.human }) item {
                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.Info, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("A playable scenario needs at least two active players, including one human. You can save your project at any time.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
            items(state.snapshot.players, key = { it.id }) { player ->
                val assets = state.snapshot.entities.count { it.owner == player.id.slot }
                Surface(onClick = { state.select(player.id) }, shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (state.selection == player.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Surface(color = if (player.active) playerColors[player.id.slot].copy(alpha = .13f) else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp)) {
                            Icon(if (player.active && player.human) Icons.Outlined.Person else if (player.active) Icons.Outlined.SmartToy else Icons.Outlined.PersonAdd,
                                null, Modifier.padding(14.dp).size(24.dp), tint = if (player.active) playerColors[player.id.slot] else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(player.name.ifBlank { "Player ${player.id.slot}" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(if (!player.active) "Inactive · Available player slot" else
                                "${if (player.human) "Human" else "Computer"} · ${if (player.religion == 1) "Christian" else "Pagan"}",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (player.active) Text(if (assets == 0) "No starting forces placed" else "$assets ${if (assets == 1) "formation or settlement" else "formations and settlements"}",
                                style = MaterialTheme.typography.labelSmall, color = if (assets == 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary)
                        }
                        OutlinedButton({ edit(player.id) }, enabled = !state.busy) { Text("Configure") }
                    }
                }
            }
        }
    }
}
