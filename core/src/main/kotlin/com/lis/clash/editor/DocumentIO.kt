package com.lis.clash.editor

import com.lis.clash.SaveFormat
import java.io.ByteArrayOutputStream
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.StandardOpenOption
import java.util.Properties
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Versioned draft container; invalid/incomplete game states are deliberately permitted. */
object ProjectIO {
    fun save(document: SaveDocument, path: Path) {
        val target = path.toAbsolutePath().normalize()
        Files.createDirectories(target.parent)
        val temp = Files.createTempFile(target.parent, ".clash-project-", ".tmp")
        try {
            ZipOutputStream(Files.newOutputStream(temp)).use { zip ->
                val manifest = Properties().apply {
                    setProperty("format", "clash-project"); setProperty("version", "1")
                    setProperty("encoding", document.encoding); setProperty("hasFac", (document.facBytes() != null).toString())
                }
                fun entry(name: String, bytes: ByteArray) {
                    zip.putNextEntry(ZipEntry(name)); zip.write(bytes); zip.closeEntry()
                }
                entry("manifest.properties", ByteArrayOutputStream().also { manifest.store(it, "Clash editor project") }.toByteArray())
                entry("game.dat", document.datBytes())
                document.facBytes()?.let { entry("game.fac", it) }
            }
            force(temp); replace(temp, target); document.markSaved()
        } finally { Files.deleteIfExists(temp) }
    }

    fun load(path: Path): SaveDocument {
        require(Files.size(path) <= 10 * 1024 * 1024) { "Project archive exceeds 10 MiB" }
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(Files.newInputStream(path)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val limit = when (entry.name) {
                    "manifest.properties" -> 16 * 1024
                    "game.dat" -> SaveFormat.DAT_SIZE
                    "game.fac" -> 8 * 1024 * 1024
                    else -> error("Unsupported project entry '${entry.name}'")
                }
                require(!entry.isDirectory && entry.name !in entries) { "Duplicate or directory project entry '${entry.name}'" }
                val bytes = zip.readNBytes(limit + 1)
                require(bytes.size <= limit) { "Project entry '${entry.name}' exceeds its limit" }
                entries[entry.name] = bytes
            }
        }
        val manifest = Properties().apply { load(requireNotNull(entries["manifest.properties"]) { "Project manifest is missing" }.inputStream()) }
        require(manifest.getProperty("format") == "clash-project" && manifest.getProperty("version") == "1") { "Unsupported project format/version" }
        val dat = requireNotNull(entries["game.dat"]) { "Project DAT image is missing" }
        require(manifest.getProperty("hasFac") in setOf("true", "false")) { "Invalid project FAC metadata" }
        require((manifest.getProperty("hasFac") == "true") == (entries["game.fac"] != null)) { "Project FAC entry does not match its manifest" }
        return SaveDocument.fromDat(dat, entries["game.fac"], requireNotNull(manifest.getProperty("encoding")))
    }
}

data class ExportResult(val datPath: Path, val facPath: Path, val backups: List<Path>)

/** DAT/FAC replacement with durable stages, non-clobbering backups and rollback journal. */
object SaveSlotIO {
    fun load(path: Path): SaveDocument {
        val dat = path.toAbsolutePath().normalize()
        require(!Files.exists(journalPath(dat))) { "Interrupted save-slot export detected. Recover the DAT/FAC pair before opening it: ${journalPath(dat)}" }
        require(Files.size(dat) == SaveFormat.DAT_SIZE.toLong()) { "DAT size must be exactly ${SaveFormat.DAT_SIZE} bytes" }
        val fac = companionPath(dat)
        return SaveDocument.fromDat(Files.readAllBytes(dat), if (Files.exists(fac)) {
            require(Files.size(fac) <= 8 * 1024 * 1024) { "FAC exceeds 8 MiB" }; Files.readAllBytes(fac)
        } else null)
    }

    fun export(document: SaveDocument, path: Path): ExportResult {
        val errors = document.validate(forExport = true).filter { it.severity == IssueSeverity.ERROR }
        require(errors.isEmpty()) { errors.joinToString("\n") { it.message } }
        return writePair(document.datBytes(), requireNotNull(document.facBytes()), path)
    }

