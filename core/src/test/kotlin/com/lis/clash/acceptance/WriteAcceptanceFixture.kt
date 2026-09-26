package com.lis.clash.acceptance

import com.lis.clash.editor.*
import java.nio.file.Path

/** Uses public editor commands, never a separately assembled substitute save. */
fun main(args: Array<String>) {
    val destination = Path.of(args.single()).toAbsolutePath()
    val document = SaveDocument.newScenario("Compose test")
    document.execute(EditCommand.ConfigurePlayer(0, true, true, name = "North"))
    document.execute(EditCommand.ConfigurePlayer(1, true, true, name = "South"))
    document.execute(EditCommand.CreateArmy(4, 4, 0, 1))
    document.execute(EditCommand.CreateArmy(12, 12, 1, 1))
    document.execute(EditCommand.CreateBuilding(5, 6, 0, 2, "North Keep"))
    document.execute(EditCommand.CreateBuilding(12, 14, 1, 2, "South Keep"))
    ProjectIO.save(document, destination.resolveSibling("acceptance.clashproj"))
    val result = SaveSlotIO.export(document, destination)
    println("ACCEPTANCE_DAT=${result.datPath}")
    println("ACCEPTANCE_FAC=${result.facPath}")
    println("VALIDATION=${document.validate(forExport = true)}")
}
