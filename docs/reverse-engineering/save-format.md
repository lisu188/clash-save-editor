# Clash save-slot format

This document describes `save/N.dat` and the associated `save/N.fac` sidecar. It does not describe `strateg/clash.dat`, which is a separate CLIPS binary construct file.

## File pair

A complete save slot consists of:

```text
save/N.dat
save/N.fac
```

The DAT writer stores a 16-byte label followed by a byte-for-byte dump of the global `gameData` block. The FAC file contains CLIPS facts saved separately by the rules engine.

## DAT envelope

| File offset | Size | Meaning |
|---:|---:|---|
| `0x000000` | `0x10` | Save-slot label |
| `0x000010` | `0x8F29E` | Raw `gameData` image |
| `0x08F2AE` | — | End of file |

The exact DAT size is `586414` bytes. There is no magic value, version field, checksum, compression, section directory, or pointer-relocation table.

```text
DAT file offset = 0x10 + gameData offset
```

All recovered multibyte values are little-endian.

## Top-level payload map

| gameData offset | DAT offset | Size | Region |
|---:|---:|---:|---|
| `0` | `0x000010` | `140000` | 100x100 map tile records, 14 bytes each |
| `140000` | `0x0222F0` | `24` | World/session header |
| `140024` | `0x022308` | `7115` | Five player records, 1423 bytes each |
| `147139` | `0x023ED3` | `8` | Turn owner and viewed player indices |
| `147147` | `0x023EDB` | `27` | Unresolved gap |
| `147174` | `0x023EF6` | `362500` | 500 army records, 725 bytes each |
| `509674` | `0x07C6FA` | `46700` | 100 building records, 467 bytes each |
| `556374` | `0x087D66` | `20000` | 100x100 uint16 occupancy layer |
| `576374` | `0x08CB86` | `10000` | 100x100 trap owner-mask layer |
| `586374` | `0x08F296` | `24` | Port runtime state |

## Map tile record

Each tile occupies 14 bytes.

| Relative offset | Size | Meaning |
|---:|---:|---|
| `+0` | 2 | Terrain tile ID |
| `+2` | 2 | Overlay tile ID |
| `+4` | 2 | Road, bridge, or track tile ID |
| `+6` | 4 | Unresolved transient state, cleared after load |
| `+10` | 4 | Unresolved transient state, cleared after load |

Tile address:

```text
file_offset = 0x10 + 14 * (100 * row + column)
```

## World/session header

| DAT offset | Size | Meaning |
|---:|---:|---|
| `140016` | 4 | Map width |
| `140020` | 4 | Map height |
| `140024` | 4 | Camera left |
| `140028` | 4 | Camera top |
| `140032` | 1 | Map theme index |
| `140033` | 4 | Signed active mission index; `-1` means free/skirmish map |
| `140037` | 1 | Mission failure flag |
| `140038` | 2 | Game turn counter |

## Player record

Five 1423-byte records begin at DAT offset `140040`.

| Relative offset | Size | Meaning |
|---:|---:|---|
| `+0` | 4 | Active flag |
| `+4` | 11 | Display name |
| `+15` | 4 | Saved camera left |
| `+19` | 4 | Saved camera top |
| `+23` | 4 | Minimap-visible flag |
| `+27` | 4 | Controller mode: AI or human |
| `+31` | 4 | AI intelligence tier |
| `+39` | 4 | Religion/alignment flag |
| `+47` | 1 | Technology level |
| `+48` | 1 | Last reported technology level |
| `+49` | 4 | Battle action taken flag |
| `+53` | 4 | Consecutive idle battle turns |
| `+57` | 1300 | Revealed-tile bitset, 13 bytes per map row |
| `+1357` | 60 | Ten unresolved six-byte prisoner-transfer entries |
| `+1419` | 1 | Signed queen relationship state; `-1` is meaningful |
| `+1420` | 1 | Queen portrait index |
| `+1421` | 2 | Next queen relationship-check turn |

Fog bit address within the 1300-byte field:

```text
byte_index = 13 * row + (column >> 3)
bit_mask = 1 << (column & 7)
```

## Army record

The fixed table contains 500 records at DAT offset `147190`, each 725 bytes.

| Relative offset | Size | Meaning |
|---:|---:|---|
| `+0` | 2 | Signed tile row |
| `+2` | 2 | Signed tile column |
| `+4` | 1 | Owner player index |
| `+5` | 1 | Facing direction |
| `+6` | 310 | Ten 31-byte unit slots |
| `+316` | 404 | Queued path buffer |
| `+720` | 1 | Hidden-on-world-map flag |
| `+721` | 4 | Unresolved tail |

An army is active when it contains at least one unit slot whose signed type ID is not `-1`. Empty records may occur before later active records, so the table must be scanned completely rather than stopped at the first empty record.

