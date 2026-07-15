# Clash Save Editor

Kotlin/JVM editor and MCP server for recovered Clash save-slot DAT files.

## Supported format

The editor accepts exact-size `save/N.dat` files:

- 16-byte save label;
- `0x8F29E`-byte raw `gameData` image;
- total size `586414` bytes;
- little-endian recovered numeric fields.

A complete game save also has a matching `save/N.fac` CLIPS facts sidecar. The editor does not rewrite FAC files, so back up and move both files together.

See `docs/reverse-engineering/save-format.md` for the recovered field map and unresolved regions.

## Requirements

- Java 17 or newer
- Kotlin 1.9.x through Gradle

## Setup

```bash
./setup.sh
```

If downloads are blocked, place the required files inside `local-repo` and rerun the script.

## Building and testing

```bash
./gradlew test
./gradlew build
```

The build creates `build/libs/clash-save-editor-all.jar` with the GUI classes and runtime dependencies.

## Running the GUI

```bash
java -jar build/libs/clash-save-editor-all.jar
```

The build uses IntelliJ form instrumentation so `.form` UI files are compiled automatically.

## MCP server

During development:

```bash
./gradlew runMcpServer
```

From the fat JAR:

```bash
java -cp build/libs/clash-save-editor-all.jar com.lis.clash.mcp.ClashSaveMcpServerKt
```

The MCP server exposes schema discovery, save overviews, parsed object navigation, occupancy and trap inspection, raw-byte reads, structured property edits, and explicit raw-byte writes. Structured edits require `outputPath` unless `inPlace=true`; in-place writes create a `.bak` file by default. MCP writes preserve the exact DAT length and do not modify the FAC sidecar.
