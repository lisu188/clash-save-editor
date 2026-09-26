# Clash Studio

A Kotlin Compose Desktop editor for Clash saves and free-game scenarios, with a headless MCP server. The map uses procedural graphics; original game artwork is not required.

## Workspace

Open a DAT or `.clashproj`, select a map tile, army, building or player, and edit its properties in the inspector. Map and filtered tables share physical record IDs. Pan, zoom, fit, layer controls and batched brushes help navigate the fixed 100×100 map. Diagnostics, reports and source bytes stay inside the workspace.

- **New** creates an empty grass draft with recovered player/options defaults. Configure players and place starting armies or buildings.
- **Save project** stores unfinished work as a versioned `.clashproj` ZIP containing the manifest, DAT image, optional FAC and document encoding.
- **Export game save** validates the scenario and writes a DAT/FAC pair. Use a separate destination, then copy both files to a numbered `save/N.dat` and `save/N.fac` slot for Clash's **Load Game** menu.
- Undo/redo restores the complete transaction, including occupancy and supported FAC facts.
- Shortcuts: `Ctrl+N` New, `Ctrl+O` Open, `Ctrl+S` Save project, `Ctrl+Shift+S` Export, `Ctrl+Z` Undo, `Ctrl+Y` / `Ctrl+Shift+Z` Redo.

Playable export requires at least two active players, a human player, a qualifying starting force for every active player, valid ownership/footprints, consistent occupancy and supported rules dependencies. Projects can be saved before these requirements are met.

Structural editing is bounded by recovered lifecycle evidence: building types 0–2, known terrain/site/road presets, traps and supported free-game facts. Type 3 is inspectable. Campaign logic and unknown FAC forms remain lossless; structural changes are blocked when their dependencies cannot be interpreted. DAT-only files allow inspection and independent scalar edits. The editor never invents replacement campaign facts.

## Binary compatibility

Clash's disk format is unchanged: **586,414-byte DAT + companion FAC**. No-op decoding/saving preserves every byte, including text, padding, unknown fields and unused slots. The original format has no version marker or checksum. DAT indexing always uses a 100-cell row stride, independently of visible bounds.

Text defaults to Windows-1250 interpretation, recorded in project metadata. Choose the name encoding on the Scenario page; changing the interpretation is undoable and does not transcode existing bytes. The core API also accepts an explicit charset. Unchanged strings retain their original bytes; edits reject unrepresentable or overlong text. Packed-field edits preserve unrelated bits. Source evidence is pinned to `clash-disassembly` revision `c9c0fa7`; see [format and evidence](docs/reverse-engineering/save-format.md).

DAT/FAC export stages both files, keeps uniquely named backups and writes a recovery journal. If interrupted, the application offers restoration of the previous pair before opening it. See [safety and integrity](docs/reverse-engineering/safety-and-integrity.md).

## Build and run

Use **JDK 21**. Gradle 8.14.4, Kotlin/compiler 2.4.20 and Compose 1.12.1 are pinned; compiled bytecode targets JVM 17. IntelliJ form instrumentation is no longer used.

```sh
./gradlew test
./gradlew run
./gradlew :mcp:fatJar
```

Use `gradlew.bat` on Windows. Compose UI tests need a display; on headless Linux use `xvfb-run -a ./gradlew test`. Public CI uses synthetic fixtures and does not need retail assets.

On Windows:

```powershell
.\gradlew.bat :desktop:portableZip
```

The archive is `desktop/build/distributions/ClashSaveEditor-windows-x64.zip`. Extract it and run `ClashSaveEditor/ClashSaveEditor.exe`; the Java runtime is included. `ClashSaveMcp.cmd` runs the headless server using that same bundled runtime. The unarchived application image is under `desktop/build/compose/binaries/main/app`.

## MCP

```sh
./gradlew runMcpServer
# Or after :mcp:fatJar:
java -jar mcp/build/libs/mcp-2.0.0-all.jar
```

The seven tools and JSON-RPC stdio protocol remain available: `save_get_schema`, `save_get_overview`, `save_list_entities`, `save_read_object`, `save_read_bytes`, `save_set_property` and `save_write_bytes`. Existing `objectPath` selectors keep their filtered-index meaning. For stable access use `record`, mutually exclusive with `objectPath`:

```json
{"path":"save/0.dat","record":{"kind":"army","slot":499}}
```

For a unit, add `"unitSlot": 0` to an `army_unit` or `building_unit` selector. `save_get_schema` describes names, ranges, evidence and fields requiring a transaction. Structured writes share core validation; coupled fields cannot be edited independently. MCP raw-byte writes remain explicitly advanced. Writes require `outputPath` unless `inPlace=true`; in-place writes retain the established non-clobbering `.bak`, `.bak.1`, … behavior. DAT-only MCP writes leave FAC untouched.

## Modules and verification

- `core`: byte-preserving record views, recovered schema, transactional document, FAC handling, project and pair IO.
- `desktop`: Compose workspace and Canvas, immutable snapshots and file IO off the UI thread.
- `mcp`: compatible headless tools and subprocess protocol.

See the [developer guide](docs/reverse-engineering/developer-guide.md), [invariants](docs/reverse-engineering/invariants.md) and [delivery validation](docs/modernization-validation.md). Original-game acceptance is recorded separately from automated tests. New Game/Campaign integration, arbitrary objective scripting and original-art loading are outside this editor's scope.