### Unit slot

| Relative offset | Size | Meaning |
|---:|---:|---|
| `+0` | 2 | Signed unit type ID; `-1` means empty |
| `+2` | 1 | Owner player index |
| `+8` | 1 | Current action points |
| `+9` | 1 | Health percentage, or cargo quantity for types 31 and 32 |
| `+10` | 1 | Fatigue |
| `+11` | 1 | Morale |
| `+12` | 1 | Packed stance/status/order/volley bits |
| `+13` | 1 | Runtime state flags |
| `+18` | 4 | Transient auxiliary runtime state, cleared after load |
| `+22` | 1 | Secondary state bits |

Known state flags at `+13`:

| Mask | Meaning |
|---:|---|
| `0x01` | Ready for stack-turn execution |
| `0x02` | Turn spent; fatigue recovery suppressed |
| `0x04` | Low-morale refusal |
| `0x08` | Plague |

### Queued path buffer

| Relative offset | Size | Meaning |
|---:|---:|---|
| `+0` | 4 | Signed waypoint count |
| `+4` | 400 | Up to 100 four-byte waypoints |

Each waypoint stores `uint8 row`, `uint8 column`, and `uint16 cumulativeCost`.

## Building record

The fixed table contains 100 records at DAT offset `509690`, each 467 bytes. The editor retains the historical `Castle` class name for API compatibility.

| Relative offset | Size | Meaning |
|---:|---:|---|
| `+0` | 1 | Anchor row |
| `+1` | 1 | Anchor column |
| `+2` | 1 | Owner player index |
| `+3` | 1 | Appearance/variant byte |
| `+4` | 1 | Signed footprint class |
| `+5` | 11 | Display name |
| `+16` | 2 | Signed construction turns; `-1` unused/destroyed, `0` complete |
| `+18` | 372 | Twelve 31-byte garrison slots |
| `+390` | 12 | Packed training and repair state |
| `+402` | 12 | Signed unit production-licence type IDs; `-1` means empty |
| `+414` | 1 | Signed active production-licence slot index |
| `+415` | 1 | Production turns remaining |
| `+416` | 1 | Castle add-on flags |
| `+420` | 1 | Construction lock flags |
| `+421` | 1 | Wall strength |
| `+429` | 1 | Upgrade timer |
| `+430` | 2 | Peasant count in low 12 bits |
| `+434` | 1 | Satisfaction |
| `+435` | 1 | Plague state in low three bits |
| `+436` | 1 | Tax rate in low six bits |
| `+438` | 4 | Stored money |
| `+444` | 1 | Technology level in low three bits |
| `+445` | 18 | Three unresolved six-byte prisoner slots |
| `+463` | 4 | Transient CLIPS castle fact handle |

Castle add-on flags at `+416`:

| Mask | Add-on |
|---:|---|
| `0x01` | Hospital |
| `0x02` | Barracks |
| `0x04` | Workshop |
| `0x08` | School |
| `0x10` | Smiths |

The bytes at `+402` are production licences indexed by unit type. They are not castle add-on IDs.

## Occupancy layer

The layer begins at DAT offset `556390` and contains 10,000 little-endian uint16 cells.

```text
file_offset = 556390 + 200 * row + 2 * column
```

| Value | Meaning |
|---:|---|
| `0xFFFF` | Empty tile |
| `0x0000..0x7FFF` | Army table index |
| `0x8000..0xFFFE` | Building table index plus `0x8000` |

## Trap owner-mask layer

The layer begins at DAT offset `576390` and contains one byte per tile.

```text
file_offset = 576390 + 100 * row + column
```

Bit `N` records the trap owner or trap knowledge for player `N`. The byte is cleared when the trap triggers.

## Port state

| DAT offset | Size | Meaning |
|---:|---:|---|
| `586390` | 4 | Signed port row; `-1` means no port |
| `586394` | 4 | Signed port column |
| `586398` | 4 | Next reinforcement turn |
| `586402` | 4 | Reinforcement-ready flag |
| `586406` | 4 | Pending reinforcement unit count |
| `586410` | 4 | Shoreline visual-variant flag |

## FAC sidecar and load reconstruction

The `.fac` file is CLIPS text and is required for a faithful restore. On load, the game resets the rules engine, recreates army and castle facts from DAT records, clears transient unit and tile state, loads the FAC facts, and rebuilds rendering/minimap resources.

Editor rules:

- Preserve every unresolved byte.
- Keep DAT output exactly `586414` bytes.
- Copy or back up the matching FAC file with the DAT file.
- Do not treat serialized fact handles or transient runtime dwords as stable identifiers.
- Do not infer format compatibility from file existence; the format has no version marker.
