# Clash Studio documentation

[Project README](../README.md) · [Clash projects overview](https://github.com/lisu188/clash-disassembly/blob/main/docs/CLASH_PROJECTS.md)

## Using and developing the editor

| Document | Use it for |
| --- | --- |
| [Getting started](getting-started.md) | Toolchain, build/test/run commands, Windows distribution and the workspace workflow |
| [Workspace guide](studio-ui.md) | Current navigation, map editing, property dialogs and UI validation evidence |
| [MCP interface](mcp.md) | Headless startup, tool discovery, physical selectors and DAT write behavior |
| [Developer guide](reverse-engineering/developer-guide.md) | Module ownership and changes to fields, commands and evidence |
| [Implemented scope and status](reverse-engineering/final-status.md) | Current capabilities, repository baseline and coverage limits |

## Binary contract and evidence

| Document | Use it for |
| --- | --- |
| [Save-slot format](reverse-engineering/save-format.md) | Canonical offsets, record counts, packed fields and DAT/FAC semantics |
| [Pinned disassembly evidence](reverse-engineering/clash-disassembly-evidence.md) | Source revision, recovered routine anchors and confidence |
| [Invariants](reverse-engineering/invariants.md) | Byte preservation, physical identity, coupled edits and export requirements |
| [Safety and integrity](reverse-engineering/safety-and-integrity.md) | Backups, interrupted pair recovery and limits of raw writes |
| [Unknown fields](reverse-engineering/unknown-fields.md) | Unresolved spans and the evidence required to name them |

The editor's evidence pin is independent of the latest `clash-disassembly`
branch. Advancing either repository does not automatically revalidate the
editor's schema. Keep offsets and counts in the format reference; keep build
commands in Getting started and API details in the MCP interface.

## Recorded validation

| Record | What it establishes |
| --- | --- |
| [Compose modernization validation](modernization-validation.md) | Dated migration tests, package smoke checks and application captures |
| [Workspace redesign validation](studio-ui.md#verification) | Dated interaction/layout tests and Compose scene captures, with native walkthrough limits |
| [Original-game acceptance](original-game-acceptance.md) | One documented free-game route in the unmodified retail executable, with hashes and local evidence inventory |
| [Legacy clash95 overview](reverse-engineering/clash95-overview.md) | Compatibility entry point into the maintained format and evidence references |

Validation records are historical evidence, not continuously refreshed release
certificates. New results should identify the tested revision and route. Keep
incomplete or failed captures and retail-dependent evidence separate from public
synthetic CI. The latest reviewed repository baseline is recorded in
[implemented scope and status](reverse-engineering/final-status.md#reviewed-baseline).

## Related repositories

- [clash-disassembly](https://github.com/lisu188/clash-disassembly): recovered game source and save-layout evidence.
- [clash-hd](https://github.com/lisu188/clash-hd): the separate HD runtime project, with its own implementation and acceptance gates.
- [clash-assets](https://github.com/lisu188/clash-assets): manual transcription and GOG runtime import provenance.

Use the [shared project overview](https://github.com/lisu188/clash-disassembly/blob/main/docs/CLASH_PROJECTS.md)
for cross-project boundaries. An editor test or accepted save does not establish
HD runtime acceptance, and a newer recovered symbol does not by itself change
the editor's supported write policy.
