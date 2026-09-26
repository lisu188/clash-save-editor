package com.lis.clash.editor

import com.lis.clash.SaveFormat as F
import com.lis.clash.getClassDescriptor
import com.lis.clash.objects.*
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/** Authoritative bytes with copy-on-command transactions; decoded UI objects never own writable state. */
class SaveDocument private constructor(dat: ByteArray, fac: ByteArray?, encoding: String) {
    private data class State(val dat: ByteArray, val fac: ByteArray?, val encoding: String)
    private var state = State(dat.copyOf(), fac?.copyOf(), Charset.forName(encoding).name())
    val encoding: String get() = state.encoding
    private var saved: State? = state
    private val undoStates = ArrayDeque<State>()
    private val redoStates = ArrayDeque<State>()
    var revision: Long = 0; private set
    val dirty: Boolean get() = saved?.let { same(it, state) } != true
    val canUndo: Boolean get() = undoStates.isNotEmpty()
    val canRedo: Boolean get() = redoStates.isNotEmpty()
    fun datBytes() = state.dat.copyOf()
    fun facBytes() = state.fac?.copyOf()
    fun markSaved() { saved = state }
    /** Reinterpret legacy names without transcoding a byte. Subsequent text edits use this charset. */
    fun setEncoding(name: String) { commit(state.copy(encoding = Charset.forName(name).name())) }
    fun undo(): Boolean {
        if (undoStates.isEmpty()) return false
        redoStates.addLast(state); state = undoStates.removeLast(); revision++; return true
    }
    fun redo(): Boolean {
        if (redoStates.isEmpty()) return false
        undoStates.addLast(state); state = redoStates.removeLast(); revision++; return true
    }

    fun execute(command: EditCommand): RecordId? {
        val working = Transaction(state.dat.copyOf(), state.fac?.copyOf())
        val result = working.apply(command)
        commit(State(working.dat, working.fac, state.encoding))
        return result
    }

    private fun commit(candidate: State) {
        if (!same(state, candidate)) {
            undoStates.addLast(state)
            if (undoStates.size > 50) undoStates.removeFirst()
            redoStates.clear(); state = candidate; revision++
        }
    }

    fun snapshot(): DocumentSnapshot {
        val b = state.dat
        val players = (0 until 5).map { slot ->
            val p = playerOffset(slot)
            PlayerSnapshot(RecordId(RecordKind.PLAYER, slot), b.text(p + 4, 11), b.int(p) != 0,
                b.int(p + 27) != 0, b.int(p + 31), b.int(p + 39))
        }
        val entities = buildList {
            for (slot in 0 until 500) if (armyActive(b, slot)) {
                val id = RecordId(RecordKind.ARMY, slot); val p = byteOffset(id)
                add(EntitySnapshot(id, "Army $slot", b.signed16(p), b.signed16(p + 2), b.u8(p + 4), 0, units(b, id)))
            }
            for (slot in 0 until 100) if (buildingActive(b, slot)) {
                val id = RecordId(RecordKind.BUILDING, slot); val p = byteOffset(id)
                add(EntitySnapshot(id, b.text(p + 5, 11), b.u8(p), b.u8(p + 1), b.u8(p + 2), b.u8(p + 4), units(b, id)))
            }
        }
        // The original width field bounds rows; expose conventional column/row dimensions to Canvas.
        return DocumentSnapshot(b.text(0, 16), b.int(F.MAP_HEIGHT_FILE_OFFSET), b.int(F.MAP_WIDTH_FILE_OFFSET),
            List(10000) { slot -> val p = F.TILE_RECORDS_FILE_OFFSET + slot * 14
                TileSnapshot(slot, slot / 100, slot % 100, b.u16(p), b.u16(p + 2), b.u16(p + 4),
                    b.u16(F.OCCUPANCY_RECORDS_FILE_OFFSET + 2 * slot), b.u8(F.TRAP_MASK_RECORDS_FILE_OFFSET + slot))
            }, players, entities, revision, dirty, validate())
    }

    fun properties(id: RecordId): List<PropertySnapshot> {
        val obj = resolve(Save.parse(state.dat, Charset.forName(encoding)), id)
        return getClassDescriptor(obj::class).getSimpleProperties().sortedBy { it.index() }.map {
            val value = it.get(obj)
            PropertySnapshot(it.getName(), if (value is List<*>) value.joinToString(" ") else value.toString(),
                !it.isReadOnly() && !paired(id, it.getName()) && value !is List<*>)
        }
    }

