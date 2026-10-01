# Unofficial Clash Save Editor workspace

[Documentation index](README.md) · [Getting started](getting-started.md)

The desktop interface uses a shared light/dark Material theme, compact controls,
clear navigation, and a map-focused workspace. Graphics are procedural and do
not require original game artwork.

## Start and navigate

Startup asks the user to create a scenario or open a save/project. Recent files
provide a direct way back to a document, and opening or cancelling a chooser
does not create an unfinished scenario. The File menu keeps New, Open, and
recent documents together once a workspace is open.

The sidebar switches between the map, armies, buildings, players, and scenario
settings. It becomes a labeled compact rail on smaller windows. The map and
entity tables retain visible player-filter controls at every supported width.
Clearing filters preserves physical record selection and does not edit the save.

## Edit with context

The map keeps placement, painting, brushes, layers, and player filtering above
the canvas, with zoom and fit controls over the canvas. The inspector can be
collapsed to make more room. Locate centers the selected formation or building
at a usable scale and makes its entity layer visible.

The inspector presents identity, player, location, units, and common properties
first. Advanced fields and source bytes remain available in a disclosure. A
property's Edit button opens a draft dialog: Apply commits through the document
transaction, Cancel leaves the document unchanged, and validation stays with
the input. Creation and player setup use named choices for players, units,
building types, difficulty, and religion.

Empty scenarios offer steps for player setup, starting forces, and playability
checks. Filtered empty tables explain the filter state and expose Clear filters.

## Save and inspect

Save project is available throughout authoring. Export save performs the
existing playable-save validation. Diagnostics are initially collapsed and can
be opened from the status bar. Check playable save retains export-only findings
until the document changes; structural checks are labeled separately. Reports
and read-only source bytes can be selected and copied.

Keyboard shortcuts remain Ctrl+N (new), Ctrl+O (open), Ctrl+S (save project),
Ctrl+Shift+S (export), Ctrl+Z (undo), and Ctrl+Y / Ctrl+Shift+Z (redo).
Editable property dialogs also accept Enter to apply and Escape to cancel.

## Verification

Desktop interaction tests exercise actual Compose controls and Canvas hit
testing. Layout coverage renders the welcome, workspace, players, and dialogs
at 980×700 and 1440×960 in both light and dark themes. Optional scene captures
come directly from Compose's rendered test scene, including dialogs, without
capturing the Windows desktop:

```powershell
.\gradlew.bat :desktop:test -PstudioPreviewDir=C:/path/to/preview-output --no-daemon --console=plain
```

CI retains these scene captures with its test reports. They are distinct from
native packaged-application captures and original-game acceptance evidence.
The UI tests use generated scenarios and need no retail game files.

The September 26 overhaul passed 32 desktop tests (20 control/Canvas interaction
tests, four viewport tests, and eight viewport/theme layout journeys). The
unchanged core and MCP suites also passed: 104 tests total, with no failures or
skips. Twenty scene PNGs were generated; representative welcome, map, player,
and dialog captures were visually reviewed across both sizes and themes. The
final Windows archive's bundled runtime passed the seven-tool MCP smoke check.

The packaged welcome screen was observed on Windows. The remaining native
walkthrough was paused when the desktop locked; the full workspace evidence
for this revision comes from rendered Compose scenes, not a completed native
input session. The earlier failed layout run is retained separately in local
artifacts, including the real map-fit resize regression it exposed and the
fixture/selector errors corrected before the passing run.
