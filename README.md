# Clash Save Editor

This project uses Gradle with Kotlin support.

## Requirements
- **Java 9** or higher (toolchain target is 9)
- **Kotlin 1.9.x** (handled by Gradle)

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
