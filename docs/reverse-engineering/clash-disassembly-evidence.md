# Pinned clash-disassembly evidence

The binary schema is checked against `clash-disassembly` commit `c9c0fa7`.
The authoritative layout is `data/save_dat_layout.json`; code anchors below
explain interpretations. Confidence describes the field meaning, not whether a
newly authored scenario has passed original-game acceptance.

| Region | Evidence | Confidence |
|---|---|---|
| DAT envelope, fixed tables, 100-cell storage stride | `data/save_dat_layout.json`, recovered `SaveGame`/`LoadGame` | high |
| Raw map dimensions | `Map_LoadFromFile`: MAP_WIDTH scans rows at +1400, MAP_HEIGHT scans columns at +14; screen width/height are transposed | high |
| 27-byte options record | schema `options`; persistence `0044AE90_0044E850_persistence_005.cpp` copies 24+2+1 bytes | high; final music/brightness byte medium-high |
| Army tail at +721 | schema `unit_stack.army_fact_handle` | high; transient runtime handle, read-only |
| Unit +12 low two bits, bits 2..3, bits 4..6 | schema `unit_slot`: status level, order state, volleys used | high |
| Unit +13 bits 0..3 | ready for turn, spent turn, low morale, plague | high |
| Building +422, +432, +434, +438, +442 | seven wall integrity bytes; signed 12-bit growth; signed satisfaction; uint32 money; uint16 last income | high |
| Building prisoners, six bytes | schema `building_prisoner_slot`; `Prisoner_SetInCastles`, `BuildingPrisoner_RecalculateRansomValue` | high |
| Player queued prisoners, six bytes | `Prisoner_QueueCapturedUnit`, `Prisoner_SetInCastles` | high: signed type, owner, uint16 row, uint16 column |
| Display text interpretation | configurable Windows-1250 default | interpretation only; raw bytes retained until an explicit edit |

The old `experienceLevel` and `experienceProgress` names were inaccurate. They
remain deprecated API aliases for `statusLevel` and `orderState` so existing MCP
object paths keep working. They do not establish experience accumulation.

## Preservation rules

- Reading uses byte-backed views and never invokes a write or normalization.
- Edits encode an entire validated scalar before applying its bounded byte patch.
- Masked edits preserve all other packed bits. Out-of-range values are rejected.
- Army and building tables are sparse; packed unit views stop at their first
  `-1` type sentinel. Physical slot views expose every record, including inactive
  and unexpected records, without deleting or rewriting them.
- Cached CLIPS handles are runtime-local metadata, not stable record identities.
- The original DAT remains exactly 586414 bytes. Unsupported fields remain raw.
