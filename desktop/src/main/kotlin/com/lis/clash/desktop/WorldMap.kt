package com.lis.clash.desktop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.lis.clash.editor.*
import kotlin.math.max
import kotlin.math.pow

val playerColors = listOf(Color(0xFFEF876B), Color(0xFF70B5E8), Color(0xFFE5BF61), Color(0xFFB99ADB), Color(0xFF83C599))

fun terrainColor(id: Int): Color = when {
    id == 65535 -> Color(0xFF242C30)
    id == 0 || id == 4 -> Color(0xFF668B6B)
    id in 603..610 -> Color(0xFF688E9C)
    id in 707..714 -> Color(0xFFB3986A)
    else -> {
        val shades = listOf(0xFF789475, 0xFF7A9B89, 0xFF6B8995, 0xFF9D9974, 0xFF859C7F, 0xFF8B806D, 0xFF91AA86, 0xFF657D74)
        Color(shades[(id / 12).mod(shades.size)])
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WorldMap(state: EditorState, onCreate: (String) -> Unit) {
    val snapshot = state.snapshot
    var viewport by remember { mutableStateOf(MapViewport()) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var hovered by remember { mutableStateOf<Int?>(null) }
    var toolMenu by remember { mutableStateOf(false) }
    var presetMenu by remember { mutableStateOf(false) }
    var layersMenu by remember { mutableStateOf(false) }
    var needsFit by remember(state.document) { mutableStateOf(true) }
    val currentViewport by rememberUpdatedState(viewport)
    val rows = snapshot.mapHeight.coerceIn(1, 100)
    val columns = snapshot.mapWidth.coerceIn(1, 100)
    val selected = state.selection
    val parentSelection = when (selected?.kind) {
        RecordKind.ARMY_UNIT -> RecordId(RecordKind.ARMY, selected.slot)
        RecordKind.BUILDING_UNIT -> RecordId(RecordKind.BUILDING, selected.slot)
        else -> selected
    }
    val selectedEntity = snapshot.entities.find { it.id == parentSelection }
    val selectedSlot = if (selected?.kind == RecordKind.TILE) selected.slot else selectedEntity?.let { it.row * 100 + it.column }
    var strokeSlots by remember { mutableStateOf(setOf<Int>()) }

    fun paint(slots: List<Int>) {
        if (slots.isEmpty()) return
        val value = state.paintValue.toIntOrNull()
        if (value == null) { state.error = "Enter a whole number for the brush value."; return }
        when (state.tool) {
            MapTool.TERRAIN -> state.execute(EditCommand.PaintTiles(slots, terrain = value), "Painted ${slots.size} terrain tile(s)")
            MapTool.OVERLAY -> state.execute(EditCommand.PaintTiles(slots, overlay = value), "Painted ${slots.size} overlay tile(s)")
            MapTool.ROAD -> state.execute(EditCommand.PaintTiles(slots, road = value), "Painted ${slots.size} road tile(s)")
            MapTool.TRAP -> state.execute(EditCommand.SetTraps(slots, value), "Updated ${slots.size} trap tile(s)")
            else -> Unit
        }
    }
    val paintAction by rememberUpdatedState<(List<Int>) -> Unit>({ paint(it) })
    val selectionAction by rememberUpdatedState<(Int) -> Unit>({ slot ->
        if (state.tool != MapTool.SELECT) {
            state.select(RecordId(RecordKind.TILE, slot))
            paintAction(listOf(slot))
        } else {
            val entity = if (state.showEntities) snapshot.entities.lastOrNull { entity ->
                val footprint = if (entity.id.kind == RecordKind.BUILDING && entity.type in 1..2) 2 else 1
                (if (entity.id.kind == RecordKind.ARMY) state.showArmies else state.showBuildings) && (state.ownerFilter == null || entity.owner == state.ownerFilter) && slot / 100 in entity.row until entity.row + footprint && slot % 100 in entity.column until entity.column + footprint
            } else null
            state.select(entity?.id ?: RecordId(RecordKind.TILE, slot))
        }
    })

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(20.dp, 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("World canvas", style = MaterialTheme.typography.titleLarge)
                Text("${columns} × ${rows} tiles · Procedural preview", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = { onCreate("army") }, enabled = !state.busy && snapshot.players.any { it.active }) { Text("+ Army") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { onCreate("building") }, enabled = !state.busy && snapshot.players.any { it.active }) { Text("+ Building") }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box {
                FilledTonalButton(onClick = { toolMenu = true }) { Text("${state.tool.label}  ▾") }
                DropdownMenu(expanded = toolMenu, onDismissRequest = { toolMenu = false }) {
                    MapTool.entries.forEach { tool -> DropdownMenuItem(text = { Text(tool.label) }, onClick = { state.tool = tool; toolMenu = false }) }
                }
            }
            Box {
                TextButton(onClick = { presetMenu = true }) { Text("Brushes ▾") }
                DropdownMenu(presetMenu, { presetMenu = false }) {
                    listOf(
                        Triple("Ground · 0", MapTool.TERRAIN, "0"),
                        Triple("Ground variant · 4", MapTool.TERRAIN, "4"),
                        Triple("Castle foundation · 707", MapTool.TERRAIN, "707"),
                        Triple("Treasure · 752", MapTool.TERRAIN, "752"),
                        Triple("Religious site · 728", MapTool.OVERLAY, "728"),
                        Triple("Road / bridge · 872", MapTool.ROAD, "872"),
                        Triple("Clear overlay", MapTool.OVERLAY, "65535"),
                        Triple("Clear road", MapTool.ROAD, "65535"),
                        Triple("Trap · player 0", MapTool.TRAP, "1"),
                        Triple("Clear trap", MapTool.TRAP, "0")
                    ).forEach { (label, tool, value) -> DropdownMenuItem(text = { Text(label) }, onClick = { state.tool = tool; state.paintValue = value; presetMenu = false }) }
                }
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { viewport = viewport.zoom(0.8f, canvasSize.width / 2f, canvasSize.height / 2f) }) { Text("−") }
            Text("${(viewport.cellSize / 8f * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
            TextButton(onClick = { viewport = viewport.zoom(1.25f, canvasSize.width / 2f, canvasSize.height / 2f) }) { Text("+") }
            OutlinedButton(onClick = { viewport = MapViewport.fit(canvasSize.width.toFloat(), canvasSize.height.toFloat(), rows, columns) }) { Text("Fit") }
        }
        if (state.tool != MapTool.SELECT) Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(state.paintValue, { state.paintValue = it }, label = { Text(if (state.tool == MapTool.TRAP) "Owner mask" else "Tile ID") }, singleLine = true, modifier = Modifier.width(130.dp).height(58.dp))
            if (state.tool != MapTool.TRAP) TextButton(onClick = { state.paintValue = if (state.tool == MapTool.TERRAIN) "0" else "65535" }) { Text(if (state.tool == MapTool.TERRAIN) "Reset ground" else "Erase") }
            Text("Choose a supported brush, then paint", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { state.tool = MapTool.SELECT }) { Text("Done") }
        }
        Row(Modifier.padding(horizontal = 20.dp).padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                TextButton(onClick = { layersMenu = true }) { Text("Layers ▾") }
                DropdownMenu(layersMenu, { layersMenu = false }) {
                    LayerMenuItem("Terrain", state.showTerrain) { state.showTerrain = it }
                    LayerMenuItem("Overlays", state.showOverlays) { state.showOverlays = it }
                    LayerMenuItem("Roads & bridges", state.showRoads) { state.showRoads = it }
                    LayerMenuItem("Armies", state.showArmies) { state.showArmies = it }
                    LayerMenuItem("Buildings", state.showBuildings) { state.showBuildings = it }
                    LayerMenuItem("Occupancy", state.showOccupancy) { state.showOccupancy = it }
                    LayerMenuItem("Traps", state.showTraps) { state.showTraps = it }
                }
            }
            Text("${listOf(state.showTerrain, state.showOverlays, state.showRoads, state.showArmies, state.showBuildings, state.showOccupancy, state.showTraps).count { it }} / 7 visible", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LayerSwitch("Grid", state.showGrid) { state.showGrid = it }
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 14.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF202A2E))) {
            Canvas(Modifier.fillMaxSize()
                .semantics { contentDescription = "World map, $columns columns and $rows rows. Click to select a tile or entity; drag to pan; scroll to zoom." }
                .onSizeChanged { size ->
                    canvasSize = size
                    if (needsFit && size.width > 0) { viewport = MapViewport.fit(size.width.toFloat(), size.height.toFloat(), rows, columns); needsFit = false }
                }
                .onPointerEvent(PointerEventType.Move) { event -> event.changes.firstOrNull()?.position?.let { hovered = viewport.tileAt(it.x, it.y, rows, columns) } }
                .onPointerEvent(PointerEventType.Exit) { hovered = null }
                .onPointerEvent(PointerEventType.Scroll) { event ->
                    event.changes.firstOrNull()?.let { change -> viewport = viewport.zoom(1.15f.pow(-change.scrollDelta.y), change.position.x, change.position.y) }
                }
                .pointerInput(state.tool, rows, columns) {
                    detectTapGestures { position -> currentViewport.tileAt(position.x, position.y, rows, columns)?.let { selectionAction(it) } }
                }
                .pointerInput(state.tool, rows, columns) {
                    detectDragGestures(
                        onDragStart = { position -> strokeSlots = currentViewport.tileAt(position.x, position.y, rows, columns)?.let { setOf(it) } ?: emptySet() },
                        onDragCancel = { strokeSlots = emptySet() },
                        onDragEnd = { if (state.tool != MapTool.SELECT) paintAction(strokeSlots.toList()); strokeSlots = emptySet() },
                        onDrag = { change, delta ->
                            change.consume()
                            if (state.tool == MapTool.SELECT) viewport = currentViewport.pan(delta.x, delta.y)
                            else currentViewport.tileAt(change.position.x, change.position.y, rows, columns)?.let { strokeSlots = strokeSlots + it }
                        }
                    )
                }
            ) {
                val cell = viewport.cellSize
                val ox = viewport.x
                val oy = viewport.y
                snapshot.tiles.forEach { tile ->
                    if (tile.row >= rows || tile.column >= columns) return@forEach
                    val left = ox + tile.column * cell
                    val top = oy + tile.row * cell
                    if (left + cell < 0 || top + cell < 0 || left > size.width || top > size.height) return@forEach
                    drawRect(if (state.showTerrain) terrainColor(tile.terrain) else Color(0xFF2B393B), Offset(left, top), Size(cell + .4f, cell + .4f))
                    if (state.showOverlays && tile.overlay != 65535) {
                        val overlayColor = Color(0xFF354C3B).copy(alpha = .55f)
                        drawCircle(overlayColor, cell * .25f, Offset(left + cell * .55f, top + cell * .5f))
                    }
                    if (state.showRoads && tile.road != 65535) {
                        drawLine(Color(0xFFD8C698), Offset(left, top + cell / 2), Offset(left + cell, top + cell / 2), max(1f, cell * .14f))
                    }
                    if (state.showTraps && tile.trapMask != 0) {
                        drawLine(Color(0xFFFFA97D), Offset(left + cell * .3f, top + cell * .3f), Offset(left + cell * .7f, top + cell * .7f), max(1f, cell * .1f))
                        drawLine(Color(0xFFFFA97D), Offset(left + cell * .7f, top + cell * .3f), Offset(left + cell * .3f, top + cell * .7f), max(1f, cell * .1f))
                    }
                    if (state.showOccupancy && tile.occupancy != 65535) {
                        val occupancyColor = if (tile.occupancy in 0..499) Color(0xFF77D1E9) else if (tile.occupancy in 0x8000..0x8063) Color(0xFFEDD27D) else Color(0xFFFF6C7D)
                        drawRect(occupancyColor.copy(alpha = .42f), Offset(left, top), Size(cell, cell))
                        drawRect(occupancyColor, Offset(left + .5f, top + .5f), Size(cell - 1, cell - 1), style = Stroke(1f))
                    }
                    if (state.showGrid && cell >= 7f) drawRect(Color.Black.copy(alpha = .12f), Offset(left, top), Size(cell, cell), style = Stroke(1f))
                    if (tile.slot in strokeSlots && state.tool != MapTool.SELECT) drawRect(Color.White.copy(alpha = .35f), Offset(left, top), Size(cell, cell))
                }
                if (state.showEntities) snapshot.entities.filter { state.ownerFilter == null || it.owner == state.ownerFilter }.forEach { entity ->
                    if (entity.id.kind == RecordKind.ARMY && !state.showArmies || entity.id.kind == RecordKind.BUILDING && !state.showBuildings) return@forEach
                    val left = ox + entity.column * cell
                    val top = oy + entity.row * cell
                    val color = playerColors.getOrElse(entity.owner) { Color.LightGray }
                    if (entity.id.kind == RecordKind.BUILDING) {
                        val extent = cell * if (entity.type in 1..2) 2 else 1
                        drawRect(Color(0xFF182A2D), Offset(left, top), Size(extent, extent))
                        drawRect(color, Offset(left + 1.5f, top + 1.5f), Size(max(1f, extent - 3f), max(1f, extent - 3f)), style = Stroke(max(2f, cell * .18f)))
                        if (cell > 6f) drawRect(color, Offset(left + extent * .35f, top + extent * .35f), Size(extent * .3f, extent * .3f))
                    } else {
                        val center = Offset(left + cell / 2, top + cell / 2)
                        val radius = max(cell * .48f, 3.5f)
                        val path = Path().apply { moveTo(center.x, center.y - radius); lineTo(center.x + radius, center.y); lineTo(center.x, center.y + radius); lineTo(center.x - radius, center.y); close() }
                        drawPath(path, color)
                        drawPath(path, Color(0xFF182A2D), style = Stroke(1.5f))
                    }
                }
                selectedSlot?.let { slot ->
                    val extent = cell * if (selectedEntity?.id?.kind == RecordKind.BUILDING && selectedEntity.type in 1..2) 2 else 1
                    drawRect(Color(0xFFFFDB87), Offset(ox + slot % 100 * cell - 2, oy + slot / 100 * cell - 2), Size(extent + 4, extent + 4), style = Stroke(2.5f))
                }
                hovered?.let { slot -> drawRect(Color.White.copy(alpha = .65f), Offset(ox + slot % 100 * cell, oy + slot / 100 * cell), Size(cell, cell), style = Stroke(1f)) }
            }
            Surface(Modifier.align(Alignment.BottomStart).padding(12.dp), color = Color(0xDD172226), shape = RoundedCornerShape(6.dp)) {
                Text(hovered?.let { "ROW ${it / 100}  ·  COL ${it % 100}  ·  TILE #$it" } ?: "Drag to pan  ·  Scroll to zoom  ·  Click to inspect", Modifier.padding(10.dp, 7.dp), color = Color(0xFFDFE9E5), style = MaterialTheme.typography.labelSmall)
            }
            if (snapshot.players.none { it.active }) Surface(Modifier.align(Alignment.TopCenter).padding(18.dp).widthIn(max = 370.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .97f), shape = RoundedCornerShape(12.dp), shadowElevation = 6.dp) {
                Column(Modifier.padding(18.dp)) {
                    Text("Start with your players", style = MaterialTheme.typography.titleMedium)
                    Text("Enable a human player, then place a starting army or castle in this world.", Modifier.padding(top = 7.dp, bottom = 12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { state.page = WorkspacePage.PLAYERS }) { Text("Configure players") }
                }
            }
        }
    }
}

@Composable
private fun LayerSwitch(label: String, value: Boolean, change: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(value, change, modifier = Modifier.size(28.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun LayerMenuItem(label: String, value: Boolean, change: (Boolean) -> Unit) {
    DropdownMenuItem(text = { Text(label) }, leadingIcon = { Checkbox(value, null) }, onClick = { change(!value) })
}
