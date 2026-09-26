# Safety and integrity

## Byte and transaction guarantees

DAT input/output remains exactly **586414 bytes**, comprising a 16-byte label and
the original raw `gameData` image. Lazy decoding never invokes a write. Untouched
names, padding, inactive records, and unknown fields remain byte-identical.
Explicit scalar edits reject representation overflow and string encoding/length
errors before patching; masked edits preserve unrelated bits. Player/building
names reserve their terminating NUL byte, while the save label permits 16 bytes.

Structured operations run through `SaveDocument` transactions. A failed command
leaves both DAT and FAC unchanged. Undo/redo restores both buffers, including
occupancy, paths, and supported rule dependencies. Raw-byte MCP writes are an
advanced escape hatch and can violate these semantic guarantees.

## DAT and FAC handling

FAC is losslessly framed as CLIPS text. Independent scalar edits leave it
unchanged. Supported free-game commands update the associated player, building,
site, and trap facts while preserving unrelated text, whitespace, and comments.
Unknown or malformed dependencies block structural commands. Campaign saves
retain their existing logic; the editor does not synthesize replacement campaign
facts. DAT-only inputs remain inspectable and permit independent scalar edits,
but rules-dependent operations require the companion file.

Playable export validates the roster, ownership, footprints, packed units,
occupancy, terrain range, and supported free-game FAC dependencies. Drafts can
always be saved as versioned `.clashproj` archives before they become playable.
Exact file size and successful validation do not prove original-game acceptance.

Pair export stages DAT and FAC, creates non-clobbering backups when destinations
exist, records recovery metadata, and then replaces the destinations. Interrupted
exports are detected through a sidecar journal; recovery restores the previous
pair. This is recoverable two-file replacement, not a filesystem transaction
that makes both filenames change simultaneously. Keep both resulting save files
together when copying them into the game's numbered save slots.

MCP retains its established single-DAT write and `.bak`, `.bak.1`, ... behavior.
Those writes neither copy nor reconstruct FAC. Its coupled structured fields are
blocked by the shared policy; deliberate raw writes remain the caller's responsibility.

## Runtime state and evidence limits

Tile dwords at `+6/+10`, unit auxiliary state at `+18`, army fact handle at `+721`,
and building fact handle at `+463` are runtime state. Handles are read-only
metadata rather than stable identities. Newly allocated records use recovered
initialization, cloning clears fact handles, and affected movement paths/tile
caches are invalidated by supported structural commands. Browsing a save does
not clear any of these bytes.

The original format has no embedded version, checksum, compression, or
relocation table. Different executable variants cannot be identified by a DAT
version tag. Preserve source evidence, keep copies, and distinguish automated
preservation checks from the original-game acceptance recorded in
[delivery validation](../modernization-validation.md).
