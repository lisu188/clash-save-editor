# Safety and integrity

## Current safety posture

- The editor preserves and rewrites the complete DAT byte buffer.
- DAT input and output must be exactly `586414` bytes.
- Unknown and unmodeled regions are preserved unless explicitly changed through raw-byte editing.
- Structured edits are bounded to their annotated field windows.
- Masked writes preserve unrelated bits in the same byte window.
- Sparse army and building tables are scanned to their full fixed counts.

## Save-pair limitation

The DAT is only one part of a save slot. The matching FAC file contains CLIPS facts and is not modified by the DAT editor. Back up and move both files together.

## Transient serialized values

The original game writes the full `gameData` image, including values that are rebuilt or cleared after loading:

- map-tile dwords at `+6` and `+10`;
- unit-slot auxiliary dword at `+18`;
- building CLIPS castle fact handle at `+463`.

These values are not stable identifiers and should not be deliberately copied between unrelated saves.

## Integrity behavior

The recovered writer and loader show no magic value, version marker, checksum, compression, or relocation pass. The absence of validation makes malformed files more dangerous, not less. Exact size alone does not prove semantic validity.

## Remaining risks

- Raw-byte editing can corrupt records, indices, and CLIPS relationships.
- Unknown packed bits may share bytes with editable fields.
- Editing DAT state without corresponding FAC facts may create inconsistent rules-engine state.
- A save from an incompatible executable build cannot be identified through an embedded format version.

## Conservative usage guidance

- Work on copies of both DAT and FAC files.
- Prefer structured fields over raw writes.
- Preserve unresolved bytes.
- Do not edit transient handles unless debugging loader behavior.
- Compare output size and binary diff before launching the game.
- Validate important changes against real saves, not only synthetic fixtures.