    internal fun writePair(dat: ByteArray, fac: ByteArray, path: Path, afterFirstReplacement: (() -> Unit)? = null): ExportResult {
        SaveFormat.requireValidDatSize(dat.size)
        val target = path.toAbsolutePath().normalize()
        require(target.fileName.toString().endsWith(".dat", true)) { "Choose a .dat save-slot filename" }
        val sidecar = companionPath(target); val journal = journalPath(target)
        Files.createDirectories(target.parent)
        require(!Files.exists(journal)) { "An interrupted export exists. Recover it before exporting: $journal" }
        val token = UUID.randomUUID().toString()
        val datStage = target.resolveSibling("${target.fileName}.stage.$token")
        val facStage = sidecar.resolveSibling("${sidecar.fileName}.stage.$token")
        val datBackup = target.resolveSibling("${target.fileName}.bak.$token")
        val facBackup = sidecar.resolveSibling("${sidecar.fileName}.bak.$token")
        val oldDat = Files.exists(target); val oldFac = Files.exists(sidecar)
        var journalCreated = false
        try {
            writeDurable(datStage, dat); writeDurable(facStage, fac)
            if (oldDat) { Files.copy(target, datBackup); force(datBackup) }
            if (oldFac) { Files.copy(sidecar, facBackup); force(facBackup) }
            val properties = Properties().apply {
                setProperty("version", "1"); setProperty("dat", target.fileName.toString()); setProperty("fac", sidecar.fileName.toString())
                setProperty("token", token); setProperty("oldDat", oldDat.toString()); setProperty("oldFac", oldFac.toString())
            }
            writeDurable(journal, ByteArrayOutputStream().also { properties.store(it, "Restore both old files after an interrupted export") }.toByteArray())
            journalCreated = true
            replace(datStage, target)
            afterFirstReplacement?.invoke()
            replace(facStage, sidecar)
            Files.delete(journal)
            return ExportResult(target, sidecar, listOfNotNull(datBackup.takeIf { oldDat }, facBackup.takeIf { oldFac }))
        } catch (failure: Exception) {
            if (journalCreated) runCatching { recover(target) }.exceptionOrNull()?.let { failure.addSuppressed(it) }
            throw failure
        } finally {
            Files.deleteIfExists(datStage); Files.deleteIfExists(facStage)
        }
    }

    /** Returns false when no recovery is pending. Both previous files are restored as one recovery operation. */
    fun recover(path: Path): Boolean {
        val dat = path.toAbsolutePath().normalize(); val fac = companionPath(dat); val journal = journalPath(dat)
        if (!Files.exists(journal)) return false
        require(Files.size(journal) <= 16 * 1024) { "Invalid export journal size" }
        val p = Properties().apply { Files.newInputStream(journal).use { load(it) } }
        require(p.getProperty("version") == "1" && p.getProperty("dat") == dat.fileName.toString() && p.getProperty("fac") == fac.fileName.toString()) { "Export journal does not match this save slot" }
        val token = requireNotNull(p.getProperty("token")); require(runCatching { UUID.fromString(token).toString() == token }.getOrDefault(false)) { "Invalid recovery token" }
        val oldDat = p.getProperty("oldDat"); val oldFac = p.getProperty("oldFac")
        require(oldDat in setOf("true", "false") && oldFac in setOf("true", "false")) { "Invalid recovery state" }
        val pairs = listOf(dat to (oldDat == "true"), fac to (oldFac == "true"))
        pairs.forEach { (target, existed) -> if (existed) require(Files.isRegularFile(target.resolveSibling("${target.fileName}.bak.$token"))) { "Recovery backup is missing" } }
        pairs.forEach { (target, existed) ->
            if (existed) {
                val stage = target.resolveSibling("${target.fileName}.restore.$token")
                Files.copy(target.resolveSibling("${target.fileName}.bak.$token"), stage, REPLACE_EXISTING)
                force(stage); replace(stage, target)
            } else Files.deleteIfExists(target)
            Files.deleteIfExists(target.resolveSibling("${target.fileName}.stage.$token"))
        }
        Files.delete(journal); return true
    }

    fun companionPath(dat: Path): Path = dat.resolveSibling(dat.fileName.toString().substringBeforeLast('.') + ".fac")
    fun journalPath(dat: Path): Path = dat.resolveSibling(dat.fileName.toString() + ".clash-export-journal")
}

private fun writeDurable(path: Path, bytes: ByteArray) {
    Files.write(path, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE); force(path)
}
private fun force(path: Path) { FileChannel.open(path, StandardOpenOption.WRITE).use { it.force(true) } }
private fun replace(from: Path, to: Path) {
    try { Files.move(from, to, ATOMIC_MOVE, REPLACE_EXISTING) }
    catch (_: java.nio.file.AtomicMoveNotSupportedException) { Files.move(from, to, REPLACE_EXISTING) }
}
