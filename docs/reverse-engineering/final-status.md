# Recovered save-format implementation status

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

See [delivery validation](../modernization-validation.md) for current test,
packaging, screenshot, and original-game acceptance evidence. This status file
describes implemented capabilities; it does not imply that loading, a human
action, a turn cycle, and game save/reload have all been observed successfully.

Remaining scope limits include type-3 building lifecycle, arbitrary campaign
objectives, unproven binary fields, original-art loading, and New Game/Campaign
menu integration. Unknown bytes continue to be preserved rather than guessed.
