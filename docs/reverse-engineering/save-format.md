# Save format

This document describes Clash `save/N.dat` files. Offsets in this repository are absolute file offsets. The original game stores a 16-byte save label followed by a byte-for-byte image of the 586,398-byte `gameData` allocation.

## Top-level file layout

| Section | File offset | Size | Count / stride | Confidence |
|---|---:|---:|---:|---|
| Save label | 0 | 16 | 1 | High |
| Map tile records | 16 | 140000 | 10000 × 14 | High |
| World/session header | 140016 | 24 | 1 | High |
| Player runtime records | 140040 | 7115 | 5 × 1423 | High |
| Turn/viewed-player indices | 147155 | 8 | 2 × 4 | High |
| Persistent options record | 147163 | 27 | 6 dwords + 3 bytes | High for layout |
| Army stack records | 147190 | 362500 | 500 × 725 | High |
| Building records | 509690 | 46700 | 100 × 467 | High |
| Occupancy index layer | 556390 | 20000 | 10000 × uint16 | High |
| Trap owner-mask layer | 576390 | 10000 | 10000 × uint8 | High |
| Port runtime state | 586390 | 24 | 6 × int32 | High |

Expected DAT size: **586,414 bytes** (`16 + 586398`).

For comparison with `clash-disassembly`, which usually uses offsets relative to `gameData`:

```text
file offset = gameData offset + 16
```

## World/session header

| File offset | Size | Field |
|---:|---:|---|
| 140016 | 4 | `mapWidthTiles` |
| 140020 | 4 | `mapHeightTiles` |
| 140024 | 4 | `mapViewLeft` |
| 140028 | 4 | `mapViewTop` |
| 140032 | 1 | `mapThemeId` |
| 140033 | 4 | `activeMissionIndex` |
| 140037 | 1 | `missionFailureFlag` |
| 140038 | 2 | `turnCounter` |

The old editor incorrectly treated bytes `140032..140035` as a four-byte mission id. The mission id actually begins one byte later; byte `140032` is the map/theme selector.

## Persistent options record

The game copies 24 bytes of its options record into `gameData`, followed by three slider bytes. Campaign transitions preserve all 27 bytes.

| File offset | Size | Meaning |
|---:|---:|---|
| 147163 | 4 | transition/event animation enable flag |
| 147167 | 4 | map grid overlay flag |
| 147171 | 4 | map information/status overlay flag |
| 147175 | 4 | fast/skip movement animation flag |
| 147179 | 4 | music enabled flag |
| 147183 | 4 | sound effects enabled flag |
| 147187 | 1 | scroll-speed raw value |
| 147188 | 1 | sound-volume raw value |
| 147189 | 1 | music/brightness-style signed raw value used by the original options code |

The first four dwords are boolean-style runtime controls. Their names remain intentionally close to observed behavior rather than UI-label speculation.

## Player record — 1423 bytes

Player records begin at file offset `140040`.

Known fields include:

- `+0` `isActive`, int32.
- `+4` `displayName`, 11 bytes.
- `+15` `cameraLeft`, int32.
- `+19` `cameraTop`, int32.
- `+23` `minimapVisibleFlag`, int32.
- `+27` `controllerMode`, int32.
- `+31` AI intelligence, int32; currently not exposed by the editor model.
- `+39` `religionFlag`, int32.
- `+47` `techLevel`, uint8.
- `+48` `lastReportedTechLevel`, uint8.
- `+49` `battleActionTakenFlag`, int32.
- `+53` `consecutiveIdleBattleTurns`, int32.
- `+57` `revealedTilesBitset`, 1300 bytes.
- `+1357` prisoner transfer queue, ten six-byte entries.
- `+1419` `queenRelationshipState`, byte.
- `+1420` `queenPortraitIndex`, byte.
- `+1421` `queenNextRelationshipCheckTurn`, uint16.

## Army stack — 725 bytes

500 records begin at file offset `147190`.

| Relative offset | Size | Field |
|---:|---:|---|
| 0 | 2 | signed tile row |
| 2 | 2 | signed tile column |
| 4 | 1 | owner player index |
| 5 | 1 | facing direction |
| 6 | 310 | ten 31-byte unit slots |
| 316 | 404 | queued path buffer |
| 720 | 1 | hidden-on-world-map flag |
| 721 | 4 | cached CLIPS/rules army-fact handle |

The `+721` dword is not padding. `Rules_CreateArmyFact` stores the asserted fact handle there and later synchronization code reuses it.

### Queued path

- `+316` int32 waypoint count.
- `+320` up to 100 four-byte waypoints.
- Each waypoint is `row:uint8`, `column:uint8`, `cumulativeCost:uint16`.

## Unit slot — 31 bytes