    fun validate(forExport: Boolean = false): List<ValidationIssue> {
        val b = state.dat
        val issues = mutableListOf<ValidationIssue>()
        fun error(message: String, id: RecordId? = null) { issues += ValidationIssue(IssueSeverity.ERROR, message, id) }
        val rows = b.int(F.MAP_WIDTH_FILE_OFFSET); val columns = b.int(F.MAP_HEIGHT_FILE_OFFSET)
        if (rows !in 1..100 || columns !in 1..100) error("Map dimensions must each be within 1–100")
        val fac = state.fac?.let { runCatching { FacDocument.parse(it) }.getOrElse { ex -> error("Malformed FAC: ${ex.message}"); null } }
        if (state.fac == null) issues += ValidationIssue(if (forExport) IssueSeverity.ERROR else IssueSeverity.WARNING,
            "Companion FAC is missing; rules-dependent editing and playable export require it")
        fac?.structuralProblem()?.let { issues += ValidationIssue(if (forExport && b.int(F.ACTIVE_MISSION_FILE_OFFSET) == -1) IssueSeverity.ERROR else IssueSeverity.WARNING, it) }
        val expected = IntArray(10000) { 65535 }
        val assets = IntArray(5)
        for (kind in listOf(RecordKind.ARMY, RecordKind.BUILDING)) {
            for (slot in 0 until if (kind == RecordKind.ARMY) 500 else 100) {
                if (!(if (kind == RecordKind.ARMY) armyActive(b, slot) else buildingActive(b, slot))) continue
                val id = RecordId(kind, slot); val p = byteOffset(id)
                val owner = b.u8(p + if (kind == RecordKind.ARMY) 4 else 2)
                val row = if (kind == RecordKind.ARMY) b.signed16(p) else b.u8(p)
                val col = if (kind == RecordKind.ARMY) b.signed16(p + 2) else b.u8(p + 1)
                if (owner !in 0..4 || b.int(playerOffset(owner.coerceIn(0, 4))) == 0) error("Entity owner must be an active player", id)
                else if (kind == RecordKind.ARMY || b.u8(p + 4) in 1..2 || b.signed16(p + 18) in 0..40) assets[owner]++
                val type = if (kind == RecordKind.ARMY) 0 else b.u8(p + 4)
                if (kind == RecordKind.BUILDING && type == 3) {
                    issues += ValidationIssue(IssueSeverity.WARNING, "Building type 3 footprint remains inspection-only", id)
                    continue
                }
                val width = if (kind == RecordKind.BUILDING && type in 1..2) 2 else 1
                if (row < 0 || col < 0 || row + width > rows || col + width > columns) error("Entity footprint is outside the map", id)
                else if (!(kind == RecordKind.ARMY && b.u8(p + 720) != 0)) {
                    for (r in row until row + width) for (c in col until col + width) {
                        val tile = r * 100 + c
                        if (expected[tile] != 65535) error("Entity footprints overlap at $r,$c", id)
                        expected[tile] = occupant(id)
                    }
                }
                val (unitBase, count) = unitRange(id)
                var empty = false
                for (u in 0 until count) {
                    val up = unitBase + u * 31; val ut = b.signed16(up)
                    if (ut == -1) empty = true else {
                        if (ut !in 0..40) error("Unsupported unit type $ut in slot $u", id)
                        if (empty) error("Unit slots must form a packed sequence", id)
                        if (b.u8(up + 2) != owner) error("Unit owner differs from its containing entity", id)
                        if (ut !in 31..32 && b.u8(up + 9) !in 1..100) error("Unit health must be within 1–100", id)
                        if (b.u8(up + 10) > 100 || b.u8(up + 11) > 20) error("Unit fatigue must be 0–100 and morale 0–20", id)
                    }
                }
            }
        }
        val mismatches = (0 until 10000).count { expected[it] != b.u16(F.OCCUPANCY_RECORDS_FILE_OFFSET + 2 * it) }
        if (mismatches != 0) error("Occupancy differs from entity footprints in $mismatches cells")
        if (forExport) {
            val visibleTiles = (0 until rows.coerceIn(0, 100)).flatMap { r -> (0 until columns.coerceIn(0, 100)).map { c -> r * 100 + c } }
            if (visibleTiles.any { val p = F.TILE_RECORDS_FILE_OFFSET + it * 14; b.u16(p) !in 0..1023 || listOf(b.u16(p + 2), b.u16(p + 4)).any { value -> value != 65535 && value !in 0..1023 } })
                error("Visible terrain cannot contain empty cells or sprite IDs outside the recovered 1024-entry table")
            val active = (0..4).filter { b.int(playerOffset(it)) != 0 }
            val activeMask = active.fold(0) { mask, slot -> mask or (1 shl slot) }
            for (slot in 0 until 10000) {
                val trapMask = b.u8(F.TRAP_MASK_RECORDS_FILE_OFFSET + slot)
                if (trapMask and activeMask.inv() != 0) error("Trap owner mask must reference active players only", RecordId(RecordKind.TILE, slot))
                if (trapMask != 0 && (slot / 100 >= rows || slot % 100 >= columns)) error("Trap is outside the active map", RecordId(RecordKind.TILE, slot))
            }
            if (active.size < 2) error("Playable export requires at least two active players")
            if (active.none { b.int(playerOffset(it) + 27) != 0 }) error("Playable export requires at least one human player")
            active.filter { assets[it] == 0 }.forEach { error("Player $it needs an army or qualifying building", RecordId(RecordKind.PLAYER, it)) }
            if (b.int(F.TURN_OWNER_FILE_OFFSET) !in active || b.int(F.VIEWED_PLAYER_FILE_OFFSET) !in active) error("Turn owner and viewed player must be active")
            if (fac != null && b.int(F.ACTIVE_MISSION_FILE_OFFSET) == -1) {
                active.forEach { slot ->
                    val p = playerOffset(slot)
                    val expectedFact = listOf("gameinfo", "gracz", "$slot", "komputer", "${1 - b.int(p + 27)}", "inteligencja", "${b.int(p + 31)}", "chrzesc", "${b.int(p + 39)}")
                    if (fac.forms.count { it.atoms == expectedFact } != 1) error("FAC gameinfo does not match player $slot")
                }
                if (fac.forms.count { it.atoms == listOf("misja", "-1") } != 1) error("FAC requires one free-game mission fact")
                if (fac.forms.count { it.atoms == listOf("initial-fact") } != 1) error("FAC requires one initial-fact")
                for (form in fac.forms) {
                    val a = form.atoms
                    if (a.size == 9 && a[0] == "gameinfo" && a[2].toIntOrNull() !in active) error("FAC gameinfo references an inactive player")
                    val building = when {
                        a.size == 4 && a.take(3) == listOf("zamek", "w", "budowie") -> a[3].toIntOrNull()
                        a.size == 3 && a.take(2) == listOf("zbudowano", "zamek") -> a[2].toIntOrNull()
                        a.size == 4 && a[0] == "schemat" -> a[2].toIntOrNull()
                        else -> null
                    }
                    if (building != null && building in 0..99) {
                        if (!buildingActive(b, building)) error("FAC ${a[0]} references an inactive building $building")
                        else if (a[0] == "schemat" && a[1].toIntOrNull() != b.u8(buildingOffset(building) + 2)) error("FAC scheme owner differs from building $building")
                        else if (a[0] == "zamek" && b.signed16(buildingOffset(building) + 16) == 0) error("FAC construction fact references completed building $building")
                        else if (a[0] == "zbudowano" && b.signed16(buildingOffset(building) + 16) != 0) error("FAC completed fact references unfinished building $building")
                    }
                }
                val siteFacts = fac.forms.map { it.atoms }.filter { it.firstOrNull() in setOf("swiatynia", "skarb", "zamek_place", "pulapka") }.groupingBy { it }.eachCount()
                if (siteFacts.keys.any { it.size != 3 || it[1].toIntOrNull() !in 0 until rows || it[2].toIntOrNull() !in 0 until columns }) error("FAC site facts reference cells outside the active map")
                for (slot in visibleTiles) {
                    val p = F.TILE_RECORDS_FILE_OFFSET + slot * 14
                    for ((kind, present) in listOf("swiatynia" to (b.u16(p + 2) in 728..739), "skarb" to (b.u16(p) in setOf(752, 755)),
                        "zamek_place" to (b.u16(p) in setOf(707, 711)), "pulapka" to (b.u8(F.TRAP_MASK_RECORDS_FILE_OFFSET + slot) != 0))) {
                        val count = siteFacts[listOf(kind, "${slot / 100}", "${slot % 100}")] ?: 0
                        if ((present && count != 1) || (!present && count != 0)) error("FAC $kind facts do not match tile ${slot / 100},${slot % 100}", RecordId(RecordKind.TILE, slot))
                    }
                }
            }
        }
        return issues.distinct()
    }

