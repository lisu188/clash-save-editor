package com.lis.clash.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lis.clash.UnitTypes
import com.lis.clash.editor.EditCommand
import com.lis.clash.editor.RecordId
import com.lis.clash.editor.RecordKind

internal data class ActionRequest(val kind: String, val id: RecordId? = null)

@Composable
internal fun EditActionDialog(state: EditorState, request: ActionRequest, dismiss: () -> Unit) {
    val id = request.id
    val entity = state.snapshot.entities.find { it.id == id }
    val tile = if (id?.kind == RecordKind.TILE) state.snapshot.tiles.find { it.slot == id.slot } else null
    val player = state.snapshot.players.find { it.id == id }
    val initialRow = entity?.row ?: tile?.row ?: 10
    val initialCol = entity?.column ?: tile?.column ?: 10
    val activePlayers = state.snapshot.players.filter { it.active }
    val owner = entity?.owner ?: state.ownerFilter ?: activePlayers.firstOrNull()?.id?.slot ?: 0
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
    val scrollState = rememberScrollState()
    val destructive = request.kind in listOf("delete", "removeUnit")
    val hasCoordinates = values.containsKey("Row")
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
    val description = when (request.kind) {
        "army" -> "Choose a player and the first unit for your new army."
        "building" -> "Choose a home for your forces and give it a name."
        "move" -> "Choose a free position for this formation."
        "clone" -> "Create a copy with the same player and units at a new position."
        "delete" -> "This formation and its units will be removed. You can restore them with Undo."
        "removeUnit" -> "Remove this unit from its formation. You can restore it with Undo."
        "unit" -> "Choose the unit to add to this formation."
        "owner" -> "Transfer this formation and its units to another active player."
        "player" -> "Choose who takes part and how this player is controlled."
        else -> ""
    }
    fun update(label: String, value: String) {
        values = values + (label to value)
        formError = null
    }
    fun apply() {
        formError = null
        try {
            fun number(key: String): Int = values[key]?.trim()?.toIntOrNull()
                ?: throw IllegalArgumentException("$key must be a whole number.")
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
                // Preserve inputs and keep validation in this dialog, beside the controls.
                formError = commandError
                state.error = null
            }
        } catch (failure: Exception) {
            formError = failure.message ?: "Please check the values."
        }
    }
    LaunchedEffect(formError) {
        if (formError != null) {
            withFrameNanos { }
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }
    AlertDialog(
        onDismissRequest = { if (!state.busy) dismiss() },
        modifier = Modifier.widthIn(max = 540.dp),
        icon = {
            Icon(
                if (destructive) Icons.Outlined.DeleteOutline else when (request.kind) {
                    "player", "owner" -> Icons.Outlined.PersonOutline
                    "move" -> Icons.Outlined.OpenWith
                    "clone" -> Icons.Outlined.ContentCopy
                    else -> Icons.Outlined.AddCircleOutline
                },
                contentDescription = null,
                tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 430.dp).verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (values.containsKey("Name")) OutlinedTextField(
                    values.getValue("Name"), { update("Name", it) },
                    label = { Text("Name") }, singleLine = true, enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth()
                )
                if (values.containsKey("Player")) DialogChoice(
                    label = "Player", value = values.getValue("Player"),
                    choices = activePlayers.map { it.id.slot.toString() to "${it.name.ifBlank { "Player ${it.id.slot}" }} · Player ${it.id.slot}" },
                    enabled = !state.busy, onSelect = { update("Player", it) },
                    emptyText = "Set up an active player first"
                )
                if (values.containsKey("Building type")) DialogChoice(
                    label = "Building type", value = values.getValue("Building type"),
                    choices = listOf("0" to "Small building · 1 tile", "1" to "Fortress · 2 × 2 tiles", "2" to "Castle · 2 × 2 tiles"),
                    enabled = !state.busy, onSelect = { update("Building type", it) }
                )
                if (values.containsKey("Unit type")) DialogChoice(
                    label = "Unit type", value = values.getValue("Unit type"),
                    choices = UnitTypes.all.map { it.id.toString() to it.displayName },
                    enabled = !state.busy, onSelect = { update("Unit type", it) }
                )
                if (hasCoordinates) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text("Map position", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("Row" to state.snapshot.mapHeight, "Column" to state.snapshot.mapWidth).forEach { (label, limit) ->
                            OutlinedTextField(
                                values.getValue(label), { update(label, it) }, label = { Text(label) },
                                singleLine = true, enabled = !state.busy, modifier = Modifier.weight(1f),
                                supportingText = { Text("0–${limit - 1}") }
                            )
                        }
                    }
                    Text("The position must be clear of other armies and buildings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (request.kind == "player") {
                    DialogToggle("Active", "Include this player in the scenario", values.getValue("Active").toBoolean(), !state.busy) { update("Active", it.toString()) }
                    DialogToggle("Human", "On: controlled by a person. Off: controlled by the computer.", values.getValue("Human").toBoolean(), !state.busy) { update("Human", it.toString()) }
                    DialogChoice(
                        label = "Intelligence", value = values.getValue("Intelligence"),
                        choices = listOf("0" to "Easy", "1" to "Medium", "2" to "Hard"),
                        enabled = !state.busy && !values.getValue("Human").toBoolean(),
                        onSelect = { update("Intelligence", it) }, supportingText = "Used when the computer controls this player."
                    )
                    DialogChoice(
                        label = "Religion", value = values.getValue("Religion"),
                        choices = listOf("0" to "Pagan", "1" to "Christian"),
                        enabled = !state.busy, onSelect = { update("Religion", it) }
                    )
                }
                formError?.let { message ->
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(10.dp)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Outlined.ErrorOutline, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = ::apply, enabled = !state.busy,
                colors = if (destructive) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
                else ButtonDefaults.buttonColors()
            ) { Text(if (destructive) "Remove" else "Apply") }
        },
        dismissButton = { TextButton(onClick = dismiss, enabled = !state.busy) { Text("Cancel") } }
    )
}

@Composable
private fun DialogChoice(
    label: String,
    value: String,
    choices: List<Pair<String, String>>,
    enabled: Boolean,
    onSelect: (String) -> Unit,
    emptyText: String = "Choose an option",
    supportingText: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = choices.firstOrNull { it.first == value }?.second
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true }, enabled = enabled && choices.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$label: ${selected ?: emptyText}" },
                shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 13.dp)
            ) {
                Text(selected ?: emptyText, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
                Icon(Icons.Outlined.ExpandMore, contentDescription = null, Modifier.size(20.dp))
            }
            DropdownMenu(expanded, { expanded = false }, modifier = Modifier.heightIn(max = 300.dp)) {
                choices.forEach { (key, text) ->
                    DropdownMenuItem(
                        text = { Text(text) },
                        onClick = { expanded = false; onSelect(key) },
                        trailingIcon = { if (key == value) Icon(Icons.Outlined.Check, contentDescription = "Selected", Modifier.size(18.dp)) }
                    )
                }
            }
        }
        if (supportingText != null) Text(supportingText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DialogToggle(label: String, description: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(10.dp)) {
        Row(
            Modifier.fillMaxWidth().toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelLarge)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked, onCheckedChange = null, enabled = enabled)
        }
    }
}
