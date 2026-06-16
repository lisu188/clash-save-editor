# Clash Save Editor

This project uses Gradle with Kotlin support.

## Requirements
- **Java 9** or higher (toolchain target is 9)
- **Kotlin 1.9.x** (handled by Gradle)

## Setup

Run the helper script to fetch IntelliJ dependencies and build offline:

```bash
./setup.sh
```

If the downloads are blocked, place the required files inside `local-repo`
and rerun the script.

## Building
Run the following command with your system-installed Gradle:

```bash
gradle build
```

The build creates a fat JAR at `build/libs/clash-save-editor-all.jar` which
includes all dependencies and the compiled GUI classes.

## Running

Execute the application with:

```bash
java -jar build/libs/clash-save-editor-all.jar
```

The build uses IntelliJ form instrumentation so `.form` UI files are compiled
automatically.

## MCP server

The project also includes a stdio MCP server for LLM-assisted inspection and
editing of Clash save files. During development, run it with:

```bash
./gradlew runMcpServer
```

After building the fat JAR, configure an MCP client to launch:

```bash
java -cp build/libs/clash-save-editor-all.jar com.lis.clash.mcp.ClashSaveMcpServerKt
```

The server exposes tools for schema discovery, save overviews, parsed object
reads, raw byte reads, annotated property edits, and explicit raw byte writes.
Structured edits require `outputPath` unless `inPlace=true`; in-place writes
create a `.bak` file by default.
