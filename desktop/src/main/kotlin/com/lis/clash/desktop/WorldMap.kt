package com.lis.clash.desktop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
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
    var placementMenu by remember { mutableStateOf(false) }
    var autoFit by remember(state.document) { mutableStateOf(true) }
    val currentViewport by rememberUpdatedState(viewport)
    val rows = snapshot.mapHeight.coerceIn(1, 100)
    val columns = snapshot.mapWidth.coerceIn(1, 100)
    LaunchedEffect(state.document, canvasSize, autoFit) {
        if (autoFit && canvasSize.width > 0 && canvasSize.height > 0) {
            viewport = MapViewport.fit(canvasSize.width.toFloat(), canvasSize.height.toFloat(), rows, columns)
        }
    }
    LaunchedEffect(state.busy) {
        if (state.busy) {
            toolMenu = false
            presetMenu = false
            layersMenu = false
            placementMenu = false
        }
    }
    val selected = state.selection
    val parentSelection = when (selected?.kind) {
        RecordKind.ARMY_UNIT -> RecordId(RecordKind.ARMY, selected.slot)
        RecordKind.BUILDING_UNIT -> RecordId(RecordKind.BUILDING, selected.slot)
        else -> selected
    }
    val selectedEntity = snapshot.entities.find { it.id == parentSelection }
    val selectedSlot = if (selected?.kind == RecordKind.TILE) selected.slot else selectedEntity?.let { it.row * 100 + it.column }
    var focusedSequence by remember(state.document) { mutableStateOf(0) }
    LaunchedEffect(state.mapFocusSequence, canvasSize) {
        if (state.mapFocusSequence > focusedSequence && canvasSize.width > 0 && canvasSize.height > 0) {
            val target = state.mapFocusRequest
            val parent = when (target?.kind) {
                RecordKind.ARMY_UNIT -> RecordId(RecordKind.ARMY, target.slot)
                RecordKind.BUILDING_UNIT -> RecordId(RecordKind.BUILDING, target.slot)
                else -> target
            }
            val entity = snapshot.entities.find { it.id == parent }
            val slot = if (target?.kind == RecordKind.TILE) target.slot else entity?.let { it.row * 100 + it.column }
            if (slot != null) {
                val cell = max(viewport.cellSize, 12f)
                val extent = if (entity?.id?.kind == RecordKind.BUILDING && entity.type in 1..2) 2f else 1f
                autoFit = false
                viewport = MapViewport(cell, canvasSize.width / 2f - (slot % 100 + extent / 2) * cell, canvasSize.height / 2f - (slot / 100 + extent / 2) * cell)
            }
            focusedSequence = state.mapFocusSequence
        }
    }
    var strokeSlots by remember { mutableStateOf(setOf<Int>()) }
    val painting = state.tool != MapTool.SELECT
    val brush = mapBrushes.find { it.tool == state.tool && if (state.tool == MapTool.TRAP) (it.value == "0") == (state.paintValue == "0") else it.value == state.paintValue }
    val activeLayers = listOf(state.showTerrain, state.showOverlays, state.showRoads, state.showArmies, state.showBuildings, state.showOccupancy, state.showTraps).count { it }

    fun paint(slots: List<Int>) {
        if (slots.isEmpty() || state.busy) return
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
        if (state.busy) {
            // A document operation owns its snapshot until it finishes.
        } else if (state.tool != MapTool.SELECT) {
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

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().height(70.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text("World canvas", style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$columns × $rows tiles  ·  ${snapshot.entities.size} assets", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            MapPlayerFilter(state)
            Box {
                OutlinedButton(onClick = { layersMenu = true }, enabled = !state.busy, contentPadding = PaddingValues(horizontal = 12.dp)) {
                    Icon(Icons.Outlined.Layers, null, Modifier.size(17.dp))
                    Text("Layers", Modifier.padding(start = 6.dp), style = MaterialTheme.typography.labelMedium)
                    Icon(Icons.Outlined.ExpandMore, null, Modifier.size(16.dp))
                }
                DropdownMenu(layersMenu, { layersMenu = false }, modifier = Modifier.width(220.dp)) {
                    Text("MAP LAYERS", Modifier.padding(horizontal = 16.dp, vertical = 9.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LayerMenuItem("Terrain", state.showTerrain) { state.showTerrain = it }
                    LayerMenuItem("Overlays", state.showOverlays) { state.showOverlays = it }
                    LayerMenuItem("Roads & bridges", state.showRoads) { state.showRoads = it }
                    LayerMenuItem("Armies", state.showArmies) { state.showArmies = it }
                    LayerMenuItem("Buildings", state.showBuildings) { state.showBuildings = it }
                    LayerMenuItem("Occupancy", state.showOccupancy) { state.showOccupancy = it }
                    LayerMenuItem("Traps", state.showTraps) { state.showTraps = it }
                    HorizontalDivider(Modifier.padding(vertical = 5.dp))
                    LayerMenuItem("Tile grid", state.showGrid) { state.showGrid = it }
                }
            }
            Box {
                FilledTonalButton(onClick = { placementMenu = true }, enabled = !state.busy && snapshot.players.any { it.active }, contentPadding = PaddingValues(horizontal = 12.dp)) {
                    Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                    Text("Place", Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelMedium)
                    Icon(Icons.Outlined.ExpandMore, null, Modifier.size(16.dp))
                }
                DropdownMenu(placementMenu, { placementMenu = false }) {
                    DropdownMenuItem(text = { Text("Starting army") }, leadingIcon = { Icon(Icons.Outlined.Flag, null) }, onClick = { placementMenu = false; onCreate("army") })
                    DropdownMenuItem(text = { Text("Building") }, leadingIcon = { Icon(Icons.Outlined.Apartment, null) }, onClick = { placementMenu = false; onCreate("building") })
                }
            }
        }
        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f)), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Row(Modifier.padding(5.dp).height(38.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilledTonalIconToggleButton(checked = !painting, onCheckedChange = { state.tool = MapTool.SELECT }, enabled = !state.busy, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.NearMe, "Select and pan", Modifier.size(19.dp))
                }
                Box {
                    TextButton(onClick = { toolMenu = true }, enabled = !state.busy, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Icon(Icons.Outlined.Brush, null, Modifier.size(17.dp))
                        Text(if (painting) state.tool.shortLabel() else "Paint", Modifier.padding(start = 6.dp), style = MaterialTheme.typography.labelMedium)
                        Icon(Icons.Outlined.ExpandMore, null, Modifier.size(16.dp))
                    }
                    DropdownMenu(toolMenu, { toolMenu = false }) {
                        MapTool.entries.filter { it != MapTool.SELECT }.forEach { tool ->
                            DropdownMenuItem(text = { Text(tool.label) }, leadingIcon = { Icon(tool.mapIcon(), null, Modifier.size(20.dp)) }, onClick = {
                                if (state.tool != tool) state.paintValue = if (tool == MapTool.TRAP) state.defaultTrapBrushValue() else mapBrushes.first { it.tool == tool }.value
                                state.tool = tool; toolMenu = false
                            })
                        }
                    }
                }
                VerticalDivider(Modifier.height(22.dp).padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Box(Modifier.weight(1f)) {
                    TextButton(onClick = { presetMenu = true }, enabled = !state.busy, contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.fillMaxWidth()) {
                        if (brush != null) Box(Modifier.size(14.dp).clip(RoundedCornerShape(3.dp)).background(brush.color))
                        else Icon(Icons.Outlined.Palette, null, Modifier.size(17.dp))
                        Text(if (painting) brush?.label ?: "Custom brush" else "Brush library", Modifier.weight(1f).padding(horizontal = 7.dp), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Outlined.ExpandMore, null, Modifier.size(16.dp))
                    }
                    DropdownMenu(presetMenu, { presetMenu = false }, modifier = Modifier.width(280.dp).heightIn(max = 430.dp)) {
                        Text("BRUSH LIBRARY", Modifier.padding(horizontal = 16.dp, vertical = 10.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        mapBrushes.forEach { preset ->
                            DropdownMenuItem(text = { Column { Text(preset.label, style = MaterialTheme.typography.bodyMedium); Text(if (preset.tool == MapTool.TRAP && preset.value != "0") "Traps  ·  Choose an active player" else "${preset.tool.shortLabel()}  ·  ${preset.value}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                                leadingIcon = { Box(Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(preset.color)) },
                                trailingIcon = { if (preset == brush) Icon(Icons.Outlined.Check, null, Modifier.size(18.dp)) },
                                enabled = !state.busy && (preset.tool != MapTool.TRAP || preset.value == "0" || snapshot.players.any { it.active }),
                                onClick = { state.tool = preset.tool; state.paintValue = if (preset.tool == MapTool.TRAP && preset.value != "0") state.defaultTrapBrushValue() else preset.value; presetMenu = false })
                        }
                    }
                }
                if (painting) {
                    if (state.tool == MapTool.TRAP) TrapBrushControl(state)
                    else Row(Modifier.height(32.dp).width(91.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("ID", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        BasicTextField(state.paintValue, { state.paintValue = it }, enabled = !state.busy, singleLine = true,
                            textStyle = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurface), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f).padding(start = 8.dp).semantics { contentDescription = "Tile ID" })
                    }
                    MapIconButton(Icons.Outlined.RestartAlt, if (state.tool == MapTool.TERRAIN) "Reset ground" else "Erase", enabled = !state.busy) {
                        state.paintValue = if (state.tool in listOf(MapTool.TERRAIN, MapTool.TRAP)) "0" else "65535"
                    }
                } else {
                    MapIconButton(Icons.Outlined.GridOn, if (state.showGrid) "Hide tile grid" else "Show tile grid", enabled = !state.busy, active = state.showGrid) { state.showGrid = !state.showGrid }
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF243333))) {
            Canvas(Modifier.fillMaxSize()
                .semantics { contentDescription = "World map, $columns columns and $rows rows. Click to select a tile or entity; drag to pan; scroll to zoom." }
                .onSizeChanged { size ->
                    val previousSize = canvasSize
                    canvasSize = size
                    if (size.width > 0 && size.height > 0) {
                        if (autoFit) viewport = MapViewport.fit(size.width.toFloat(), size.height.toFloat(), rows, columns)
                        else if (previousSize.width > 0 && previousSize.height > 0) {
                            // Keep the same world point in view when a panel changes the canvas bounds.
                            viewport = viewport.pan((size.width - previousSize.width) / 2f, (size.height - previousSize.height) / 2f)
                        }
                    }
                }
                .onPointerEvent(PointerEventType.Move) { event -> event.changes.firstOrNull()?.position?.let { hovered = viewport.tileAt(it.x, it.y, rows, columns) } }
                .onPointerEvent(PointerEventType.Exit) { hovered = null }
                .onPointerEvent(PointerEventType.Scroll) { event ->
                    event.changes.firstOrNull()?.let { change ->
                        autoFit = false
                        viewport = viewport.zoom(1.15f.pow(-change.scrollDelta.y), change.position.x, change.position.y)
                    }
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
                            if (state.tool == MapTool.SELECT) {
                                autoFit = false
                                viewport = currentViewport.pan(delta.x, delta.y)
                            }
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
                    if (state.showTerrain && cell >= 8f) {
                        val variation = (tile.row * 31 + tile.column * 17).mod(7)
                        if (variation < 3) drawRect(Color.White.copy(alpha = .018f * (variation + 1)), Offset(left, top), Size(cell, cell))
                        if (tile.terrain in 603..610 && cell >= 12f) {
                            drawLine(Color(0xFFCEE1DD).copy(alpha = .28f), Offset(left + cell * .2f, top + cell * .55f), Offset(left + cell * .7f, top + cell * .55f), max(1f, cell * .035f))
                        } else if ((tile.terrain == 0 || tile.terrain == 4) && cell >= 16f && variation == 0) {
                            drawLine(Color(0xFF254839).copy(alpha = .2f), Offset(left + cell * .62f, top + cell * .55f), Offset(left + cell * .69f, top + cell * .37f), max(1f, cell * .04f))
                        }
                    }
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
                    if (state.showGrid && cell >= 7f) {
                        drawRect(Color(0xFF21352C).copy(alpha = .17f), Offset(left, top), Size(cell, cell), style = Stroke(1f))
                        if (tile.column % 10 == 0) drawLine(Color(0xFF21352C).copy(alpha = .32f), Offset(left, top), Offset(left, top + cell), 1f)
                        if (tile.row % 10 == 0) drawLine(Color(0xFF21352C).copy(alpha = .32f), Offset(left, top), Offset(left + cell, top), 1f)
                    }
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
                hovered?.let { slot ->
                    val origin = Offset(ox + slot % 100 * cell, oy + slot / 100 * cell)
                    if (painting && !state.busy) drawRect((brush?.color ?: Color.White).copy(alpha = .45f), origin, Size(cell, cell))
                    drawRect(Color.White.copy(alpha = .8f), origin, Size(cell, cell), style = Stroke(if (painting) 2f else 1f))
                }
            }
            Surface(Modifier.align(Alignment.BottomEnd).padding(12.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .96f), shape = RoundedCornerShape(9.dp), shadowElevation = 4.dp) {
                Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    MapIconButton(Icons.Outlined.Remove, "Zoom out") { autoFit = false; viewport = viewport.zoom(.8f, canvasSize.width / 2f, canvasSize.height / 2f) }
                    Text("${(viewport.cellSize / 8f * 100).toInt()}%", Modifier.width(43.dp), style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    MapIconButton(Icons.Outlined.Add, "Zoom in") { autoFit = false; viewport = viewport.zoom(1.25f, canvasSize.width / 2f, canvasSize.height / 2f) }
                    VerticalDivider(Modifier.height(18.dp).padding(horizontal = 3.dp))
                    MapIconButton(Icons.Outlined.FitScreen, "Fit map to window", active = autoFit) { autoFit = true; viewport = MapViewport.fit(canvasSize.width.toFloat(), canvasSize.height.toFloat(), rows, columns) }
                }
            }
            if (snapshot.players.none { it.active }) Surface(Modifier.align(Alignment.TopCenter).padding(18.dp).widthIn(max = 370.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .97f), shape = RoundedCornerShape(12.dp), shadowElevation = 6.dp) {
                Column(Modifier.padding(18.dp)) {
                    Text("Start with your players", style = MaterialTheme.typography.titleMedium)
                    Text("Enable a human player, then place a starting army or castle in this world.", Modifier.padding(top = 7.dp, bottom = 12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { state.page = WorkspacePage.PLAYERS }) { Text("Configure players") }
                }
            }
        }
        Row(Modifier.fillMaxWidth().height(36.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(hovered?.let { "Row ${it / 100}  ·  Col ${it % 100}  ·  #$it" } ?: if (painting) "Drag to paint  ·  Select tool to pan" else "Drag to pan  ·  Scroll to zoom", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val hoveredTile = hovered?.let { slot -> snapshot.tiles.getOrNull(slot)?.takeIf { it.slot == slot } }
            Text(if (hoveredTile != null) "Terrain ${hoveredTile.terrain}" else "$activeLayers of 7 layers", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LayerMenuItem(label: String, value: Boolean, change: (Boolean) -> Unit) {
    DropdownMenuItem(text = { Text(label, style = MaterialTheme.typography.bodyMedium) }, trailingIcon = { Checkbox(value, null, modifier = Modifier.size(22.dp)) }, onClick = { change(!value) })
}

@Composable
private fun MapPlayerFilter(state: EditorState) {
    var expanded by remember { mutableStateOf(false) }
    val owner = state.ownerFilter
    val label = owner?.let { slot -> state.snapshot.players.find { it.id.slot == slot }?.name ?: "Player $slot" } ?: "All players"
    LaunchedEffect(state.busy) { if (state.busy) expanded = false }
    Box {
        Surface(shape = RoundedCornerShape(8.dp), color = if (owner == null) Color.Transparent else MaterialTheme.colorScheme.primaryContainer) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { expanded = true }, enabled = !state.busy, contentPadding = PaddingValues(horizontal = 9.dp),
                    modifier = Modifier.widthIn(max = 134.dp).semantics { contentDescription = "Filter map by player: $label" }) {
                    if (owner == null) Icon(Icons.Outlined.FilterList, null, Modifier.size(17.dp))
                    else Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(playerColors.getOrElse(owner) { MaterialTheme.colorScheme.primary }))
                    Text(label, Modifier.weight(1f, fill = false).padding(start = 6.dp), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Icon(Icons.Outlined.ExpandMore, null, Modifier.size(15.dp))
                }
                if (owner != null) StudioIconButton("Clear player filter", Icons.Outlined.Close, enabled = !state.busy) { state.ownerFilter = null }
            }
        }
        DropdownMenu(expanded, { expanded = false }, modifier = Modifier.widthIn(min = 210.dp, max = 280.dp)) {
            DropdownMenuItem(text = { Text("All players") }, leadingIcon = { Icon(Icons.Outlined.People, null, Modifier.size(20.dp)) },
                trailingIcon = { if (owner == null) Icon(Icons.Outlined.Check, null, Modifier.size(18.dp)) }, enabled = !state.busy,
                onClick = { state.ownerFilter = null; expanded = false })
            state.snapshot.players.filter { it.active }.forEach { player ->
                DropdownMenuItem(text = { Column { Text(player.name, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("Player ${player.id.slot}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                    leadingIcon = { Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(playerColors.getOrElse(player.id.slot) { MaterialTheme.colorScheme.primary })) },
                    trailingIcon = { if (owner == player.id.slot) Icon(Icons.Outlined.Check, null, Modifier.size(18.dp)) }, enabled = !state.busy,
                    onClick = { state.ownerFilter = player.id.slot; expanded = false })
            }
        }
    }
}

@Composable
private fun MapIconButton(icon: ImageVector, label: String, enabled: Boolean = true, active: Boolean = false, action: () -> Unit) {
    StudioIconButton(label, icon, enabled, tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, onClick = action)
}

private fun EditorState.defaultTrapBrushValue(): String {
    val player = snapshot.players.firstOrNull { it.active && it.id.slot == ownerFilter } ?: snapshot.players.firstOrNull { it.active }
    return player?.let { (1 shl it.id.slot).toString() } ?: "0"
}

@Composable
private fun TrapBrushControl(state: EditorState) {
    var expanded by remember { mutableStateOf(false) }
    var advanced by remember { mutableStateOf(false) }
    var maskDraft by remember { mutableStateOf("") }
    val players = state.snapshot.players.filter { it.active }
    val player = players.find { state.paintValue.toIntOrNull() == (1 shl it.id.slot) }
    val label = if (state.paintValue == "0") "Clear trap" else player?.name ?: "Custom mask"
    LaunchedEffect(state.busy) { if (state.busy) { expanded = false; advanced = false } }
    Box {
        OutlinedButton(onClick = { expanded = true }, enabled = !state.busy, contentPadding = PaddingValues(horizontal = 8.dp),
            modifier = Modifier.widthIn(max = 144.dp).height(34.dp).semantics { contentDescription = "Trap player: $label" }) {
            Text(label, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Outlined.ExpandMore, null, Modifier.padding(start = 4.dp).size(16.dp))
        }
        DropdownMenu(expanded, { expanded = false }, modifier = Modifier.widthIn(min = 210.dp, max = 280.dp)) {
            players.forEach { candidate ->
                DropdownMenuItem(text = { Text(candidate.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = { Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(playerColors.getOrElse(candidate.id.slot) { MaterialTheme.colorScheme.primary })) },
                    trailingIcon = { if (candidate == player) Icon(Icons.Outlined.Check, null, Modifier.size(18.dp)) },
                    onClick = { state.paintValue = (1 shl candidate.id.slot).toString(); expanded = false })
            }
            DropdownMenuItem(text = { Text("Clear trap") }, leadingIcon = { Icon(Icons.Outlined.RestartAlt, null, Modifier.size(19.dp)) }, onClick = { state.paintValue = "0"; expanded = false })
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            DropdownMenuItem(text = { Text("Advanced mask") }, leadingIcon = { Icon(Icons.Outlined.Tune, null, Modifier.size(19.dp)) }, onClick = { maskDraft = state.paintValue; expanded = false; advanced = true })
        }
    }
    if (advanced) AlertDialog(onDismissRequest = { advanced = false }, title = { Text("Advanced trap mask") },
        text = {
            Column(Modifier.width(320.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Use an exact bit mask when the trap needs more than one player. Each active player occupies its corresponding bit; zero clears the trap.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(maskDraft, { maskDraft = it }, label = { Text("Owner mask") }, singleLine = true, supportingText = { Text("Whole number from 0 to 31") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(onClick = { state.paintValue = maskDraft; advanced = false }, enabled = !state.busy && maskDraft.toIntOrNull()?.let { it in 0..31 } == true) { Text("Use mask") } },
        dismissButton = { TextButton(onClick = { advanced = false }) { Text("Cancel") } })
}

private fun MapTool.shortLabel(): String = when (this) {
    MapTool.SELECT -> "Select"
    MapTool.TERRAIN -> "Terrain"
    MapTool.OVERLAY -> "Overlay"
    MapTool.ROAD -> "Road"
    MapTool.TRAP -> "Traps"
}

private fun MapTool.mapIcon(): ImageVector = when (this) {
    MapTool.SELECT -> Icons.Outlined.NearMe
    MapTool.TERRAIN -> Icons.Outlined.Terrain
    MapTool.OVERLAY -> Icons.Outlined.Layers
    MapTool.ROAD -> Icons.Outlined.Route
    MapTool.TRAP -> Icons.Outlined.WarningAmber
}

private data class MapBrush(val label: String, val tool: MapTool, val value: String, val color: Color)

private val mapBrushes = listOf(
    MapBrush("Grassland", MapTool.TERRAIN, "0", Color(0xFF668B6B)),
    MapBrush("Grassland variant", MapTool.TERRAIN, "4", Color(0xFF7C9971)),
    MapBrush("Castle foundation", MapTool.TERRAIN, "707", Color(0xFFB3986A)),
    MapBrush("Treasure", MapTool.TERRAIN, "752", Color(0xFFE5BF61)),
    MapBrush("Religious site", MapTool.OVERLAY, "728", Color(0xFF9DADC0)),
    MapBrush("Road / bridge", MapTool.ROAD, "872", Color(0xFFD8C698)),
    MapBrush("Clear overlay", MapTool.OVERLAY, "65535", Color(0xFFBAC6BF)),
    MapBrush("Clear road", MapTool.ROAD, "65535", Color(0xFFBAC6BF)),
    MapBrush("Place trap", MapTool.TRAP, "1", Color(0xFFCB8465)),
    MapBrush("Clear trap", MapTool.TRAP, "0", Color(0xFFBAC6BF))
)
