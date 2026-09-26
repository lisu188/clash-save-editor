# Clash Studio

A Kotlin Compose Desktop editor for Clash saves and free-game scenarios, with a
headless MCP server. The repository retains the name `clash-save-editor`. The
map uses procedural graphics; original game artwork is not required.

## Start here

- [Getting started](docs/getting-started.md): build, run, Windows packaging and the create/open/export workflow.
- [MCP interface](docs/mcp.md): launch the server, inspect physical records and make bounded DAT edits.
- [Documentation index](docs/README.md): format reference, contributor guidance, implemented scope and validation records.
- [Clash projects overview](https://github.com/lisu188/clash-disassembly/blob/main/docs/CLASH_PROJECTS.md): the save editor, recovered game source, HD runtime and reference assets.

With **JDK 21** installed, run `./gradlew run` from the repository root
(`.\gradlew.bat run` in Windows PowerShell). Choose **Create scenario from scratch**
or **Open existing save**; the workspace opens after a draft is created or a DAT
or `.clashproj` is loaded. Cancelling Open keeps the startup choices visible.

## What the editor supports

The workspace combines a fixed 100×100 map, filtered entity tables and an
inspector using physical record IDs. It supports pan/zoom, layers, batched
brushes, player setup, supported army/building and unit editing, and complete
transaction undo/redo. Draft `.clashproj` files can be saved before a scenario
meets playable-export requirements.

Clash saves retain the original **586,414-byte DAT + companion FAC** format.
No-op decoding/saving preserves every byte, including text, padding, unknown
fields and unused slots. Structural authoring is bounded by recovered free-game
lifecycles: building types 0–2, known terrain/site/road presets, traps and
supported facts. Type 3 is inspectable. Unknown FAC forms and campaign logic
remain lossless and block dependency-sensitive changes. DAT-only inputs support
inspection and independent scalar edits.

Use **Export game save** to validate and write a DAT/FAC pair, then copy both to
a numbered `save/N.dat` and `save/N.fac` slot for the original game's **Load Game**
menu. Export retains backups and a recovery journal. See
[safety and integrity](docs/reverse-engineering/safety-and-integrity.md) and the
[implemented scope](docs/reverse-engineering/final-status.md) for precise limits.

## Verification

Public CI uses synthetic fixtures on Linux and Windows; Windows also builds the
portable distribution and checks its bundled MCP runtime. Automated tests,
packaged application captures and original-game acceptance are separate forms
of evidence. The [original-game acceptance record](docs/original-game-acceptance.md)
covers one two-player free-game fixture through load, human movement, a full turn
cycle and save/reload. It does not establish campaign, AI or combat coverage.

Binary evidence is pinned to `clash-disassembly` revision `c9c0fa7`; see the
[format reference](docs/reverse-engineering/save-format.md). New Game/Campaign
menu integration, arbitrary objective scripting and original-art loading remain
outside this editor's scope.
