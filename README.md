# Clash Save Editor

A Kotlin/JVM reverse-engineering tool for inspecting and editing save files from the Windows 95 game **Clash**.

The project combines a desktop editor with an explicit binary schema and a stdio **MCP server**, so the same parsing/editing surface can be used interactively or through automated tooling. Structured edits are designed to be safer than blind byte patching: the MCP API supports schema discovery, parsed-object reads, raw inspection, annotated property edits and explicit raw writes.

## Engineering highlights

- Kotlin/JVM binary parsing and structured save-file editing
- IntelliJ-form desktop UI
- Gradle build producing a self-contained/fat JAR
- stdio MCP interface for programmatic and LLM-assisted inspection
- explicit output/in-place semantics; in-place edits create a backup by default
- reverse-engineering documentation and recovered structure information

This repository is part of a broader set of Clash reverse-engineering projects. For executable reconstruction and runtime evidence, see `clash-disassembly`; for resolution/binary-patching work, see `clash-hd`.

## Requirements

- Java 9+
- Kotlin 1.9.x (managed by Gradle)

## Setup

The helper script fetches the IntelliJ dependencies used by the UI build:

```bash
./setup.sh
```

If downloads are blocked, place the required files inside `local-repo` and rerun the script.

## Build

```bash
gradle build
```

The build creates:

```text
build/libs/clash-save-editor-all.jar
```

## Run the desktop editor

```bash
java -jar build/libs/clash-save-editor-all.jar
```

## MCP server

During development:

```bash
./gradlew runMcpServer
```

From the built JAR:

```bash
java -cp build/libs/clash-save-editor-all.jar com.lis.clash.mcp.ClashSaveMcpServerKt
```

The MCP server exposes tools for:

- schema discovery
- save overviews
- parsed object reads
- raw byte reads
- annotated property edits
- explicit raw byte writes

Structured edits require `outputPath` unless `inPlace=true`; in-place writes create a `.bak` file by default.

## Repository hygiene

IDE workspace/cache files are intentionally excluded from version control. Binary save material in the repository is retained only where it is useful as reverse-engineering/test evidence; generated backups and local build artifacts are ignored.
