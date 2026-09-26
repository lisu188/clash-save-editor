# Kotlin Compose modernization validation

Implementation starts from remote `main` at `585393ce97528a5883ee85c85b16b15208a088e9`
in the isolated `codex/compose-scenario-editor` worktree. Binary interpretations
are pinned to `clash-disassembly` revision `c9c0fa7`.

## Scope

The application remains Kotlin and uses Compose Desktop. The modules separate
byte-preserving codec/transactional document logic, the desktop workspace and
the compatible seven-tool MCP server. Game saves remain exact-size DAT files
with companion FAC files; `.clashproj` is an editor draft container, not a new
game format.

Supported structural authoring uses recovered free-game lifecycles. Unknown FAC
forms and campaign facts remain lossless but block dependency-sensitive edits.
Type-3 buildings and unverified field lifecycles stay inspectable. Terrain and
site brushes expose a limited source-backed palette; imported IDs are retained.
No original artwork or proprietary game binary is included in public CI.

## Automated evidence

The stale sparse-record test offsets were repaired before migration; the
original baseline then passed 22 tests. The new suites add byte-identical DAT
and FAC handling, all physical slots and sparse holes, signed/unsigned limits,
packed bits, legacy encodings, immutable inspection, transactional rollback,
undo/redo, occupancy footprints, unit compaction, route invalidation, typed FAC
dependencies, interrupted pair recovery and bounded project archives.

Compose tests dispatch events to rendered controls and the actual Canvas,
including rectangular visible bounds over the fixed 100-cell storage stride,
filtered physical selection, invalid input and unsaved-change choices. MCP
tests retain subprocess JSON-RPC and filtered-index contracts.

Final integrated command on September 26:

```powershell
.\gradlew.bat test :desktop:portableZip :mcp:fatJar --no-daemon --console=plain
```

**Passed: 85 tests, zero failures/errors/skips** (62 core, 13 desktop including
nine rendered Compose tests, and 10 MCP). The desktop invalid-input test initially
matched a status-bar notice after its dialog closed; its assertion was narrowed
to dialog controls and the full run then passed. The staged diff also passed
`git diff --cached --check`.

PR #16's initial CI run `36263098274` passed on both `ubuntu-latest` and
`windows-latest`, including the Windows package and bundled MCP smoke test.

On September 26, `:core:test` passed **62 tests in eight suites**, with no
failures, errors or skips. `:core:writeAcceptanceFixture` regenerated the
586414-byte DAT and 136-byte FAC with `VALIDATION=[]`. Post-maintenance empty
Gradle metadata directories and the absent generated test-worker bootstrap
were repaired before this run; those environment failures were not test passes.

## Packaging evidence

The Windows portable archive includes the Compose application image, JDK 21
runtime and headless MCP launcher. `tools/test-package.ps1` extracts the actual
archive into a fresh directory and performs initialize/tools-list through its
bundled runtime. The console launcher comes from the same toolchain used for
the runtime image because Compose normally strips native JDK commands.

The final archive was extracted and tested with `tools/test-package.ps1`:
`PACKAGED_MCP_OK tools=7`. The extracted application loaded the exported scenario,
produced `artifacts/acceptance/compose-final.png`, and exited with no stderr.
The frame was visually inspected for layout, map, roster and diagnostics.
These are actual Windows application captures, distinct from Compose assertions.

Archive SHA-256:
`393d15bd48c9573f9bc88ebd203dabb5fa6759657e1b32093b643c837dfdc85c`.
Local packages, extracted smoke directories and raw acceptance evidence remain
ignored rather than being committed as source.

## Original-game acceptance

`core:writeAcceptanceFixture` creates a scenario using the same public document
commands as the editor: two human players, two armies and two castles on an
empty 100×100 grass map. Its export must pass the playable checks before use.

`tools/validate-original.sh` uses an isolated native Linux copy of the retail
executable and a private Wine prefix inside Xvfb. Audio is disabled. Installed
assets and saves are not modified. Exported input hashes, executable hashes,
actual input events, frames and runtime logs remain local acceptance evidence.

Earlier September 24 startup/menu captures are incomplete attempts, not a pass.
A successful acceptance record must separately establish Load Game entry,
map/roster, a human action, a full turn cycle, and original-game save/reload.
Retail-dependent checks are intentionally absent from public CI.

The local harness rejects nonempty evidence destinations and checks the recorded
Xvfb executable and display before signaling a process. Windows Git Bash syntax
and isolated mocked checks passed for evidence preservation, stale/reused PIDs,
wrong displays, invalid PIDs and repeated shutdown. These checks validate the
harness safeguards, separately from original-game behavior.

September 26's completed run in `original-complete-20260926/` established the
entire acceptance sequence through the unmodified original game's UI: **Load
Game**, map and both players' rosters, a human-player army movement, the full
North → South → North turn cycle, save to a separate slot and reload. Inputs
used real X11 keyboard/mouse events; no debugger or in-memory state shortcut
was used. The earlier maintenance-interrupted run remains separate evidence.

The shipped MCP server independently decoded the game-authored save as exactly
586,414 bytes, turn **2**, North active, two active players, two armies and two
buildings. Physical army slot 0 moved from **(4, 4)** to **(4, 3)**. A second save
written after reload independently retained that position and turn counter.
The original export slot remained byte-identical. Detailed hashes, frames, limitations and
reproduction steps are in [original-game acceptance](original-game-acceptance.md).
