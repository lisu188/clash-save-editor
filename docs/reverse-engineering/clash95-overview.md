# clash95.c reverse-engineering overview

This legacy entry point is retained for existing links. The maintained binary
reference is [Clash save-slot format](save-format.md), with source anchors and
confidence in [pinned disassembly evidence](clash-disassembly-evidence.md).

The earlier overview described only part of the editor's layout and incorrectly
listed ten castle records. The fixed building table contains **100 records of
467 bytes at DAT offset 509690**. Kotlin retains the historical `Castle` class
name for API compatibility; it represents the recovered building records.

The current editor also covers the full DAT envelope, FAC preservation,
transactional scenario authoring, a Compose workspace and a headless MCP server.
See [implemented scope and status](final-status.md), [developer guide](developer-guide.md)
and the [documentation index](../README.md). Unknown semantics remain explicitly
tracked in [unknown fields](unknown-fields.md); an old decompiler name alone is
not evidence to promote a field to a supported edit.
