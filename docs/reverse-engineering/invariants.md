# Invariants and validity rules

## Structural invariants
- Each section uses fixed offsets and fixed-size entries.
- A section entry is materialized into an object only while `isValid()` remains true for that entry type.

## Entry validity rules in current implementation
- `Unit` is valid when `type != -1`.
- `Army` is valid when it has at least one valid unit.
- `Castle` is valid when `type != -1`.
- `Tile` and `Player` currently have no additional validity guard beyond fixed slicing.

## Serialization invariants
- Editing a known field updates only that field's byte window.
- If new field data is shorter than fixed field length, untouched tail bytes are preserved.
- If new field data is longer than fixed field length, data is truncated to field length.
- Non-edited regions are preserved byte-for-byte.

## UI invariants
- Table columns map directly to annotated simple properties.
- Byte-level editor can modify the currently selected object's bytes directly.

## Explicitly not treated as invariant
- Range checks (e.g., map bounds, player id bounds) are not currently enforced globally.
- Semantic checks for low-confidence fields are intentionally absent.
