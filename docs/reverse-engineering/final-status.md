# Implemented scope and status

[Documentation index](../README.md)

## Reviewed baseline

Documentation reviewed on **2026-09-26** against `main` revision
[`1449731`](https://github.com/lisu188/clash-save-editor/commit/14497310f31ccba66f0c58da233434d681aad1fc),
including the Compose migration in PR #16 and the create/open startup flow in
PR #17. This is a dated baseline; later changes require their own verification.

[CI run 36266968083](https://github.com/lisu188/clash-save-editor/actions/runs/36266968083)
passed both Linux and Windows jobs for that revision. The Windows job built the
portable distribution and reported `PACKAGED_MCP_OK tools=7`. This identifies
existing CI evidence, not a new local application or retail-game test during the
documentation review. The earlier 85-test migration total belongs to
[modernization validation](../modernization-validation.md); it is not
a fixed test-count requirement for later revisions.

The later workspace redesign is documented separately in the
[workspace guide and UI validation record](../studio-ui.md), including its
rendered scene coverage and incomplete native walkthrough. Its interface
changes do not expand the binary editing or original-game acceptance scope below.

## Implemented layout and preservation

The Kotlin core implements the unchanged 586414-byte DAT envelope and all fixed
regions: 10000 terrain records, world header, five players, the 27-byte options
record, 500 armies, 100 buildings, occupancy, trap masks, and port runtime state.
Read-only decoding and explicit bounded patches preserve unknown bytes and
inactive records. Text interpretation is configurable and defaults to
Windows-1250; untouched text is never re-encoded.

Evidence is pinned to `clash-disassembly` revision `c9c0fa7`. The implemented
corrections include status/order/volley bits, signed satisfaction and 12-bit
population growth, unsigned 32-bit money, wall integrity, collected income,
typed six-byte prisoner records, and read-only cached fact handles. Recovered
map dimension names are transposed relative to screen width/height; storage
still uses the fixed 100-cell stride.

## Authoring and interfaces

The Compose Desktop workspace replaces the former Swing application. `core`,
`desktop`, and `mcp` are separate Kotlin modules. The document command model
supports undo/redo, physical record selection, supported free-game army/building
editing, unit composition, player setup, terrain/site/road painting, and traps.
Blank 100×100 drafts use recovered defaults and can be stored in a versioned
`.clashproj` independently of playable-export validation.

FAC remains a separate CLIPS sidecar. It is preserved losslessly; supported
commands update typed dependencies. Unknown dependencies and unsupported
campaign operations are blocked. DAT/FAC pair export stages both files, keeps
unique backups, and provides journal-based recovery. The headless MCP server
retains its seven tools, stdio protocol, filtered object paths, and DAT backup
behavior, while adding physical selectors and schema evidence metadata.

Source compatibility retains the historical `Castle` name and production-licence
aliases. Deprecated experience property names remain aliases for their corrected
status/order meanings. Combined diagnostic state fields are now read-only, and
lifecycle-coupled scalar writes require an appropriate command.

## Verification boundaries

Automated suites cover byte preservation, numeric/text boundaries, sparse and
packed records, command rollback/undo, occupancy/dependency updates, FAC framing,
project/export recovery, MCP subprocess contracts, and Compose interaction.
These tests use synthetic fixtures and do not establish original-game behavior.

The [modernization validation record](../modernization-validation.md) retains
dated tests, packaging and application capture evidence. The separate
[original-game acceptance record](../original-game-acceptance.md) documents the
2026-09-26 pass for one two-human-player free-game fixture: load, roster
inspection, human movement, a full turn cycle and save/reload. That recorded
route does not validate AI, campaign, combat, all editor operations or all game
executable variants.

Remaining scope limits include type-3 building lifecycle, arbitrary campaign
objectives, unproven binary fields, original-art loading, and New Game/Campaign
menu integration. Unknown bytes continue to be preserved rather than guessed.
