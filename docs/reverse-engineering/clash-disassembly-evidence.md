# clash-disassembly evidence notes

These notes summarize high-confidence facts promoted from
`../clash-disassembly/clash.c`.

## Unit record
- Unit creation initializes `currentActionPoints` from the unit-type table, sets health to `100`, sets fatigue to `0`, and clears the known state bits.
- Unit health is clamped to `0..100`, fatigue to `0..100`, and morale to `0..20`.
- Unit byte `+12` low bits `0..1` are the experience level. Values are `0..3`; `3` is the maximum tier.
- Unit byte `+12` bits `2..3` are experience progress. The game increments progress and rolls it into the experience level when progress exceeds its local threshold.
- Unit byte `+13` bit `2` is used by low-morale checks. Positive morale changes clear this bit.
- Unit types `31..34` are skipped by morale and fatigue adjustment helpers.

## Tile record
- Overlay IDs `728..739` are temple/shrine overlays.
- Terrain IDs `752` and `755` are buried treasure tiles. Digging treasure rewrites `752` to `0` and `755` to `4`.

## Castle record
- Castle add-on ids decode as `0` Court, `1` Tower, `2` Hospital, `3` Barracks, `4` Workshop, `5` School, `6` Smiths, `7` Peasants, `8` Barracks, and `255` empty.
- The original castle name pool contains generated names such as `cantown`, `stone bell`, `hopenberg`, `timbran`, and `Keep`. The save stores the actual selected name in the castle record.

## Deferred
- The unit-type table contains more combat and movement fields, but Hex-Rays split the 88-byte records into sparse symbols. The current editor exposes formulas and fields only where the semantics are clear from code use.
- Several unit state bits are copied and tested by combat/pathing code, but only the low-morale flag has been promoted to a named editable property.