| Relative offset | Field |
|---:|---|
| 0 | signed unit type id; `-1` means empty |
| 2 | owner player index |
| 8 | current action points |
| 9 | current health percentage |
| 10 | fatigue |
| 11 | morale |
| 12 | packed status/order/ranged-fire byte |
| 13 | packed state flags |
| 18 | auxiliary runtime dword |
| 22 | secondary state bits |

Byte `+12` is **not an experience byte**:

- bits `0..1`: `statusLevel`;
- bits `2..3`: `orderState`;
- bits `4..6`: `volleysUsed`;
- bit `7`: unresolved.

The game derives remaining ranged volleys as approximately `statusLevel + 1 - volleysUsed`.

Known byte `+13` flags:

- bit `0x01`: ready/active turn-state flag;
- bit `0x02`: spent-turn flag, which prevents fatigue recovery;
- bit `0x04`: low-morale refusal flag;
- bit `0x08`: plague flag;
- upper bits remain unresolved.

The editor preserves unrelated packed bits when changing any masked field.

## Building record — 467 bytes

100 records begin at file offset `509690`. An unused record has signed building type `-1` at `+4`; destruction also uses `constructionWorkRemaining = -1` at `+16`.

| Relative offset | Size | Field |
|---:|---:|---|
| 0 | 1 | tile row |
| 1 | 1 | tile column |
| 2 | 1 | owner player index |
| 3 | 1 | appearance/visual variant |
| 4 | 1 | signed building type |
| 5 | 11 | display name |
| 16 | 2 | signed construction work remaining; `0` completed, `-1` unused/destroyed |
| 18 | 372 | twelve 31-byte garrison unit slots |
| 390 | 12 | packed garrison service state |
| 402 | 12 | unit-production licence type ids (`-1` empty) |
| 414 | 1 | signed active production licence slot index |
| 415 | 1 | production turns remaining |
| 416 | 1 | castle add-on flags |
| 420 | 1 | staged-construction lock flags |
| 421 | 1 | wall strength |
| 422 | 7 | wall/building section integrity bytes |
| 429 | 1 | staged upgrade timer |
| 430 | 2 | peasant count in low 12 bits |
| 432 | 2 | signed 12-bit population-growth delta plus unrelated high nibble |
| 434 | 1 | signed satisfaction value |
| 435 | 1 | plague state in low 3 bits |
| 436 | 1 | tax rate in low 6 bits |
| 437 | 1 | unresolved packed byte; default low-six-bit value is often 50 |
| 438 | 4 | stored money |
| 442 | 2 | last collected gold income |
| 444 | 1 | technology level in low 3 bits |
| 445 | 18 | three six-byte prisoner slots |
| 463 | 4 | cached CLIPS/rules castle-fact handle |

### Garrison service state

Each byte at `+390..+401` contains two independent countdowns:

- bits `0..2`: training turns remaining;
- bits `3..5`: repair turns remaining;
- bits `6..7`: unresolved.

### Production licences

Bytes `+402..+413` are **unit type ids**, not castle add-on ids. New buildings seed a peasant licence and use `-1` for empty slots. Actual permanent castle add-ons are the separate flags at `+416`:

- `0x01` hospital;
- `0x02` barracks;
- `0x04` workshop;
- `0x08` school;
- `0x10` smiths.

### Prisoner slot — 6 bytes

Each of the three records at `+445` contains:

| Relative offset | Size | Field |
|---:|---:|---|
| 0 | 1 | signed prisoner type id |
| 1 | 1 | captured owner player index |
| 2 | 1 | turns held |
| 3 | 1 | pending action |
| 4 | 2 | ransom value |

## Occupancy index layer

The 20,000-byte region at file offset `556390` is 10,000 little-endian uint16 cells, one per logical 100×100 map tile.

Known encoding:

- `0xFFFF`: empty;
- `0..499`: army stack index;
- `0x8000..0x8063`: building index `value - 0x8000`.

This layer should be used to cross-check object locations after edits. It is independent from the 14-byte visual terrain records.

## Trap owner-mask layer

File offset `576390`, 10,000 bytes. Each byte is a player bitmask associated with the tile's trap state/knowledge. Trap creation sets a player bit; triggering/clearing a trap clears the corresponding byte.

## Port runtime state

Six signed/unsigned runtime dwords at file offset `586390`:

- `+0` port row (`-1` = no port);
- `+4` port column;
- `+8` next reinforcement turn;
- `+12` reinforcement-ready flag;
- `+16` pending reinforcement unit count;
- `+20` shoreline visual-variant flag.

## Editing rules

1. Preserve every byte not explicitly changed.
2. Preserve unrelated bits in packed bytes/words.
3. Do not reinterpret runtime fact handles as portable identifiers; the original loader rebuilds rule facts.
4. Do not resize a save: valid DAT files are exactly 586,414 bytes.
5. A field name is promoted only when source behavior or reproducible save diffs support it.
6. Treat `clash-disassembly` as the authoritative source when this editor's historical names conflict with recovered executable behavior.
