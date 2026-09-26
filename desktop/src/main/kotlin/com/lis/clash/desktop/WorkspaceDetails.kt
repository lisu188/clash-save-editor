package com.lis.clash.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lis.clash.editor.*
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
internal fun ScenarioPage(state: EditorState) {
    var encodingMenu by remember { mutableStateOf(false) }
    var advancedOpen by remember { mutableStateOf(false) }
    var optionsOpen by remember { mutableStateOf(false) }
    val properties = remember(state.document, state.snapshot.revision) { state.document.properties(RecordId(RecordKind.SAVE)) }
    val options = remember(state.document, state.snapshot.revision) { state.document.properties(RecordId(RecordKind.OPTIONS)) }
    val mission = properties.find { it.name == "activeMissionIndex" }?.value
    val hasFac = remember(state.document, state.snapshot.revision) { state.document.facBytes() != null }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        PageHeading("Scenario settings", "Set up your world and the way it plays.")
        DetailSection {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Map, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text(if (mission == "-1") "Free game" else "Campaign save", style = MaterialTheme.typography.titleMedium)
                    Text("Load this world through Clash's Load Game menu.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                QuietBadge(if (hasFac) "Rules connected" else "Rules missing", if (hasFac) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
            HorizontalDivider(Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                WorldMetric("Map size", "${state.snapshot.mapWidth} × ${state.snapshot.mapHeight}", Modifier.weight(1f))
                WorldMetric("Active players", state.snapshot.players.count { it.active }.toString(), Modifier.weight(1f))
                WorldMetric("Armies & buildings", state.snapshot.entities.size.toString(), Modifier.weight(1f))
            }
            if (!hasFac) Text("Open the companion FAC file with this save to edit changes that depend on game rules.", Modifier.padding(top = 14.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DetailSection {
            SectionLabel("BASIC DETAILS", Modifier.padding(bottom = 8.dp))
            listOf("name", "gameTurnCounter").forEach { name -> properties.find { it.name == name }?.let { PropertyEditor(state, RecordId(RecordKind.SAVE), it) } }
        }
        DetailSection {
            SectionLabel("TEXT & LANGUAGE", Modifier.padding(bottom = 12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Name encoding", style = MaterialTheme.typography.titleSmall)
                    Text("Choose the character set used to read names. Original bytes stay intact until you edit a name.", Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box {
                    OutlinedButton({ encodingMenu = true }, enabled = !state.busy) { Text("${state.document.encoding} ▾") }
                    DropdownMenu(encodingMenu, { encodingMenu = false }) {
                        listOf("windows-1250" to "Windows-1250 · Central European", "windows-1252" to "Windows-1252 · Western European", "IBM852" to "IBM852 · DOS Central European").forEach { (encoding, label) ->
                            DropdownMenuItem(text = { Text(label) }, enabled = !state.busy, onClick = {
                                try { state.document.setEncoding(encoding); state.refresh("Name encoding set to $encoding") }
                                catch (failure: Exception) { state.error = failure.message }
                                encodingMenu = false
                            })
                        }
                    }
                }
            }
        }
        DetailSection {
            DisclosureHeading("Game options", "Sound, movement and the in-game display", optionsOpen) { optionsOpen = !optionsOpen }
            if (optionsOpen) {
                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
                options.forEach { PropertyEditor(state, RecordId(RecordKind.OPTIONS), it) }
            }
        }
        DetailSection {
            DisclosureHeading("Advanced session fields", "Mission state, map theme, camera and port data", advancedOpen) { advancedOpen = !advancedOpen }
            if (advancedOpen) {
                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
                properties.filter { it.name !in listOf("name", "gameTurnCounter") }.forEach { PropertyEditor(state, RecordId(RecordKind.SAVE), it) }
            }
        }
    }
}

@Composable
private fun DetailSection(content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f))) {
        Column(Modifier.padding(18.dp), content = content)
    }
}

@Composable
private fun WorldMetric(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, Modifier.padding(top = 3.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DisclosureHeading(title: String, subtitle: String, expanded: Boolean, toggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = toggle), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, if (expanded) "Collapse $title" else "Expand $title", Modifier.padding(start = 12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun DiagnosticsPane(state: EditorState, modifier: Modifier) {
    val issues = state.exportIssues ?: state.snapshot.issues
    val checkedExport = state.exportIssues != null
    val errors = issues.count { it.severity == IssueSeverity.ERROR }
    Column(modifier.background(MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(46.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf("Diagnostics${if (issues.isNotEmpty()) " · ${issues.size}" else ""}", "Report", "Source bytes").forEachIndexed { index, label ->
                TextButton({ state.bottomTab = index }, modifier = Modifier.semantics {
                    contentDescription = "${listOf("Diagnostics", "Report", "Source bytes")[index]} tab"
                }, colors = ButtonDefaults.textButtonColors(contentColor = if (state.bottomTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)) {
                    Text(label, style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.weight(1f))
            StudioIconButton("Collapse diagnostics", Icons.Outlined.ExpandMore) { state.bottomOpen = false }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        when (state.bottomTab) {
            0 -> {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(if (checkedExport) "Playable save checks" else "Document structure", style = MaterialTheme.typography.labelMedium)
                        Text(when {
                            !checkedExport -> "Playable save requirements have not been checked."
                            errors > 0 -> "$errors issue${if (errors == 1) "" else "s"} must be resolved before export."
                            issues.isNotEmpty() -> "Export checks passed with ${issues.size} warning${if (issues.size == 1) "" else "s"}."
                            else -> "Export checks passed for this revision."
                        }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton({ state.runExportChecks() }, enabled = !state.busy, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                        Icon(Icons.AutoMirrored.Outlined.FactCheck, null, Modifier.size(16.dp))
                        Text("Check playable save", Modifier.padding(start = 7.dp), style = MaterialTheme.typography.labelMedium)
                    }
                }
                if (issues.isEmpty()) Row(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Icon(Icons.Outlined.CheckCircle, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(if (checkedExport) "No export issues found." else "No structural issues found.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
                    items(issues) { issue ->
                        Row(Modifier.fillMaxWidth().clickable(enabled = issue.recordId != null) { issue.recordId?.let { state.select(it, navigate = true) } }.padding(vertical = 9.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(if (issue.severity == IssueSeverity.ERROR) Icons.Outlined.ErrorOutline else Icons.Outlined.WarningAmber, issue.severity.name.lowercase(), Modifier.size(17.dp), tint = if (issue.severity == IssueSeverity.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary)
                            Text(issue.message, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            if (issue.recordId != null) Icon(Icons.Outlined.ChevronRight, "Inspect related record", Modifier.size(17.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            1 -> {
                val report = remember(state.document, state.snapshot.revision) {
                    buildString {
                        appendLine("${state.snapshot.name}  |  ${state.snapshot.mapWidth} columns × ${state.snapshot.mapHeight} rows")
                        appendLine("${state.snapshot.players.count { it.active }} active players  ·  ${state.snapshot.entities.count { it.id.kind == RecordKind.ARMY }} armies  ·  ${state.snapshot.entities.count { it.id.kind == RecordKind.BUILDING }} buildings  ·  ${state.snapshot.entities.sumOf { it.units.size }} units")
                        appendLine("${state.document.datBytes().size} DAT bytes  ·  FAC ${state.document.facBytes()?.let { "${it.size} bytes" } ?: "missing"}  ·  Revision ${state.snapshot.revision}")
                        state.snapshot.players.filter { it.active }.forEach { player -> appendLine("Player ${player.id.slot}: ${player.name} · ${if (player.human) "Human" else "Computer"} · ${state.snapshot.entities.count { it.owner == player.id.slot }} entities") }
                    }
                }
                Row(Modifier.fillMaxSize().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    SelectionContainer(Modifier.weight(1f).verticalScroll(rememberScrollState())) { Text(report, style = MaterialTheme.typography.bodySmall) }
                    StudioIconButton("Copy report", Icons.Outlined.ContentCopy) { copyDetails(state, report, "Report copied") }
                }
            }
            2 -> {
                val bytes = remember(state.document, state.snapshot.revision) { state.document.datBytes() }
                val range = recordByteRange(state.selection)
                val dump = remember(bytes, range) { range.toList().chunked(16).joinToString("\n") { positions -> "%06X  ".format(positions.first()) + positions.joinToString(" ") { "%02X".format(bytes[it].toInt() and 255) } } }
                Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
                    Row(Modifier.fillMaxWidth().height(38.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${state.selection?.let(::recordLabel) ?: "Save label"}  ·  ${range.count()} bytes", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        QuietBadge("Read only", MaterialTheme.colorScheme.onSurfaceVariant)
                        StudioIconButton("Copy source bytes", Icons.Outlined.ContentCopy) { copyDetails(state, dump, "Source bytes copied") }
                    }
                    SelectionContainer(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState())) {
                        Text(dump, Modifier.padding(bottom = 14.dp), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private fun copyDetails(state: EditorState, text: String, message: String) {
    try {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
        state.notice = message
    } catch (failure: Exception) {
        state.error = "Could not copy to the clipboard: ${failure.message ?: "clipboard unavailable"}"
    }
}
