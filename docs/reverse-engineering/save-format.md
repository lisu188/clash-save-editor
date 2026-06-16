# Save format (consolidated)

## Top-level file layout
| Section | Offset | Count | Entry size | Confidence |
|---|---:|---:|---:|---|
| Save name | 0 | 1 | 16 | High |
| Tiles | 16 | 10000 | 14 | High |
| Shared world view state | 140016 | 1 | 24 | High for listed fields |
| Players | 140040 | 5 | 1423 | High |
| Armies | 147190 | 500 | 725 | High |
| Castles | 509690 | 10 | 467 | High |

Minimum represented file size: `514360` bytes.

## Structure summaries

### Tile (14 bytes)
Known fields:
- `terrainTileId` at +0, length 2 (High)
- `overlayTileId` at +2, length 2 (High)
- `roadOrBridgeTileId` at +4, length 2 (High)

Other bytes are currently unknown/reserved (Low).

Known overlay interpretation:
- Overlay IDs `728..739` are temple/shrine tiles according to the original game data checks. The helper exposes `templeVariant` as `(overlayTileId - 728) / 2`.
- Odd temple/shrine overlay IDs are currently exposed as `templeVisitedOrEmpty=true`, based on the game incrementing the overlay after temple entry.
- Terrain IDs `752` and `755` are buried treasure tiles. Digging converts `752` to `0` and `755` to `4`.

### Shared world view state
Known fields:
- `mapWidthTiles` at +140016, length 4 (High)
- `mapHeightTiles` at +140020, length 4 (High)
- `mapViewLeft` at +140024, length 4 (High)
- `mapViewTop` at +140028, length 4 (High)
- `activeMissionIndex` at +140032, length 4 (High)
- `turnOwnerPlayerIndex` at +147155, length 4 (Medium/High)
- `viewedPlayerIndex` at +147159, length 4 (Medium/High)

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
- `prisonerTransferQueueRaw` at +1357, length 60 (High for layout)
- `queenRelationshipState` at +1419, length 1 (High)
- `queenPortraitIndex` at +1420, length 1 (High)
- `queenNextRelationshipCheckTurn` at +1421, length 2 (High)

Other bytes unknown/reserved (Low).

### Army (725 bytes)
Known fields:
- `tileRow` +0, length 2 (High)
- `tileColumn` +2, length 2 (High)
- `ownerPlayerIndex` +4, length 1 (High)
- `facingDirection` +5, length 1 (High)
- `units` +6, count 10, entry size 31 (High for layout)
- `queuedPathWaypointCount` +316, length 4 (High)
- `isHiddenOnWorldMap` +720, length 1 (High)

Remaining bytes unknown/reserved (Low).

### Unit (31 bytes)
Known fields:
- `typeId` +0, length 2 (High for occupancy marker, `-1` empty)
- `ownerPlayerIndex` +2 (High)
- `currentActionPoints` +8 (High)
- `currentHealthPercent` +9 (High)
- `fatigue` +10 (High)
- `morale` +11 (High)
- `stanceBits` +12, full raw byte containing experience plus other state bits (Medium/High)
- `experienceLevel` +12, low 2 bits, values 0..3 where 3 is maximum/full experience (High for mask)
- `experienceProgress` +12, bits 2..3, values 0..3; the game rolls this into `experienceLevel` (High for mask)
- `stateFlags` +13 (Medium/High)
- `lowMoraleFlag` +13, bit 2, set by low morale checks and cleared by positive morale changes (High for mask)
- `auxRuntimeState` +18, length 4 (High for layout)
- `stateBits2` +22, length 1 (High for layout)

Observed constraints:
- `currentHealthPercent` is clamped to `0..100`.
- `fatigue` is clamped to `0..100`.
- `morale` is clamped to `0..20`.
- Unit types `31..34` are skipped by morale/fatigue adjustment helpers.

Remaining bytes unknown/reserved (Low).

### Castle (467 bytes)
Known fields:
- `tileRow` +0 (High)
- `tileColumn` +1 (High)
- `ownerPlayerIndex` +2 (High)
- `appearance` +3 (Low/Medium)
- `footprintClass` +4 (High for occupancy marker)
- `displayName` +5, length 10 (High)
- `units` +18, count 12, entry size 31 (High for layout)
- `garrisonOrderBytes` +390, length 12 (High for layout)
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
- `prisonerSlotsRaw` +445, length 18 (High for layout)
- `castleFactId` +463, length 4 (High)

Known `addonTypeIds`:
- `0` Court
- `1` Tower
- `2` Hospital
- `3` Barracks
- `4` Workshop
- `5` School
- `6` Smiths
- `7` Peasants
- `8` Barracks
- `255` Empty slot

Remaining bytes unknown/reserved (Low).
