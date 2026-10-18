package com.lis.clash.editor

import com.lis.clash.SaveFormat
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.io.TempDir
import kotlin.test.*

/** Actual process termination, not a caught exception or a fabricated journal. */
class SavePairProcessCrashTest {
    @TempDir lateinit var directory: Path

    @TestFactory fun everyDurableBoundaryRecoversInFreshJvms(): List<DynamicTest> =
        listOf(false, true).flatMap { hadDat ->
            listOf(false, true).flatMap { hadFac ->
                PairWriteCheckpoint.entries.map { point ->
                    DynamicTest.dynamicTest("DAT=$hadDat FAC=$hadFac crash=$point") {
                        verifyCrash(hadDat, hadFac, point)
                    }
                }
            }
        }

    private fun verifyCrash(hadDat: Boolean, hadFac: Boolean, point: PairWriteCheckpoint) {
        val fixture = Files.createDirectory(directory.resolve("$hadDat-$hadFac-$point"))
        Files.writeString(fixture.resolve(SavePairCrashProcess.GUARD), SavePairCrashProcess.GUARD_TEXT)
        val dat = fixture.resolve("0.dat")
        val fac = fixture.resolve("0.fac")
        if (hadDat) Files.write(dat, SavePairCrashProcess.oldDat)
        if (hadFac) Files.write(fac, SavePairCrashProcess.oldFac)

        runChild(fixture, "write", point.name, expectedExit = SavePairCrashProcess.HALT_EXIT)
        assertEquals(point.name, Files.readString(fixture.resolve("checkpoint.txt")))

        // These assertions inspect the durable pair before any recovery process.
        val datReplaced = point in setOf(PairWriteCheckpoint.DAT_REPLACED, PairWriteCheckpoint.FAC_REPLACED, PairWriteCheckpoint.COMMITTED)
        val facReplaced = point in setOf(PairWriteCheckpoint.FAC_REPLACED, PairWriteCheckpoint.COMMITTED)
        assertFile(dat, if (datReplaced) SavePairCrashProcess.newDat else SavePairCrashProcess.oldDat.takeIf { hadDat })
        assertFile(fac, if (facReplaced) SavePairCrashProcess.newFac else SavePairCrashProcess.oldFac.takeIf { hadFac })
        val journalPending = point in setOf(PairWriteCheckpoint.JOURNAL_DURABLE, PairWriteCheckpoint.DAT_REPLACED, PairWriteCheckpoint.FAC_REPLACED)
        assertEquals(journalPending, Files.exists(SaveSlotIO.journalPath(dat)))

        val recovery = runChild(fixture, "recover", "first", expectedExit = 0)
        assertEquals("recovered=$journalPending", recovery.trim())
        val committed = point == PairWriteCheckpoint.COMMITTED
        assertFile(dat, if (committed) SavePairCrashProcess.newDat else SavePairCrashProcess.oldDat.takeIf { hadDat })
        assertFile(fac, if (committed) SavePairCrashProcess.newFac else SavePairCrashProcess.oldFac.takeIf { hadFac })
        assertFalse(Files.exists(SaveSlotIO.journalPath(dat)))

        // A third JVM proves recovery is not relying on state in either predecessor.
        assertEquals("recovered=false", runChild(fixture, "recover", "second", expectedExit = 0).trim())
        assertFile(dat, if (committed) SavePairCrashProcess.newDat else SavePairCrashProcess.oldDat.takeIf { hadDat })
        assertFile(fac, if (committed) SavePairCrashProcess.newFac else SavePairCrashProcess.oldFac.takeIf { hadFac })
    }

    private fun assertFile(path: Path, expected: ByteArray?) {
        if (expected == null) assertFalse(Files.exists(path), "Unexpected file: $path")
        else assertContentEquals(expected, Files.readAllBytes(path), "Wrong bytes: $path")
    }

    private fun runChild(fixture: Path, mode: String, point: String, expectedExit: Int): String {
        val suffix = if (System.getProperty("os.name").startsWith("Windows")) ".exe" else ""
        val java = Path.of(System.getProperty("java.home"), "bin", "java$suffix")
        val classpath = requireNotNull(System.getProperty("clash.test.runtimeClasspath")) {
            "Gradle must provide the test runtime classpath for fresh JVM probes"
        }
        val output = fixture.resolve("$mode-$point.log")
        val process = ProcessBuilder(java.toString(), "-Xmx64m", "-cp", classpath,
            SavePairCrashProcess::class.java.name, mode, fixture.toString(), point)
            .redirectErrorStream(true).redirectOutput(output.toFile()).start()
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            check(process.waitFor(10, TimeUnit.SECONDS)) { "Child process did not stop" }
            fail("Child JVM timed out: ${Files.readString(output)}")
        }
        val text = Files.readString(output)
        assertEquals(expectedExit, process.exitValue(), text)
        return text
    }
}

/** Test-only entry point; a fixture marker prevents accidental use on user saves. */
internal object SavePairCrashProcess {
    const val GUARD = ".synthetic-pair-crash-fixture"
    const val GUARD_TEXT = "Disposable synthetic DAT/FAC process test only"
    const val HALT_EXIT = 77
    val oldDat = ByteArray(SaveFormat.DAT_SIZE) { (it * 7 + 3).toByte() }
    val newDat = ByteArray(SaveFormat.DAT_SIZE) { (it * 31 + 17).toByte() }
    val oldFac = "; old synthetic sidecar\n(initial-fact)\n".toByteArray()
    val newFac = "; new synthetic sidecar\n(initial-fact)\n".toByteArray()

    @JvmStatic fun main(args: Array<String>) {
        require(args.size == 3)
        val fixture = Path.of(args[1]).toAbsolutePath().normalize()
        require(Files.readString(fixture.resolve(GUARD)) == GUARD_TEXT)
        val dat = fixture.resolve("0.dat")
        when (args[0]) {
            "write" -> {
                val stopAt = PairWriteCheckpoint.valueOf(args[2])
                SaveSlotIO.writePairWithCheckpoints(newDat, newFac, dat, { point ->
                    if (point == stopAt) {
                        Files.writeString(fixture.resolve("checkpoint.txt"), point.name)
                        Runtime.getRuntime().halt(HALT_EXIT)
                    }
                })
                error("Requested crash checkpoint was never reached")
            }
            "recover" -> println("recovered=${SaveSlotIO.recover(dat)}")
            else -> error("Unsupported synthetic probe mode")
        }
    }
}
