package com.lis.clash.desktop

import androidx.compose.runtime.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.lis.clash.editor.ProjectIO
import com.lis.clash.editor.SaveDocument
import com.lis.clash.editor.SaveSlotIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Robot
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter

data class EditorActions(
    val newDocument: () -> Unit,
    val open: () -> Unit,
    val openRecent: (String) -> Unit,
    val saveProject: () -> Unit,
    val export: () -> Unit,
    val close: () -> Unit,
    val recover: () -> Unit = {}
)

fun main(args: Array<String>) = application {
    val state = remember { EditorState(SaveDocument.newScenario().also { it.markSaved() }) }
    val scope = rememberCoroutineScope()
    val unsavedChanges = remember { UnsavedChangesGuard() }
    val requestGuarded: (() -> Unit) -> Unit = { action ->
        unsavedChanges.request(state.document.dirty, action)
    }
    fun error(failure: Exception) {
        state.error = failure.message ?: failure.javaClass.simpleName
        state.notice = "Operation failed · Your document is still open"
    }
    fun load(path: Path) {
        if (state.busy) return
        state.busy = true
        scope.launch {
            state.notice = "Opening ${path.fileName}…"
            try {
                val isProject = path.toString().endsWith(".clashproj", true)
                val (document, snapshot) = withContext(Dispatchers.IO) {
                    val document = if (isProject) ProjectIO.load(path) else SaveSlotIO.load(path)
                    document to document.snapshot()
                }
                state.install(document, path, if (isProject) path else null, snapshot)
            } catch (failure: Exception) {
                if (path.toString().endsWith(".dat", true) && Files.exists(SaveSlotIO.journalPath(path.toAbsolutePath()))) {
                    state.recoveryPath = path
                    state.notice = "Interrupted game save detected · Recovery is available"
                } else error(failure)
            }
            finally { state.busy = false }
        }
    }
    fun save(after: (() -> Unit)? = null) {
        if (state.busy) return
        state.busy = true
        scope.launch {
            var completed = false
            try {
                val path = state.projectPath ?: withContext(Dispatchers.IO) {
                    choosePath(true, "Save editor project", "clashproj", suggestedName(state.snapshot.name, "clashproj"), state.sourcePath)
                } ?: return@launch
                val document = state.document
                withContext(Dispatchers.IO) { ProjectIO.save(document, path) }
                document.markSaved()
                state.projectPath = path
                state.rememberPath(path)
                state.refresh("Project saved · ${path.fileName}")
                completed = true
            } catch (failure: Exception) { error(failure) }
            finally { state.busy = false }
            if (completed) after?.invoke()
        }
    }
    val actions = EditorActions(
        newDocument = { if (!state.busy) requestGuarded { state.install(SaveDocument.newScenario()) } },
        open = {
            requestGuarded {
                scope.launch {
                    state.busy = true
                    var path: Path? = null
                    try { path = withContext(Dispatchers.IO) { choosePath(false, "Open Clash save or project", null, null, state.sourcePath) } }
                    catch (failure: Exception) { error(failure) }
                    finally { state.busy = false }
                    path?.let { load(it) }
                }
            }
        },
        openRecent = { path -> requestGuarded { load(Path.of(path)) } },
        saveProject = { save() },
        export = {
            if (!state.busy) {
                state.busy = true
                scope.launch {
                try {
                    val issues = state.document.validate(forExport = true)
                    val errors = issues.filter { it.severity.name == "ERROR" }
                    if (errors.isNotEmpty()) {
                        state.error = "Resolve these issues before exporting a playable save:\n\n" + errors.joinToString("\n") { "• ${it.message}" }
                        state.bottomOpen = true
                        state.bottomTab = 0
                        return@launch
                    }
                    val exportName = state.sourcePath?.fileName?.toString()?.takeIf { it.endsWith(".dat", true) }
                        ?.substringBeforeLast('.')?.let { "$it-edited.dat" } ?: "0.dat"
                    val path = withContext(Dispatchers.IO) { choosePath(true, "Export game save slot (DAT + FAC)", "dat", exportName, state.sourcePath) } ?: return@launch
                    val result = withContext(Dispatchers.IO) { SaveSlotIO.export(state.document, path) }
                    state.refresh("Game save exported · ${path.fileName} + FAC${if (result.backups.isNotEmpty()) " · ${result.backups.size} recovery backup(s) kept" else ""}")
                } catch (failure: Exception) { error(failure) }
                finally { state.busy = false }
                }
            }
        },
        close = { if (!state.busy) requestGuarded { exitApplication() } },
        recover = {
            val path = state.recoveryPath
            if (!state.busy && path != null) {
                state.busy = true
                scope.launch {
                    try {
                        val (restored, snapshot) = withContext(Dispatchers.IO) {
                            SaveSlotIO.recover(path)
                            val document = SaveSlotIO.load(path)
                            document to document.snapshot()
                        }
                        state.recoveryPath = null
                        state.install(restored, path, preparedSnapshot = snapshot)
                        state.notice = "Previous DAT + FAC pair restored · Recovery backups retained"
                    } catch (failure: Exception) { state.recoveryPath = null; error(failure) }
                    finally { state.busy = false }
                }
            }
        }
    )
    Window(
        onCloseRequest = actions.close,
        title = "${if (state.snapshot.dirty) "● " else ""}${state.snapshot.name.ifBlank { "Untitled" }} — Clash Studio",
        state = rememberWindowState(width = 1440.dp, height = 960.dp),
        onKeyEvent = { event ->
            if (event.type == KeyEventType.KeyDown && event.isCtrlPressed && !state.busy) {
                when (event.key) {
                    Key.O -> { actions.open(); true }
                    Key.N -> { actions.newDocument(); true }
                    Key.S -> { if (event.isShiftPressed) actions.export() else actions.saveProject(); true }
                    Key.Z -> { if (event.isShiftPressed) state.redo() else state.undo(); true }
                    Key.Y -> { state.redo(); true }
                    else -> false
                }
            } else false
        }
    ) {
        LaunchedEffect(Unit) {
            window.minimumSize = java.awt.Dimension(980, 700)
            val smokePath = args.option("--smoke")
            if (smokePath != null) {
                state.busy = true
                try {
                    val path = Path.of(smokePath)
                    val (doc, snapshot) = withContext(Dispatchers.IO) {
                        val document = if (smokePath.endsWith(".clashproj", true)) ProjectIO.load(path) else SaveSlotIO.load(path)
                        document to document.snapshot()
                    }
                    state.install(doc, path, preparedSnapshot = snapshot)
                } catch (failure: Exception) { error(failure) }
                finally { state.busy = false }
            }
            val screenshot = args.option("--screenshot")
            if (screenshot != null) {
                window.isAlwaysOnTop = true
                window.toFront()
                window.requestFocus()
                // Await multiple rendered frames, then capture the actual application window.
                repeat(3) { withFrameNanos { } }
                delay(1800)
                val bounds = window.bounds
                try {
                    withContext(Dispatchers.IO) {
                        val path = Path.of(screenshot).toAbsolutePath()
                        path.parent?.let { Files.createDirectories(it) }
                        ImageIO.write(Robot().createScreenCapture(bounds), "png", path.toFile())
                    }
                    println("COMPOSE_SMOKE_CAPTURED $screenshot")
                } catch (failure: Exception) {
                    System.err.println("COMPOSE_SMOKE_FAILED ${failure.message}")
                }
                exitApplication()
            }
        }
        EditorWindow(state, actions, unsavedChanges.confirmationRequired,
            onDiscard = unsavedChanges::discard,
            onCancelDiscard = unsavedChanges::cancel,
            onSaveAndContinue = { unsavedChanges.saveAndContinue { next -> save(next) } })
    }
}

