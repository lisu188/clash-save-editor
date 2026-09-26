# Invariants and validity rules

## Binary layout and identity

- DAT is exactly 586414 bytes, with fixed region offsets and record sizes.
- Terrain and both auxiliary map layers use a fixed 100-cell row stride, independent of visible bounds.
- Raw `MAP_WIDTH` bounds rows; raw `MAP_HEIGHT` bounds columns. UI snapshots transpose their names into conventional screen dimensions.
- Physical table views retain all 500 armies, 100 buildings, and all unit slots. Filtered lists never change a record's physical identity.
- Army/building table scanning continues through inactive holes. Packed unit lists stop at signed type `-1`; unexpected later data remains available through physical views.
- A building is active when its signed type is 0–3 and construction work is not `-1`. An army with any nonempty physical unit slot remains inspectable, including malformed packed sequences.

## Reads and explicit writes

- Decoding, browsing, and no-op serialization never normalize stored bytes.
- Bounded scalar writes validate before applying any bytes. Out-of-range numbers and overlong/unrepresentable strings are rejected rather than truncated.
- Changed strings are encoded using the document charset and zero-pad their field; unchanged strings keep all original bytes, including bytes after NUL.
- Masked edits preserve all other bits. Diagnostic combined unit state fields and transient fact handles are read-only in the structured interface.
- `bytes` and `toByteArray()` are defensive snapshots; normal UI state is held by one authoritative `SaveDocument`.

## Structural commands

A command commits DAT and FAC together or leaves both unchanged. Creation uses
free physical slots without overwriting unexpected records. Malformed packed
sequences block structural commands. Supported creation is limited to recovered
unit types 0–34 and building types 0–2; type 3 remains inspectable.

Moves, ownership changes, deletion, cloning, and unit composition maintain the
relevant occupancy footprints, ownership references, queued paths, garrison
service bytes, and supported FAC dependencies. Terrain/road/site painting uses
recovered presets, active map bounds, and cache/path invalidation. Brush strokes
commit as one undoable transaction. Unknown FAC dependencies and campaign logic
block structural operations whose consequences are not supported.

## Drafts, validation, and UI

An unfinished scenario is a valid project even if it cannot be exported as a
playable save. Playable export requires at least two active players, at least one
human, qualifying assets for each active player, consistent ownership/occupancy,
legal bounds, valid unit state, and matching supported free-game facts. A type-0
building qualifies when it has a garrison; cargo types 31/32 store quantity rather
than health percentage in their shared byte.

Map/table selection uses `RecordId` and remains stable after filtering. Inspector
edits use shared descriptors and policy; the byte panel is diagnostic. Advanced
MCP byte writes can bypass semantic invariants and are not equivalent to a
validated command. Automated validity is separate from original-game acceptance.
