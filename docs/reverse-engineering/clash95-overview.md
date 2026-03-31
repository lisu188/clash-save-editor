# clash95.c reverse-engineering overview

## Scope of this repository
This project currently targets the binary save file layout represented by `Save` and nested structures (`Tile`, `Player`, `Army`, `Unit`, `Castle`). The layout is implemented as fixed offsets and sizes in Kotlin annotations. No dynamic schema discovery is used.

## Confidence legend
- **High**: backed by stable offsets in code and repeatedly used behavior.
- **Medium**: likely correct, but semantic meaning is only partially validated.
- **Low**: retained as unknown/reserved bytes; no trusted semantics.

## Current high-confidence anchors
- Save header `name` at bytes `0..15` (**High**).
- `tiles` block starts at offset `16`, `10000` entries, `14` bytes each (**High**).
- `players` block starts at offset `140040`, `5` entries, `1423` bytes each (**High**).
- `armies` block starts at offset `147190`, `500` entries, `725` bytes each (**High**).
- `castles` block starts at offset `509690`, `10` entries, `467` bytes each (**High**).

## Current high-confidence symbols from decompilation
`clash95.c` symbol naming strongly suggests castle building flags:
- hospital
- barracks
- workshop
- school
- smiths

These are represented as bit flags in `Castle.castleAddonFlags` and exposed via helper methods (`hasBuilding`, `buildingNames`).

The current code also treats recovered multi-byte integers as little-endian and preserves unrelated bits for masked fields such as `Player.techLevel`, `Castle.peasantCount`, `Castle.plagueState`, `Castle.taxRate`, and `Castle.techLevelBits`.

## Boundaries
This consolidation pass intentionally does **not** infer additional semantics for unknown bytes inside each fixed-size record. Unknown bytes remain preserved.