private fun Array<String>.option(name: String): String? = indexOf(name).takeIf { it >= 0 }?.let { getOrNull(it + 1) }

private fun suggestedName(name: String, extension: String) = name.replace(Regex("[^A-Za-z0-9 _-]"), "").trim().ifBlank { "scenario" } + "." + extension

private fun choosePath(save: Boolean, title: String, extension: String?, filename: String?, previous: Path?): Path? {
    var result: Path? = null
    SwingUtilities.invokeAndWait {
        val chooser = JFileChooser(previous?.parent?.toFile()).apply {
            dialogTitle = title
            isAcceptAllFileFilterUsed = !save
            fileFilter = if (extension == null) FileNameExtensionFilter("Clash save and editor projects", "dat", "clashproj")
            else FileNameExtensionFilter(if (extension == "dat") "Clash save slot (*.dat)" else "Clash editor project (*.clashproj)", extension)
            if (filename != null) selectedFile = java.io.File(filename)
        }
        val choice = if (save) chooser.showSaveDialog(null) else chooser.showOpenDialog(null)
        if (choice == JFileChooser.APPROVE_OPTION) {
            var path = chooser.selectedFile.toPath().toAbsolutePath()
            if (save && extension != null && !path.toString().endsWith(".$extension", true)) path = Path.of("$path.$extension")
            if (save && Files.exists(path)) {
                val message = if (extension == "dat") "Replace ${path.fileName} and its matching FAC? Recovery backups of existing files will be kept." else "Replace ${path.fileName}?"
                val confirmed = javax.swing.JOptionPane.showConfirmDialog(null,
                    message, "Replace existing file", javax.swing.JOptionPane.OK_CANCEL_OPTION)
                if (confirmed != javax.swing.JOptionPane.OK_OPTION) return@invokeAndWait
            }
            result = path
        }
    }
    return result
}
