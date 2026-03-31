# Final consolidation status

## Confidently supported now
- Fixed top-level section layout (`Save` name, tiles, players, armies, castles) with stable offsets and sizes.
- Corrected player block base offset to `140040` and expanded `Player` parsing to include activation, camera, controller, religion, tech, battle-activity, and visibility state.
- Nested object extraction for `Tile`, `Player`, `Army`, `Unit`, `Castle`.
- Replaced the old guessed `Unit` runtime fields with recovered `typeId`, `ownerPlayerIndex`, `currentActionPoints`, `currentHealthPercent`, `fatigue`, `morale`, `stanceBits`, and `stateFlags`.
- Expanded `Castle` parsing to include add-on ids, construction lock, wall strength, upgrade timer, masked peasant/tax/plague fields, stored money, tech bits, and fact id.
- Castle building bit flags exposed as: hospital, barracks, workshop, school, smiths.
- Fixed-width integer fields now round-trip as little-endian values, and masked bitfields preserve unrelated bits.
- Save exports now include recovered unit roster names and sprite folders from `clash-disassembly`.
- Conservative field-window serialization for known fields.

## Still uncertain / partial
- Multiple per-structure bytes remain unknown/reserved.
- Several exposed fields remain medium or low confidence (`type3`, `type4`, `dir`, `appearance`, `stanceBits`, `stateFlags`).
- No verified checksum/integrity algorithm is implemented.

## What this consolidation pass cleaned up
- Promoted multiple player, unit, and castle fields from guessed names to recovered names backed by `clash-disassembly`.
- Removed transitional deprecated accessors so the code, UI column names, and docs use one canonical schema.
- Hardened scalar serialization for little-endian integers and masked fields.
- Added targeted regression tests for the recovered player offset, castle money encoding, masked tax preservation, and castle building flag decoding.

## Files changed in this pass
- `build.gradle.kts`
- `src/com/lis/clash/converters.kt`
- `src/com/lis/clash/objects/ClashObject.kt`
- `src/com/lis/clash/objects/Player.kt`
- `src/com/lis/clash/objects/Save.kt`
- `src/com/lis/clash/objects/Unit.kt`
- `src/com/lis/clash/objects/Castle.kt`
- `src/com/lis/clash/parser.kt`
- `src/com/lis/clash/scripts.kt`
- `src/com/lis/clash/types.kt`
- `src/test/kotlin/com/lis/clash/SaveConsolidationTest.kt`
- `docs/reverse-engineering/clash95-overview.md`
- `docs/reverse-engineering/save-format.md`
- `docs/reverse-engineering/invariants.md`
- `docs/reverse-engineering/unknown-fields.md`
- `docs/reverse-engineering/safety-and-integrity.md`
- `docs/reverse-engineering/developer-guide.md`
- `docs/reverse-engineering/final-status.md`

## Intentionally deferred
- Broad schema refactors.
- New semantics for unknown bytes.
- Checksum algorithm implementation without strong evidence.
- UI redesign for confidence-aware edit gating beyond the improved field names.

## Recommended next steps
1. Establish a checksum/integrity evidence track (if present in original game).
2. Add binary fixture-based round-trip tests from real saves.
3. Introduce optional read-only gating for low-confidence fields in UI.
4. Continue promoting fields from low/medium to high confidence only with reproducible evidence.