    private inner class Transaction(val dat: ByteArray, var fac: ByteArray?) {
        private fun rules(): FacDocument {
            require(dat.int(F.ACTIVE_MISSION_FILE_OFFSET) == -1) { "Structural editing is unavailable for campaign saves because objectives can reference fixed entities and coordinates" }
            val parsed = FacDocument.parse(requireNotNull(fac) { "This operation requires the companion FAC" })
            require(parsed.structuralProblem() == null) { parsed.structuralProblem()!! }
            return parsed
        }
        private fun updateRules(transform: (FacDocument) -> FacDocument) { fac = transform(rules()).toByteArray() }
        private fun requireEntity(id: RecordId) {
            byteOffset(id)
            require(when (id.kind) { RecordKind.ARMY -> armyActive(dat, id.slot); RecordKind.BUILDING -> buildingActive(dat, id.slot); else -> false }) { "Select an active army or building" }
            if (id.kind == RecordKind.BUILDING) require(dat.u8(byteOffset(id) + 4) in 0..2) { "Building type 3 is inspection-only" }
            val (base, count) = unitRange(id)
            var emptySeen = false
            repeat(count) { slot ->
                val type = dat.signed16(base + 31 * slot)
                if (type == -1) emptySeen = true else {
                    require(type in 0..40 && !emptySeen) { "Malformed unit sequence; preserve this record for advanced inspection" }
                }
            }
        }
        private fun owner(value: Int) { require(value in 0..4 && dat.int(playerOffset(value)) != 0) { "Owner must be an active player 0–4" } }
        private fun position(row: Int, col: Int, width: Int, self: RecordId? = null): List<Int> {
            val rows = dat.int(F.MAP_WIDTH_FILE_OFFSET); val columns = dat.int(F.MAP_HEIGHT_FILE_OFFSET)
            require(rows in 1..100 && columns in 1..100) { "Map bounds must be within 1–100 before placing entities" }
            require(row in 0 until rows && col in 0 until columns && row <= rows - width && col <= columns - width) { "Footprint is outside the map" }
            return (row until row + width).flatMap { r -> (col until col + width).map { c -> r * 100 + c } }.also { cells ->
                require(cells.all { val v = dat.u16(F.OCCUPANCY_RECORDS_FILE_OFFSET + 2 * it); v == 65535 || (self != null && v == occupant(self)) }) { "Target footprint is occupied" }
                require(cells.all { slot ->
                    val p = F.TILE_RECORDS_FILE_OFFSET + slot * 14
                    dat.u16(p) in TileEditingPolicy.placementGroundIds && dat.u16(p + 2) == 65535 && dat.u8(F.TRAP_MASK_RECORDS_FILE_OFFSET + slot) == 0
                }) { "Placement requires supported clear ground (0, 4, 707 or 711), without sites or traps" }
            }
        }
        private fun clearOccupancy(id: RecordId) {
            for (slot in 0 until 10000) if (dat.u16(F.OCCUPANCY_RECORDS_FILE_OFFSET + slot * 2) == occupant(id)) dat.put(F.OCCUPANCY_RECORDS_FILE_OFFSET + slot * 2, 2, 65535)
        }
        private fun move(id: RecordId, row: Int, column: Int) {
            requireEntity(id)
            val p = byteOffset(id); val army = id.kind == RecordKind.ARMY
            val width = if (!army && dat.u8(p + 4) in 1..2) 2 else 1
            val cells = position(row, column, width, id)
            clearOccupancy(id)
            dat.put(p, if (army) 2 else 1, row); dat.put(p + if (army) 2 else 1, if (army) 2 else 1, column)
            if (army) { dat.fill(0, p + 316, p + 720); dat[p + 720] = 0 }
            cells.forEach { dat.put(F.OCCUPANCY_RECORDS_FILE_OFFSET + 2 * it, 2, occupant(id)) }
            reveal(dat, row, column, dat.u8(p + if (army) 4 else 2))
        }
        private fun free(kind: RecordKind): RecordId {
            val slot = (0 until if (kind == RecordKind.ARMY) 500 else 100).firstOrNull {
                if (kind == RecordKind.ARMY) (0 until 10).all { u -> dat.signed16(armyOffset(it) + 6 + u * 31) == -1 }
                else dat.u8(buildingOffset(it) + 4) == 255 && dat.signed16(buildingOffset(it) + 16) == -1 &&
                    (0 until 12).all { u -> dat.signed16(buildingOffset(it) + 18 + u * 31) == -1 }
            } ?: error("${kind.name.lowercase()} table is full")
            return RecordId(kind, slot)
        }

        fun apply(command: EditCommand): RecordId? = when (command) {
            is EditCommand.SetProperty -> {
                require(!paired(command.id, command.property)) { "${command.property} requires a transactional editing command" }
                val save = Save.parse(dat, Charset.forName(encoding)); val obj = resolve(save, command.id)
                val descriptor = requireNotNull(getClassDescriptor(obj::class).getSimpleProperty(command.property)) { "Unknown property ${command.property}" }
                require(!descriptor.isReadOnly()) { "${command.property} is read-only runtime state" }
                if (obj is com.lis.clash.objects.Unit) {
                    val range = when (command.property) {
                        "currentHealthPercent" -> if (obj.typeId in 31..32) 0..255 else 1..100
                        "fatigue" -> 0..100
                        "morale" -> 0..20
                        else -> null
                    }
                    if (range != null) require(command.value.toIntOrNull() in range) { "${command.property} must be within ${range.first}–${range.last}" }
                }
                try { descriptor.setString(obj, command.value) }
                catch (failure: java.lang.reflect.InvocationTargetException) { throw failure.targetException }
                save.toByteArray().copyInto(dat)
                command.id
            }
            is EditCommand.ConfigurePlayer -> {
                rules(); require(command.slot in 0..4); require(command.intelligence in 0..2); require(command.religion in 0..1)
                if (!command.active) require((0 until 500).none { armyActive(dat, it) && dat.u8(armyOffset(it) + 4) == command.slot } &&
                    (0 until 100).none { buildingActive(dat, it) && dat.u8(buildingOffset(it) + 2) == command.slot } &&
                    (0 until 10000).none { dat.u8(F.TRAP_MASK_RECORDS_FILE_OFFSET + it) and (1 shl command.slot) != 0 }) { "Transfer or delete the player's entities and traps before deactivation" }
                val p = playerOffset(command.slot)
                dat.put(p, 4, if (command.active) 1 else 0); dat.writeText(p + 4, 11, command.name)
                dat.put(p + 27, 4, if (command.human) 1 else 0); dat.put(p + 31, 4, command.intelligence); dat.put(p + 39, 4, command.religion)
                updateRules { it.player(command.slot, command.active, command.human, command.intelligence, command.religion) }
                val first = (0..4).firstOrNull { dat.int(playerOffset(it)) != 0 } ?: 0
                for (offset in listOf(F.TURN_OWNER_FILE_OFFSET, F.VIEWED_PLAYER_FILE_OFFSET)) {
                    val old = dat.int(offset); if (old !in 0..4 || dat.int(playerOffset(old.coerceIn(0, 4))) == 0) dat.put(offset, 4, first)
                }
                RecordId(RecordKind.PLAYER, command.slot)
            }
            is EditCommand.CreateArmy -> {
                rules(); owner(command.owner); position(command.row, command.column, 1)
                val id = free(RecordKind.ARMY); val p = byteOffset(id)
                dat.fill(0, p, p + 725); repeat(10) { dat.put(p + 6 + it * 31, 2, -1) }
                initUnit(dat, p + 6, command.unitType, command.owner); dat[p + 4] = command.owner.toByte()
                move(id, command.row, command.column); id
            }
            is EditCommand.CreateBuilding -> {
                rules(); owner(command.owner); require(command.type in 0..2) { "Only building types 0–2 are supported" }
                position(command.row, command.column, if (command.type == 0) 1 else 2)
                val id = free(RecordKind.BUILDING); val p = byteOffset(id)
                dat.fill(0, p, p + 467); dat[p + 2] = command.owner.toByte(); dat[p + 3] = command.owner.toByte(); dat[p + 4] = command.type.toByte()
                dat.writeText(p + 5, 11, command.name)
                // Building_New initializes work remaining to 100/300. Editor placement is a completed scenario building.
                dat.put(p + 16, 2, 0); repeat(12) { dat.put(p + 18 + it * 31, 2, -1); dat[p + 402 + it] = (-1).toByte() }
                dat[p + 402] = 0; dat[p + 414] = (-1).toByte(); dat[p + 434] = 50; dat[p + 437] = 50
                // The completed-construction branch of Unit_UpdatePerTurn initializes every integrity byte to 100.
                dat.fill(100, p + 422, p + 429)
                val human = dat.int(playerOffset(command.owner) + 27) != 0
                if (command.type != 0) { dat.put(p + 438, 4, if (human) 200 else 300); dat.put(p + 430, 2, if (human) 100 else 250) }
                if (command.type == 1) { dat[p + 421] = 1; dat[p + 416] = 2; dat.put(p + 430, 2, 0) }
                dat[p + 444] = (dat.u8(playerOffset(command.owner) + 47) and 7).toByte()
                repeat(3) { dat[p + 445 + it * 6] = (-1).toByte() }
                if (command.type == 2 && dat.int(playerOffset(command.owner) + 43) == -1) dat.put(playerOffset(command.owner) + 43, 4, id.slot)
                move(id, command.row, command.column); id
            }
            is EditCommand.Move -> { rules(); move(command.id, command.row, command.column); command.id }
            is EditCommand.Clone -> {
                rules(); requireEntity(command.id)
                val id = free(command.id.kind); val p = byteOffset(id); val from = byteOffset(command.id)
                dat.copyInto(dat, p, from, from + recordSize(id))
                dat.put(p + if (id.kind == RecordKind.ARMY) 721 else 463, 4, 0)
                if (id.kind == RecordKind.BUILDING) {
                    updateRules { it.cloneBuilding(command.id.slot, id.slot) }
                    val owner = dat.u8(p + 2)
                    if (dat.u8(p + 4) == 2 && dat.int(playerOffset(owner) + 43) == -1) dat.put(playerOffset(owner) + 43, 4, id.slot)
                }
                move(id, command.row, command.column); id
            }
            is EditCommand.Delete -> {
                rules(); requireEntity(command.id); val p = byteOffset(command.id); clearOccupancy(command.id)
                val (base, count) = unitRange(command.id); repeat(count) { dat.put(base + 31 * it, 2, -1) }
                if (command.id.kind == RecordKind.ARMY) { dat.fill(0, p + 316, p + 725) }
                else {
                    dat[p + 4] = (-1).toByte(); dat.put(p + 16, 2, -1); dat.put(p + 463, 4, 0)
                    updateRules { it.removeBuilding(command.id.slot) }
                    for (player in 0..4) if (dat.int(playerOffset(player) + 43) == command.id.slot) dat.put(playerOffset(player) + 43, 4, -1)
                }
                null
            }
            is EditCommand.ChangeOwner -> {
                rules(); requireEntity(command.id); owner(command.owner)
                val p = byteOffset(command.id); val oldOwner = dat.u8(p + if (command.id.kind == RecordKind.ARMY) 4 else 2)
                dat[p + if (command.id.kind == RecordKind.ARMY) 4 else 2] = command.owner.toByte()
                val (base, count) = unitRange(command.id); repeat(count) { if (dat.signed16(base + 31 * it) != -1) dat[base + 31 * it + 2] = command.owner.toByte() }
                if (command.id.kind == RecordKind.ARMY) dat.fill(0, p + 316, p + 720)
                else {
                    dat[p + 3] = command.owner.toByte(); updateRules { it.buildingOwner(command.id.slot, command.owner) }
                    if (oldOwner in 0..4 && oldOwner != command.owner && dat.int(playerOffset(oldOwner) + 43) == command.id.slot) {
                        val next = (0 until 100).firstOrNull { buildingActive(dat, it) && dat.u8(buildingOffset(it) + 2) == oldOwner && dat.u8(buildingOffset(it) + 4) == 2 } ?: -1
                        dat.put(playerOffset(oldOwner) + 43, 4, next)
                    }
                    if (dat.u8(p + 4) == 2 && dat.int(playerOffset(command.owner) + 43) == -1) dat.put(playerOffset(command.owner) + 43, 4, command.id.slot)
                }
                command.id
            }
            is EditCommand.AddUnit -> {
                rules(); requireEntity(command.id); val (base, count) = unitRange(command.id)
                val slot = (0 until count).firstOrNull { dat.signed16(base + it * 31) == -1 } ?: error("Unit capacity is $count")
                val p = byteOffset(command.id); initUnit(dat, base + slot * 31, command.type, dat.u8(p + if (command.id.kind == RecordKind.ARMY) 4 else 2))
                if (command.id.kind == RecordKind.ARMY) dat.fill(0, p + 316, p + 720) else dat[p + 390 + slot] = 0
                command.id
            }
            is EditCommand.RemoveUnit -> {
                rules(); requireEntity(command.id); val (base, count) = unitRange(command.id)
                val occupied = (0 until count).takeWhile { dat.signed16(base + it * 31) != -1 }
                require(command.unitSlot in occupied) { "Unit slot is empty" }
                if (command.id.kind == RecordKind.ARMY && occupied.size == 1) apply(EditCommand.Delete(command.id))
                else {
                    dat.copyInto(dat, base + command.unitSlot * 31, base + (command.unitSlot + 1) * 31, base + occupied.size * 31)
                    dat.put(base + (occupied.size - 1) * 31, 2, -1)
                    if (command.id.kind == RecordKind.BUILDING) {
                        val orders = byteOffset(command.id) + 390
                        dat.copyInto(dat, orders + command.unitSlot, orders + command.unitSlot + 1, orders + occupied.size)
                        dat[orders + occupied.size - 1] = 0
                    } else dat.fill(0, byteOffset(command.id) + 316, byteOffset(command.id) + 720)
                    command.id
                }
            }
            is EditCommand.PaintTiles -> {
                rules()
                command.terrain?.let { require(it in TileEditingPolicy.terrainIds) { "Terrain editing supports recovered IDs ${TileEditingPolicy.terrainIds}; port creation is unavailable" } }
                command.overlay?.let { require(it in TileEditingPolicy.overlayIds) { "Overlay editing supports religious sites 728–739 or 65535 to clear" } }
                command.road?.let { require(it in TileEditingPolicy.roadIds) { "Road editing supports 866–876, 949–952 or 65535 to clear" } }
                val slots = command.slots.distinct(); require(slots.all { it in 0 until 10000 })
                require(slots.all { it / 100 < dat.int(F.MAP_WIDTH_FILE_OFFSET) && it % 100 < dat.int(F.MAP_HEIGHT_FILE_OFFSET) }) { "Brush cells must be inside active map bounds" }
                if (command.terrain in setOf(752, 755) || command.overlay in 728..739) require(slots.all { dat.u16(F.OCCUPANCY_RECORDS_FILE_OFFSET + 2 * it) == 65535 }) { "A site cannot overlap an entity" }
                if (command.terrain in setOf(707, 711, 752, 755) || command.overlay in 728..739) require(slots.all { dat.u8(F.TRAP_MASK_RECORDS_FILE_OFFSET + it) == 0 }) { "A site cannot overlap a trap" }
                slots.forEach { slot ->
                    val p = F.TILE_RECORDS_FILE_OFFSET + slot * 14
                    val oldTerrain = dat.u16(p); val oldOverlay = dat.u16(p + 2)
                    command.terrain?.let { dat.put(p, 2, it) }; command.overlay?.let { dat.put(p + 2, 2, it) }; command.road?.let { dat.put(p + 4, 2, it) }
                    // Tile +6/+10 are transient path-search costs/flags; any surface edit invalidates them.
                    dat.fill(0, p + 6, p + 14)
                    val terrain = dat.u16(p); val overlay = dat.u16(p + 2)
                    for ((kind, old, new) in listOf(Triple("swiatynia", oldOverlay in 728..739, overlay in 728..739),
                        Triple("skarb", oldTerrain in setOf(752, 755), terrain in setOf(752, 755)),
                        Triple("zamek_place", oldTerrain in setOf(707, 711), terrain in setOf(707, 711)))) {
                        if (old != new) updateRules { it.site(kind, slot / 100, slot % 100, new) }
                    }
                }
                if (slots.isNotEmpty()) for (army in 0 until 500) if (armyActive(dat, army)) dat.fill(0, armyOffset(army) + 316, armyOffset(army) + 720)
                slots.firstOrNull()?.let { RecordId(RecordKind.TILE, it) }
            }
            is EditCommand.SetTrap -> {
                require(command.slot in 0 until 10000); requireTrapOwners(command.ownerMask)
                require(command.slot / 100 < dat.int(F.MAP_WIDTH_FILE_OFFSET) && command.slot % 100 < dat.int(F.MAP_HEIGHT_FILE_OFFSET)) { "Trap must be inside active map bounds" }
                if (command.ownerMask != 0) require(dat.u16(F.OCCUPANCY_RECORDS_FILE_OFFSET + 2 * command.slot) == 65535) { "A trap cannot overlap an entity" }
                if (command.ownerMask != 0) requireTrapGround(command.slot)
                updateRules { it.site("pulapka", command.slot / 100, command.slot % 100, command.ownerMask != 0) }
                dat[F.TRAP_MASK_RECORDS_FILE_OFFSET + command.slot] = command.ownerMask.toByte(); RecordId(RecordKind.TILE, command.slot)
            }
            is EditCommand.SetTraps -> {
                require(command.slots.all { it in 0 until 10000 }); requireTrapOwners(command.ownerMask)
                require(command.slots.all { it / 100 < dat.int(F.MAP_WIDTH_FILE_OFFSET) && it % 100 < dat.int(F.MAP_HEIGHT_FILE_OFFSET) }) { "Trap cells must be inside active map bounds" }
                if (command.ownerMask != 0) require(command.slots.all { dat.u16(F.OCCUPANCY_RECORDS_FILE_OFFSET + 2 * it) == 65535 }) { "A trap cannot overlap an entity" }
                if (command.ownerMask != 0) command.slots.forEach(::requireTrapGround)
                var rules = rules()
                command.slots.distinct().forEach { slot ->
                    rules = rules.site("pulapka", slot / 100, slot % 100, command.ownerMask != 0)
                    dat[F.TRAP_MASK_RECORDS_FILE_OFFSET + slot] = command.ownerMask.toByte()
                }
                fac = rules.toByteArray(); command.slots.firstOrNull()?.let { RecordId(RecordKind.TILE, it) }
            }
        }
        private fun requireTrapOwners(mask: Int) {
            require(mask in 0..31) { "Trap owner mask uses five player bits" }
            val activeMask = (0..4).filter { dat.int(playerOffset(it)) != 0 }.fold(0) { value, slot -> value or (1 shl slot) }
            require(mask and activeMask.inv() == 0) { "Trap owner mask must reference active players only" }
        }
        private fun requireTrapGround(slot: Int) {
            val p = F.TILE_RECORDS_FILE_OFFSET + slot * 14
            require(dat.u16(p) in setOf(0, 4) && dat.u16(p + 2) == 65535) { "Traps require clear ordinary ground, without foundations, temples or treasure" }
        }
    }

