# Recovered save-format status

## Supported DAT envelope

- Exact DAT size: `586414` bytes.
- Save label: 16 bytes.
- Raw `gameData` image: `0x8F29E` bytes.
- Little-endian recovered numeric fields.
- Exact-size validation in the GUI parser and MCP tools.
- Unknown bytes are preserved during structured edits.

## Supported fixed regions

- 10000 map tiles, 14 bytes each.
- Complete world/session header, including map theme, signed mission index, failure flag, and turn counter.
- Five player records, including AI intelligence, fog-of-war, prisoner queue, and signed queen state.
- All 500 army records, including sparse records after empty table entries.
- All 100 building records, including sparse records after unused/destroyed entries.
- 10000 occupancy cells with empty, army-index, and biased building-index decoding.
- 10000 trap owner-mask cells.
- Six-dword port runtime state.

## Corrected semantics

- `activeMissionIndex` begins at DAT offset `140033`; offset `140032` is the one-byte map theme.
- The building table contains 100 records, not 10.
- Building `+402..+413` stores unit production-licence type IDs, not castle add-on IDs.
- Building display names occupy 11 bytes at `+5..+15`.
- Building construction state is the signed word at `+16`.
- Building `+415` stores production turns remaining.
- Empty army/building records do not terminate scanning of their fixed tables.
- Occupancy building references are encoded as `0x8000 + buildingIndex`.

## Compatibility retained

- The historical `Castle` class and `save.castles` property remain available.
- Deprecated `addonTypeIds`, `selectedAddonSlotIndex`, `addonSlots()`, and `addonTypeNames()` aliases remain for source compatibility but now resolve through production-licence semantics.

## FAC sidecar

The matching `.fac` file is not embedded in the DAT. It contains CLIPS text facts and remains outside the editor's structured DAT writes. A faithful save backup must keep the DAT and FAC files together.

## Deliberately unresolved

- Unknown bytes inside tile, player, unit, army, and building records.
- Internal fields of six-byte prisoner records.
- The final four bytes of each army record.
- Exact gameplay names for some packed state bits.

## Safety limits

- No checksum is calculated because the recovered writer and loader do not use one.
- Serialized CLIPS fact handles and transient runtime dwords are not stable identifiers.
- Raw-byte editing remains inherently unsafe.
- Real-save fixture coverage is still required in addition to synthetic regression tests.
