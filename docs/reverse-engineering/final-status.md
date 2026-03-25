# Final consolidation status

## Confidently supported now
- Fixed top-level section layout (`Save` name, tiles, players, armies, castles) with stable offsets and sizes.
- Nested object extraction for `Tile`, `Player`, `Army`, `Unit`, `Castle`.
- Castle building bit flags exposed as: hospital, barracks, workshop, school.
- Conservative field-window serialization for known fields.

## Still uncertain / partial
- Multiple per-structure bytes remain unknown/reserved.
- Several exposed fields remain medium or low confidence (`type3`, `type4`, `dir`, `appearance`, `canBuild`, `shout`).
- No verified checksum/integrity algorithm is implemented.

## What this consolidation pass cleaned up
- Added a unified reverse-engineering doc set under `docs/reverse-engineering/`.
- Standardized confidence labeling and terminology across docs.
- Corrected canonical castle field name to `happiness` while preserving deprecated alias `hapiness`.
- Hardened fixed-length field writes to preserve trailing unknown bytes.
- Added targeted regression tests for preservation and castle building flag decoding.

## Files changed in this pass
- `build.gradle.kts`
- `src/com/lis/clash/objects/ClashObject.kt`
- `src/com/lis/clash/objects/Castle.kt`
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
- UI redesign for confidence-aware edit gating.

## Recommended next steps
1. Establish a checksum/integrity evidence track (if present in original game).
2. Add binary fixture-based round-trip tests from real saves.
3. Introduce optional read-only gating for low-confidence fields in UI.
4. Continue promoting fields from low/medium to high confidence only with reproducible evidence.
