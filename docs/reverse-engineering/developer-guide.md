# Developer guide for reverse-engineering integration

[Documentation index](../README.md) · [Build, run and test](../getting-started.md)

## Modules and sources of truth

- `core` owns byte-backed record views, field descriptors, the transactional `SaveDocument`, lossless FAC framing, `.clashproj` storage, and DAT/FAC pair export.
- `desktop` owns the Compose workspace and Canvas. Views consume immutable document snapshots and select entities by physical `RecordId` values.
- `mcp` owns the existing seven tools and JSON-RPC stdio protocol. Legacy object paths retain their filtered-index meaning; the optional `record` selector addresses physical slots.

The external reference is `clash-disassembly` commit `c9c0fa7`, especially
`data/save_dat_layout.json` and the recovered routines cited in
[the evidence notes](clash-disassembly-evidence.md). Kotlin annotations define
the implemented byte windows and masks. `ClashFieldEvidence` carries exceptions
to the default confidence/source, and MCP schema responses expose that metadata.

## Working on fields and commands

1. Verify the offset, signedness, packed mask, sentinel, and behavior against the pinned evidence. Preserve unknown portions and avoid promoting a guessed name.
2. Add or adjust the record field and descriptor metadata. Decoding must remain read-only; scalar delegates validate the complete replacement before applying a bounded patch. Keep runtime handles and combined diagnostic state bytes read-only.
3. Route coupled edits through an `EditCommand`, including its DAT, occupancy, FAC, path, and ownership dependencies. Update `PropertyPolicy` so the inspector and MCP cannot bypass the command with an independent scalar write.
4. Test byte preservation, boundary rejection, sparse/packed distinctions, and command rollback. Update the format, invariants, and evidence documents together.

`Save.parse(bytes, charset)` creates a detached record view; `toByteArray()` and
`bytes` return defensive snapshots. Do not turn these views into a second UI
state store. `SaveDocument.execute` copies the current DAT/FAC state, applies a
command, and commits both only after the command succeeds. Undo/redo operates on
those complete states. Advanced raw-byte tooling is separate from validated
structured editing.

## Storage and display conventions

On-disk terrain, occupancy, and traps always use `100 * row + column`. The
recovered `MAP_WIDTH` field is the row count and `MAP_HEIGHT` is the column
count. Historical scalar property names preserve that binary mapping; desktop
snapshots adapt it to conventional screen width and height.

Army/building IDs are their physical table slots. Sparse inactive entries do
not terminate those tables. Unit arrays are packed sequences ending at signed
`-1`; physical unit views retain every slot for diagnosis. Allocation and
structural editing must preserve malformed records rather than quietly reuse
or compact away unexpected data.

## Build and verification

Use [Getting started](../getting-started.md) for the canonical toolchain,
build/test/run commands and Windows package smoke test. The
[MCP interface](../mcp.md) documents the stdio launch and compatibility contract.

The pinned Kotlin/compiler versions match as required by the
[Compose compatibility guide](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html).
Packaging follows the [native distribution workflow](https://kotlinlang.org/docs/multiplatform/compose-native-distribution.html),
with an explicit JDK 21 toolchain for both the runtime image and its console launcher.

Public tests use synthetic fixtures. Packaged application screenshots, original
game loading/actions/save-and-reload, and automated tests are separate evidence
categories. See [implemented scope and status](final-status.md) for the reviewed
repository baseline, [modernization validation](../modernization-validation.md)
for the dated migration record and [original-game acceptance](../original-game-acceptance.md)
for the exact retail route and its limits. Never infer original-game parity from
a green codec test.
