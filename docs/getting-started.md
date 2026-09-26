# Getting started

[Documentation index](README.md)

## Toolchain and source launch

Use **JDK 21** and the checked-in Gradle wrapper. The build pins Gradle 8.14.4,
Kotlin/compiler 2.4.20 and Compose 1.12.1. Compiled bytecode targets JVM 17; the
configured build and packaging toolchain is still JDK 21. There is no IntelliJ
form instrumentation step.

Run commands from the repository root. On Linux or macOS:

```sh
./gradlew run
```

In Windows PowerShell:

```powershell
.\gradlew.bat run
```

The desktop needs a graphical session. Original artwork and retail game files
are not needed to launch the editor or run its public tests. The original game
and its assets are needed for retail acceptance.

## Create, inspect and export

1. Choose **Create scenario from scratch** or **Open existing save** on launch.
   New creates an empty grass draft with recovered player/options defaults.
   Open accepts a DAT or a `.clashproj`; keep a DAT's matching FAC beside it.
2. Configure players and place starting armies or buildings. Map and filtered
   tables share physical record IDs; select an entity to edit it in the
   inspector. Map tools provide pan, zoom, fit, layer controls and batched brushes.
3. Use **Save project** for unfinished work. The versioned `.clashproj` ZIP stores
   a manifest, DAT image, optional FAC and document text encoding.
4. Use **Export save** once validation succeeds. Playable export requires
   at least two active players, a human player, a qualifying starting force for
   each active player, valid ownership/footprints, consistent occupancy and
   supported rules dependencies. [Invariants](reverse-engineering/invariants.md)
   defines the detailed requirements.
5. Export to a separate destination, then copy both output files to an original
   game save slot, such as `save/0.dat` and `save/0.fac`. Load it through **Load
   Game**. A `.clashproj` is an editor container and cannot be loaded by the game.

| Action | Shortcut |
| --- | --- |
| New / Open | `Ctrl+N` / `Ctrl+O` |
| Save project / Export save | `Ctrl+S` / `Ctrl+Shift+S` |
| Undo / Redo | `Ctrl+Z` / `Ctrl+Y` or `Ctrl+Shift+Z` |

Name interpretation defaults to Windows-1250. Choose the document encoding on
the Scenario page; this undoable change does not transcode existing bytes.
Unchanged strings retain their original bytes, while edited strings must fit
the field and selected encoding. Undo/redo restores the full transaction,
including occupancy and supported FAC facts.

The [workspace guide](studio-ui.md) covers the current File menu, recent files,
Locate, player filters, property Apply/Cancel dialogs and diagnostics workflow.

DAT/FAC export stages both files, retains uniquely named backups and writes a
recovery journal. If interrupted, the application offers restoration of the
previous pair before opening it. See [safety and integrity](reverse-engineering/safety-and-integrity.md)
for the distinction between recoverable pair replacement and single-DAT MCP writes.

## Tests and CI

```sh
./gradlew test
# Headless Linux, including rendered Compose tests:
xvfb-run -a ./gradlew test --stacktrace --console=plain
```

On Windows, use `.\gradlew.bat test`. The root `test` task runs `:core:test`,
`:desktop:test` and `:mcp:test`; root `check` delegates to these tests. Focused
core/MCP tests can run without a display:

```sh
./gradlew :core:test :mcp:test
```

The [CI workflow](../.github/workflows/ci.yml) runs on Linux and Windows. Linux
uses Xvfb; Windows tests, builds the portable package and MCP fat JAR, then checks
the bundled runtime's initialize/tools-list exchange. Reports are under
`core/build/reports/tests/`, `desktop/build/reports/tests/` and
`mcp/build/reports/tests/`, and are uploaded as CI artifacts.
Rendered UI scenes are retained under `desktop/build/reports/ui/`; see
[workspace validation](studio-ui.md#verification) for capture commands and limits.

These synthetic tests do not exercise a retail executable. See
[recorded validation](README.md#recorded-validation) for the separate application
capture and original-game acceptance records.

## Windows portable distribution

Build on Windows with JDK 21:

```powershell
.\gradlew.bat test :desktop:portableZip :mcp:fatJar --stacktrace --console=plain
.\tools\test-package.ps1 -Destination artifacts/package-smoke-new
```

Choose a fresh smoke-test destination for each run; the script refuses an
existing directory. It extracts the actual archive and verifies the bundled
runtime and seven-tool MCP contract.

The archive is `desktop/build/distributions/ClashSaveEditor-windows-x64.zip`.
Extract it, then launch `ClashSaveEditor/ClashSaveEditor.exe`. The archive includes
the Java runtime; its `ClashSaveMcp.cmd` starts the headless server with that
runtime. The unarchived application image is under
`desktop/build/compose/binaries/main/app`.

CI publishes the Windows archive as the `ClashSaveEditor-windows-x64` workflow
artifact. Native MSI, DEB and DMG formats are configured in the build, but the
documented and CI-verified distribution route is the Windows portable ZIP.
See [MCP interface](mcp.md) for the standalone JAR and development server command.