    private fun ByteArray.text(offset: Int, length: Int): String {
        val end = (offset until offset + length).firstOrNull { this[it] == 0.toByte() } ?: offset + length
        return copyOfRange(offset, end).toString(Charset.forName(encoding))
    }
    private fun ByteArray.writeText(offset: Int, length: Int, value: String) {
        require('\u0000' !in value) { "Names cannot contain NUL" }
        val buffer = Charset.forName(encoding).newEncoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).encode(CharBuffer.wrap(value))
        require(buffer.remaining() < length) { "Text needs a NUL terminator and must fit in ${length - 1} bytes" }
        fill(0, offset, offset + length); buffer.get(this, offset, buffer.remaining())
    }

    companion object {
        fun fromDat(dat: ByteArray, fac: ByteArray? = null, encoding: String = "windows-1250"): SaveDocument {
            F.requireValidDatSize(dat.size); Charset.forName(encoding)
            return SaveDocument(dat, fac, encoding)
        }
        fun newScenario(name: String = "New scenario"): SaveDocument {
            val b = ByteArray(F.DAT_SIZE)
            repeat(10000) { slot -> val p = 16 + slot * 14; b.put(p + 2, 2, 65535); b.put(p + 4, 2, 65535); b.put(F.OCCUPANCY_RECORDS_FILE_OFFSET + 2 * slot, 2, 65535) }
            b.put(F.MAP_WIDTH_FILE_OFFSET, 4, 100); b.put(F.MAP_HEIGHT_FILE_OFFSET, 4, 100)
            b.put(F.ACTIVE_MISSION_FILE_OFFSET, 4, -1); b.put(F.GAME_TURN_COUNTER_FILE_OFFSET, 2, 1)
            for (player in 0..4) {
                val p = playerOffset(player)
                b.put(p + 27, 4, 1); b.put(p + 35, 4, 1); b.put(p + 39, 4, 1); b.put(p + 43, 4, -1)
                b[p + 47] = 1; b[p + 48] = 1; repeat(10) { b[p + 1357 + 6 * it] = (-1).toByte() }
            }
            b.put(F.UNKNOWN_PRE_ARMY_GAP_FILE_OFFSET, 4, 1)
            repeat(500) { a -> repeat(10) { b.put(armyOffset(a) + 6 + 31 * it, 2, -1) } }
            repeat(100) { c -> val p = buildingOffset(c); b[p + 4] = (-1).toByte(); b.put(p + 16, 2, -1); repeat(12) { b.put(p + 18 + 31 * it, 2, -1) } }
            b.put(F.PORT_ROW_FILE_OFFSET, 4, -1); b.put(F.PORT_COLUMN_FILE_OFFSET, 4, -1)
            return SaveDocument(b, FacDocument.emptyScenario().toByteArray(), "windows-1250").apply {
                state.dat.writeText(0, 16, name)
                repeat(5) { state.dat.writeText(playerOffset(it) + 4, 11, "Player ${it + 1}") }
                saved = null
            }
        }
        private fun same(a: State, b: State) = a.encoding == b.encoding && a.dat.contentEquals(b.dat) &&
            (if (a.fac == null) b.fac == null else b.fac != null && a.fac.contentEquals(b.fac))
        private fun playerOffset(slot: Int) = F.PLAYER_RECORDS_FILE_OFFSET + slot * F.PLAYER_RECORD_SIZE
        private fun armyOffset(slot: Int) = F.ARMY_RECORDS_FILE_OFFSET + slot * F.ARMY_RECORD_SIZE
        private fun buildingOffset(slot: Int) = F.BUILDING_RECORDS_FILE_OFFSET + slot * F.BUILDING_RECORD_SIZE
        private fun armyActive(b: ByteArray, slot: Int) = (0 until 10).any { b.signed16(armyOffset(slot) + 6 + 31 * it) != -1 }
        private fun buildingActive(b: ByteArray, slot: Int) = b.u8(buildingOffset(slot) + 4) in 0..3 && b.signed16(buildingOffset(slot) + 16) != -1
        private fun occupant(id: RecordId) = if (id.kind == RecordKind.ARMY) id.slot else 0x8000 + id.slot

        fun byteOffset(id: RecordId): Int {
            val count = when (id.kind) { RecordKind.SAVE, RecordKind.OPTIONS -> 1; RecordKind.TILE -> 10000; RecordKind.PLAYER -> 5; RecordKind.ARMY, RecordKind.ARMY_UNIT -> 500; else -> 100 }
            require(id.slot in 0 until count) { "Invalid physical ${id.kind} slot ${id.slot}" }
            return when (id.kind) {
                RecordKind.SAVE -> 0
                RecordKind.OPTIONS -> F.UNKNOWN_PRE_ARMY_GAP_FILE_OFFSET
                RecordKind.TILE -> F.TILE_RECORDS_FILE_OFFSET + id.slot * 14
                RecordKind.PLAYER -> playerOffset(id.slot)
                RecordKind.ARMY -> armyOffset(id.slot)
                RecordKind.BUILDING -> buildingOffset(id.slot)
                RecordKind.ARMY_UNIT, RecordKind.BUILDING_UNIT -> {
                    val army = id.kind == RecordKind.ARMY_UNIT; val unit = requireNotNull(id.unitSlot)
                    require(unit in 0 until if (army) 10 else 12)
                    (if (army) armyOffset(id.slot) + 6 else buildingOffset(id.slot) + 18) + unit * 31
                }
            }
        }
        fun recordSize(id: RecordId) = when (id.kind) {
            RecordKind.SAVE -> F.DAT_SIZE; RecordKind.OPTIONS -> 27; RecordKind.TILE -> 14; RecordKind.PLAYER -> 1423
            RecordKind.ARMY -> 725; RecordKind.BUILDING -> 467; else -> 31
        }
        private fun unitRange(id: RecordId) = when (id.kind) {
            RecordKind.ARMY -> byteOffset(id) + 6 to 10
            RecordKind.BUILDING -> byteOffset(id) + 18 to 12
            else -> error("Select an army or building")
        }
        private fun units(b: ByteArray, id: RecordId): List<UnitSnapshot> {
            val (base, count) = unitRange(id)
            return (0 until count).mapNotNull { slot -> val p = base + slot * 31; val type = b.signed16(p)
                if (type !in 0..40) null else UnitSnapshot(RecordId(if (id.kind == RecordKind.ARMY) RecordKind.ARMY_UNIT else RecordKind.BUILDING_UNIT, id.slot, slot),
                    type, b.u8(p + 9), b.u8(p + 11), b.u8(p + 8))
            }
        }
        private fun resolve(save: Save, id: RecordId): ClashObject {
            byteOffset(id)
            return when (id.kind) {
                RecordKind.SAVE -> save; RecordKind.OPTIONS -> save.options; RecordKind.TILE -> save.physicalSlots("tiles")[id.slot]; RecordKind.PLAYER -> save.physicalSlots("players")[id.slot]
                RecordKind.ARMY -> save.armySlots[id.slot]; RecordKind.BUILDING -> save.buildingSlots[id.slot]
                RecordKind.ARMY_UNIT -> save.armySlots[id.slot].unitSlots[id.unitSlot!!]
                RecordKind.BUILDING_UNIT -> save.buildingSlots[id.slot].unitSlots[id.unitSlot!!]
            }
        }
        private fun paired(id: RecordId, name: String) = !PropertyPolicy.canEditIndependently(id.kind, name)
        // Source: g_UnitTypeRuntimeCoreMetadata, 0040F420 UnitSlot_InitFromType. Types 35–40 have no recovered metadata.
        private val actionPoints = intArrayOf(24,20,20,24,22,36,32,30,32,24,20,24,20,20,16,24,26,26,18,20,26,22,26,22,40,24,34,30,24,32,36,30,30,36,36)
        private fun initUnit(b: ByteArray, p: Int, type: Int, owner: Int) {
            require(type in actionPoints.indices) { "Unit creation supports recovered types 0–34" }
            b.fill(0, p, p + 31); b.put(p, 2, type); b[p + 2] = owner.toByte(); b[p + 8] = actionPoints[type].toByte()
            b[p + 9] = 100; b[p + 11] = (if (type in 18..30) 6 else 10).toByte()
        }
        private fun reveal(b: ByteArray, row: Int, column: Int, owner: Int) {
            if (owner !in 0..4) return
            for (r in (row - 3).coerceAtLeast(0)..(row + 3).coerceAtMost(99)) for (c in (column - 3).coerceAtLeast(0)..(column + 3).coerceAtMost(99)) {
                val p = playerOffset(owner) + 57 + r * 13 + c / 8
                b[p] = (b.u8(p) or (1 shl (c % 8))).toByte()
            }
        }
    }
}

internal fun ByteArray.u8(p: Int) = this[p].toInt() and 255
internal fun ByteArray.u16(p: Int) = u8(p) or (u8(p + 1) shl 8)
internal fun ByteArray.signed16(p: Int) = u16(p).toShort().toInt()
internal fun ByteArray.int(p: Int) = u16(p) or (u16(p + 2) shl 16)
internal fun ByteArray.put(p: Int, length: Int, value: Int) { repeat(length) { this[p + it] = (value ushr (8 * it)).toByte() } }
