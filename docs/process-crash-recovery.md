# DAT/FAC process-crash recovery

The core regression `SavePairProcessCrashTest` exercises the production pair
writer in a separate JVM and calls `Runtime.halt(77)` from an internal checkpoint
callback. Halting bypasses the writer's `catch` and `finally` blocks; a checkpoint
receipt proves the requested boundary was reached. Existing callers retain the
same `writePair`/`export` API and the earlier exception-injection hook.

Each boundary runs with both original files present, neither present, only DAT
present, and only FAC present: **28 crash cases**. A fresh JVM calls `recover`,
then a third JVM calls it again. The parent checks exact bytes or absence before
recovery, after recovery, and after the idempotence check.

| Halt boundary | Pair before recovery | Recovery outcome |
| --- | --- | --- |
| DAT stage durable | Original DAT/FAC existence and bytes | No journal; originals unchanged |
| FAC stage durable | Original DAT/FAC existence and bytes | No journal; originals unchanged |
| Backups durable | Original DAT/FAC existence and bytes | No journal; originals unchanged |
| Journal durable | Original DAT/FAC existence and bytes | Restore the original pair |
| DAT replaced | New DAT, original FAC state | Restore the original pair |
| FAC replaced | Both new files | Restore the original pair |
| Journal removed (commit) | Both new files | No rollback; retain the new pair |

The helper accepts only a directory carrying the test's synthetic-fixture marker.
All files are created under JUnit's temporary directory. Original DAT and new DAT
are deterministic byte patterns of the format's declared size, and FACs are small
synthetic text. No user saves, retail files or original game process are involved.
Pre-journal interruption can leave owned stage/backup files without a recovery
journal; the test verifies preservation of the old pair rather than deleting
uncertain remnants. Existing backup retention semantics are unchanged.

Run the focused matrix without a GUI:

```sh
./gradlew :core:test --tests '*SavePairProcessCrashTest'
```

The normal Linux/Windows CI `test` tasks include this matrix and retain core JUnit
XML alongside their existing reports. A successful process-crash matrix does not
prove power-loss or filesystem durability, interruption inside an individual
write, or original-game load/combat/AI/campaign acceptance. Those require separate
evidence. No local Gradle execution is claimed for this change while the host
disk reserve is below the repository threshold; hosted results must be checked.
