# Save format (consolidated)

## Top-level file layout
| Section | Offset | Count | Entry size | Confidence |
|---|---:|---:|---:|---|
| Save name | 0 | 1 | 16 | High |
| Tiles | 16 | 10000 | 14 | High |
| Players | 140040 | 5 | 1423 | High |
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
- `isActive` at +0, length 4 (High)
- `displayName` at +4, length 11 (High)
- `cameraLeft` at +15, length 4 (High)
- `cameraTop` at +19, length 4 (High)
- `minimapVisibleFlag` at +23, length 4 (High)
- `controllerMode` at +27, length 4 (High)
- `religionFlag` at +39, length 4 (High)
- `techLevel` at +47, low 3 bits (High)
- `lastReportedTechLevel` at +48, low 3 bits (High)
- `battleActionTakenFlag` at +49, length 4 (High)
- `consecutiveIdleBattleTurns` at +53, length 4 (High)
- `revealedTilesBitset` at +57, length 1300 (High)

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
- `typeId` +0 (High for occupancy marker)
- `ownerPlayerIndex` +2 (High)
- `currentActionPoints` +8 (High)
- `currentHealthPercent` +9 (High)
- `fatigue` +10 (High)
- `morale` +11 (High)
- `stanceBits` +12 (Medium/High)
- `stateFlags` +13 (Medium/High)

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
- `addonTypeIds` +402, length 12 (High)
- `selectedAddonSlotIndex` +414 (High)
- `castleAddonFlags` +416 (High)
- `constructionLockFlags` +420 (High)
- `wallStrength` +421 (High)
- `upgradeTimerTurns` +429 (High)
- `peasantCount` +430, low 12 bits (High)
- `satisfaction` +434 (High)
- `plagueState` +435, low 3 bits (High)
- `taxRate` +436, low 6 bits (High)
- `storedMoney` +438, length 4 (High)
- `techLevelBits` +444, low 3 bits (High)
- `castleFactId` +463, length 4 (High)

Remaining bytes unknown/reserved (Low).
