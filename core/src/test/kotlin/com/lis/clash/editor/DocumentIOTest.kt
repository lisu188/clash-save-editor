package com.lis.clash.editor

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.jupiter.api.io.TempDir
import kotlin.test.*

class DocumentIOTest {
    @TempDir lateinit var directory: Path

    @Test fun losslessFacPreservesNestedUnknownFormsCommentsAndEscapedStrings() {
        val bytes = "; preface\r\n(initial-fact)\r\n(mystery (x 3) \"quote \\\" value\") ; tail\r\n".toByteArray()
        val fac = FacDocument.parse(bytes)
        assertContentEquals(bytes, fac.toByteArray())
        assertNotNull(fac.structuralProblem())
        val updated = fac.site("pulapka", 1, 2, true).toByteArray().toString(Charsets.ISO_8859_1)
        assertTrue(updated.startsWith(bytes.toString(Charsets.ISO_8859_1)))
        assertContains(updated, "(pulapka 1 2)")
    }

    @Test fun facMalformedSyntaxAndNulFailExplicitly() {
        for (text in listOf("(unclosed", ")", "bare words", "(x \"unclosed)", "(x)\u0000")) {
            assertFails { FacDocument.parse(text.toByteArray()) }
        }
    }

    @Test fun draftProjectPreservesDatAndOpaqueFacAndEncoding() {
        val original = SaveDocument.newScenario(); val bytes = original.datBytes(); bytes[123] = 81
        val fac = "; unknown draft\r\n(foo 3)\r\n".toByteArray()
        val doc = SaveDocument.fromDat(bytes, fac, "windows-1252")
        val path = directory.resolve("unfinished.clashproj")
        ProjectIO.save(doc, path); val loaded = ProjectIO.load(path)
        assertContentEquals(bytes, loaded.datBytes()); assertContentEquals(fac, loaded.facBytes())
        assertEquals("windows-1252", loaded.encoding); assertFalse(doc.dirty); assertFalse(loaded.dirty)
        assertTrue(loaded.validate(true).any { it.severity == IssueSeverity.ERROR })
    }

    @Test fun projectRejectsTraversalDuplicateEntriesAndUnsupportedVersion() {
        val bad = directory.resolve("bad.clashproj")
        ZipOutputStream(Files.newOutputStream(bad)).use { zip ->
            zip.putNextEntry(ZipEntry("../outside.dat")); zip.write(byteArrayOf(1)); zip.closeEntry()
        }
        assertFailsWith<IllegalStateException> { ProjectIO.load(bad) }
        assertFalse(Files.exists(directory.parent.resolve("outside.dat")))
        ZipOutputStream(Files.newOutputStream(bad)).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.properties")); zip.write("format=clash-project\nversion=99\n".toByteArray()); zip.closeEntry()
        }
        assertFailsWith<IllegalArgumentException> { ProjectIO.load(bad) }
    }

    @Test fun exportWritesBothFilesAndRetainsNonClobberingBackups() {
        val doc = SaveDocument.newScenario()
        doc.execute(EditCommand.ConfigurePlayer(0, true, true)); doc.execute(EditCommand.ConfigurePlayer(1, true, false))
        doc.execute(EditCommand.CreateArmy(2, 2, 0)); doc.execute(EditCommand.CreateArmy(8, 8, 1))
        val path = directory.resolve("0.dat")
        Files.write(path, byteArrayOf(7)); Files.write(directory.resolve("0.fac"), byteArrayOf(8))
        val first = SaveSlotIO.export(doc, path)
        assertEquals(2, first.backups.size); assertContentEquals(byteArrayOf(7), Files.readAllBytes(first.backups[0]))
        assertContentEquals(doc.datBytes(), Files.readAllBytes(path)); assertContentEquals(doc.facBytes(), Files.readAllBytes(first.facPath))
        val second = SaveSlotIO.export(doc, path)
        assertTrue(second.backups.none { it in first.backups }); assertTrue(first.backups.all { Files.exists(it) })
        assertFalse(Files.exists(SaveSlotIO.journalPath(path)))
    }

    @Test fun firstReplacementFailureRestoresBothOriginals() {
        val dat = directory.resolve("1.dat"); val fac = directory.resolve("1.fac")
        val oldDat = byteArrayOf(7, 8); val oldFac = byteArrayOf(9, 10)
        Files.write(dat, oldDat); Files.write(fac, oldFac)
        assertFailsWith<IllegalStateException> {
            SaveSlotIO.writePair(SaveDocument.newScenario().datBytes(), "(initial-fact)".toByteArray(), dat) { error("Injected replacement failure") }
        }
        assertContentEquals(oldDat, Files.readAllBytes(dat)); assertContentEquals(oldFac, Files.readAllBytes(fac))
        assertFalse(Files.exists(SaveSlotIO.journalPath(dat)))
    }

    @Test fun interruptedFirstExportRecoversAbsenceOfOriginalFiles() {
        val dat = directory.resolve("2.dat")
        assertFailsWith<IllegalStateException> {
            SaveSlotIO.writePair(SaveDocument.newScenario().datBytes(), "(initial-fact)".toByteArray(), dat) { error("Injected interruption") }
        }
        assertFalse(Files.exists(dat)); assertFalse(Files.exists(directory.resolve("2.fac")))
    }

    @Test fun durableRecoveryJournalRestoresPairAfterProcessInterruption() {
        val dat = directory.resolve("3.dat"); val fac = directory.resolve("3.fac"); val token = UUID.randomUUID().toString()
        Files.write(dat, byteArrayOf(5)); Files.write(fac, byteArrayOf(6))
        Files.write(directory.resolve("3.dat.bak.$token"), byteArrayOf(1)); Files.write(directory.resolve("3.fac.bak.$token"), byteArrayOf(2))
        val properties = Properties().apply {
            setProperty("version", "1"); setProperty("dat", "3.dat"); setProperty("fac", "3.fac"); setProperty("token", token)
            setProperty("oldDat", "true"); setProperty("oldFac", "true")
        }
        Files.newOutputStream(SaveSlotIO.journalPath(dat)).use { properties.store(it, "recovery fixture") }
        assertTrue(SaveSlotIO.recover(dat)); assertContentEquals(byteArrayOf(1), Files.readAllBytes(dat)); assertContentEquals(byteArrayOf(2), Files.readAllBytes(fac))
        assertFalse(SaveSlotIO.recover(dat))
    }

    @Test fun invalidDraftExportWritesNothing() {
        val dat = directory.resolve("4.dat")
        assertFailsWith<IllegalArgumentException> { SaveSlotIO.export(SaveDocument.newScenario(), dat) }
        assertFalse(Files.exists(dat)); assertFalse(Files.exists(directory.resolve("4.fac")))
    }
}
