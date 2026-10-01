# Save-format compatibility evidence

This document records the public evidence model used by the editor. It is
intentionally limited to factual information required for save-file
interoperability. Decompiler output, disassembler databases, executable code,
retail game files, and proprietary assets are not part of this repository.

## Evidence classes

| Area | Public evidence | Confidence |
| --- | --- | --- |
| DAT envelope, fixed tables, 100-cell storage stride | Exact-size parsing, synthetic fixtures, byte-preserving round trips, and controlled compatibility checks | high |
| Raw map dimensions | Reproducible rectangular-map fixtures and original-game load/save observations | high |
| 27-byte options record | Controlled before/after save comparisons and preservation tests | high for structure; medium-high for the final shared byte |
| Army and building table layout | Fixed-size parsing, sparse-record fixtures, and original-game save/reload observations | high |
| Packed unit state fields | Controlled save differences plus behavior-specific validation | high for documented masks |
| Text interpretation | Windows-1250 default with raw-byte preservation and configurable decoding | implementation convention |
| Runtime handles and transient state | Stable preservation behavior across round trips; values are treated as opaque/runtime-local | high for preservation, intentionally no semantic identity |

## Public evidence rules

A field may be documented when its offset, size, signedness, mask, sentinel, or
behavior can be reproduced without publishing proprietary program code. Good
evidence includes:

1. deterministic synthetic fixtures;
2. byte-for-byte round-trip tests;
3. controlled before/after saves created through the original game's normal UI;
4. repeated observations across independently produced save files;
5. compatibility tests against a lawfully obtained local installation.

Public documentation should state uncertainty where a field is only partially
understood. Unknown bytes remain opaque and are preserved byte-for-byte.

## Repository boundary

The public implementation may contain factual constants needed to read and write
the format, such as record sizes, offsets, masks, and supported value ranges.
It must not contain decompiler output, copied executable code, disassembler
project files, retail assets, manuals, original campaign data, or memory dumps.

Retail-dependent evidence stays local. Public CI uses synthetic fixtures and
generated UI scenes only.
