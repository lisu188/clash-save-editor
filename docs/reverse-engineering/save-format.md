# Save format (consolidated)

## Top-level file layout
| Section | Offset | Count | Entry size | Confidence |
|---|---:|---:|---:|---|
| Save name | 0 | 1 | 16 | High |
| Tiles | 16 | 10000 | 14 | High |
| Players | 140044 | 5 | 1423 | High |
| Armies | 147190 | 500 | 725 | High |
| Castles | 509690 | 10 | 467 | High |

Minimum represented file size: `514360` bytes.

## Structure summaries

### Tile (14 bytes)
Known fields:
- `type1` at +0 (Medium)
- `type2` at +1 (Medium)
- `type3` at +2 (Low)
- `type4` at +3 (Low)

Other bytes are currently unknown/reserved (Low).

### Player (1423 bytes)
Known fields:
- `name` at +0, length 10 (High)
- `explored` at +53, length 1300 (Medium: behavior known, exact semantics partially known)

Other bytes unknown/reserved (Low).

### Army (725 bytes)
Known fields:
- `x` +0 (Medium)
- `y` +2 (Medium)
- `player` +4 (Medium)
- `dir` +5 (Low/Medium)
- `units` +6, count 10, entry size 31 (High for layout)

Remaining bytes unknown/reserved (Low).

### Unit (31 bytes)
Known fields:
- `type` +0 (High for occupancy marker)
- `move` +8 (Medium)
- `health` +9 (Medium)
- `shout` +10 (Low/Medium)
- `morale` +11 (Medium)
- `exp` +12 (Medium)

Remaining bytes unknown/reserved (Low).

### Castle (467 bytes)
Known fields:
- `x` +0 (High)
- `y` +1 (High)
- `player` +2 (High)
- `appearance` +3 (Low/Medium)
- `type` +4 (High for occupancy marker)
- `name` +5, length 10 (High)
- `units` +18, count 12, entry size 31 (High for layout)
- `unitsToBuild` +402, length 12 (Medium)
- `building` +416 (Medium/High for bit-mask use)
- `canBuild` +420 (Low/Medium)
- `walls` +425 (Medium)
- `peasants` +430 (Medium)
- `happiness` +434 (Medium)
- `tax` +436 (Medium)
- `gold` +438 (Medium)

Remaining bytes unknown/reserved (Low).
