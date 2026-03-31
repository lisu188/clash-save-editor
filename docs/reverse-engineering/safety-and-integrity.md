# Safety and integrity

## Current safety posture
- The editor uses in-memory full-file bytes and writes back the full buffer.
- Unknown/non-modeled regions are preserved unless explicitly edited through byte-level table.
- Known-field edits are bounded to annotated offsets and lengths.

## Consolidation hardening applied
- Fixed-length simple field writes now preserve trailing bytes when new value is shorter.
- Overlong values are truncated to field size instead of spilling.
- Fixed-width integer fields now serialize as little-endian values.
- Masked numeric fields preserve unrelated bits in the same byte window.

## Remaining risks
- Byte table permits raw editing and can corrupt data if misused.
- No end-to-end checksum/integrity algorithm is currently implemented (none confidently identified in current codebase).
- Low-confidence fields such as `Tile.type3`, `Tile.type4`, `Army.dir`, and `Castle.appearance` remain only partially understood.

## Conservative usage guidance
- Prefer structure-table edits over raw byte-table edits.
- Treat low-confidence fields as experimental.
- Validate changes with round-trip tests and binary diffs before sharing saves.
