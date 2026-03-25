# Unknown and partially understood fields

This document tracks fields that are intentionally kept neutral.

## Principles
- Unknown bytes are preserved.
- Unknown bytes are not assigned speculative names.
- Medium/low confidence fields should not be used to infer gameplay behavior without new evidence.

## Low-confidence names still exposed in code
- `Tile.type3`, `Tile.type4`
- `Castle.appearance`
- `Castle.canBuild`
- `Army.dir`
- `Unit.shout`

These names are historical placeholders and do not imply complete semantic certainty.

## Reserved regions
All bytes not explicitly listed in `save-format.md` as known fields are currently reserved/unknown and should be treated as opaque payload.

## Evidence requirements for future upgrades
A field should move from unknown/partial to high-confidence only when at least one applies:
1. Repeated, stable runtime correlation from original game behavior.
2. Direct symbolic/constant evidence in `clash95.c` plus behavioral confirmation.
3. Reproducible before/after save diffs tied to a single in-game action.
