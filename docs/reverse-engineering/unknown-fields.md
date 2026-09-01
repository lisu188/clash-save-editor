# Unknown and partially understood fields

This document tracks save bytes that remain intentionally neutral after comparison with `clash-disassembly` and controlled save decoding.

## Principles

- Unknown bytes are preserved byte-for-byte.
- Packed fields are edited through masks so unrelated bits survive.
- A plausible UI label is not enough to rename a binary field.
- Decompiler variable names are evidence only when corroborated by reads/writes and behavior.
- `clash-disassembly` source behavior takes precedence over historical names in this editor.

## Remaining opaque unit-slot spans

The 31-byte unit slot is structurally complete, but several spans do not yet have stable gameplay names:

- `+3..+7`;
- `+14..+17` except for individually recovered low-bit behavior in source;
- `+18..+21` is exposed conservatively as `auxRuntimeState` because its subfields remain unresolved;
- `+22` has a known low-bit defensive-state effect but the full byte is not decoded;
- `+23..+30` remain opaque.

For byte `+12`, bit 7 is still unknown. For byte `+13`, only bits `0..3` are named.

## Remaining opaque map-tile bytes

Each 14-byte map tile has high-confidence fields at `+0`, `+2`, and `+4`. Bytes `+6..+13` remain opaque. They must not be rewritten when editing terrain, overlay, or road/bridge ids.

## Remaining opaque building bytes

The 467-byte building record is substantially recovered, but these areas are still intentionally neutral:

- `+417..+419`;
- high bits of several packed economy/state fields;
- byte `+437`: observed/default low-six-bit values exist, but its gameplay meaning is not yet proven strongly enough for a public field name;
- upper two bits of each garrison service-state byte at `+390..+401`.

`+421` remains `wallStrength`: current executable behavior and the `SilaMurow` rules host support that name. It should not be renamed from a single-save correlation.

## Options record caveat

The 27-byte options record is structurally proven. The dwords at offsets `+0`, `+4`, `+8`, and `+12` have observable transition/grid/status/movement-animation behavior. Their names in the editor are behavior-oriented rather than claims about exact localized UI captions.

The final three bytes are manipulated by the original slider code. The recovered code associates them with scroll speed, sound volume, and music volume, while one reused options-application routine also feeds the last byte into palette-brightness logic. Preserve the raw byte when its exact user-facing interpretation matters.

## Runtime rule handles

Army `+721` and building `+463` are cached CLIPS/rules fact handles. Their purpose is known, but their numeric values are runtime-local implementation state. An editor should expose them for inspection and preserve them unless deliberately rebuilding rule state; it should not treat them as stable cross-save identifiers.

## Evidence required for future promotions

A field should move from unknown/partial to a stronger semantic name only with at least one strong source and preferably two independent forms of evidence:

1. direct executable/disassembly reads and writes with a stable behavioral role;
2. reproducible before/after save diffs tied to a controlled in-game action;
3. rules-host/string/UI evidence that independently identifies the same field;
4. multiple real saves that confirm the proposed range, sentinel values, and packed-bit interpretation.
