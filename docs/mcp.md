# Headless MCP interface

[Documentation index](README.md) · [Build and packaging](getting-started.md)

## Launch

The `mcp` module provides a JSON-RPC stdio server without a desktop window. Build
and run the standalone JAR from the repository root:

```sh
./gradlew :mcp:fatJar
java -jar mcp/build/libs/mcp-2.0.0-all.jar
```

Use `.\gradlew.bat :mcp:fatJar` in Windows PowerShell. The Windows portable ZIP
also supplies `ClashSaveMcp.cmd`, which uses its bundled Java runtime. Configure
an MCP client to launch that script or `java -jar` with an absolute JAR path.
File paths in tool arguments are resolved by the server process; absolute save
paths avoid dependence on the client's working directory.

For development, `./gradlew -q --console=plain runMcpServer` (or the Windows
wrapper equivalent) delegates to `:mcp:run`. Prefer the JAR or packaged launcher
for client integration so Gradle output does not share the protocol stream.

The server reads one UTF-8 JSON request per line and writes one JSON response per
line. A quiet process waiting for standard input is expected. It implements
`initialize`, `ping`, `tools/list` and `tools/call`; notifications have no response.
Use `tools/list` and `save_get_schema` to discover the current inputs, field
ranges, source evidence and fields that require a transaction.

## Tools

| Tool | Purpose |
| --- | --- |
| `save_get_schema` | Recovered layout, editable properties and evidence metadata |
| `save_get_overview` | World state, counts and entity previews |
| `save_list_entities` | Paginated tiles, players, armies, buildings, units and auxiliary layers |
| `save_read_object` | Read a parsed object using a legacy path or physical selector |
| `save_read_bytes` | Inspect a bounded byte range at an absolute DAT offset |
| `save_set_property` | Validate and change one independently writable scalar property |
| `save_write_bytes` | Apply explicit raw bytes without changing DAT size |

The file-facing tools operate on exact-size DAT files. They do not open
`.clashproj` archives or provide the desktop's structural scenario commands.

## Stable record selection

Legacy `objectPath` selectors retain filtered-index meaning, so `armies[0]`
means the first entry in the filtered army view. Use `record` to address the
physical table slot, including holes. These selectors are mutually exclusive.
For example, a complete read request is:

```json
{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"save_read_object","arguments":{"path":"save/0.dat","record":{"kind":"army","slot":499}}}}
```

For a unit, add `"unitSlot": 0` to an `army_unit` or `building_unit` selector:

```json
{"path":"save/0.dat","record":{"kind":"army_unit","slot":499,"unitSlot":0}}
```

The second example shows tool arguments only. Consult the discovered schema for
the full selector set. Physical identity is shared with the desktop inspector;
see [invariants](reverse-engineering/invariants.md).

## Writes and recovery

Structured writes share core validation. Coupled fields cannot be independently
edited through `save_set_property`; use a supported desktop transaction when a
change needs occupancy, ownership, route or FAC updates. Advanced raw-byte writes
can bypass these semantic invariants.

Writes require `outputPath` unless `inPlace=true`. Replacing a separate existing
output requires `overwrite=true`. In-place writes create non-clobbering `.bak`, `.bak.1`,
… backups by default (`createBackup` controls that behavior). The server writes
only DAT and leaves FAC untouched, including when writing a separate output
file; keep the matching FAC when transferring an edited save.

These writes are distinct from the desktop's journalled DAT/FAC export. See
[safety and integrity](reverse-engineering/safety-and-integrity.md) before using
raw writes or recovering an interrupted pair export.
