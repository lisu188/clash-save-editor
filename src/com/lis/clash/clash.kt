package com.lis.clash

import com.lis.clash.objects.Army
import com.lis.clash.objects.Castle
import com.lis.clash.objects.Save
import java.awt.EventQueue
import java.io.File
import javax.swing.GroupLayout
import javax.swing.JComponent
import javax.swing.JFileChooser
import javax.swing.JFrame


private fun parseFile(readBytes: ByteArray): Save {
    return Save().withBytes(readBytes.toList())
}

class ClashSaveEditor(title: String) : JFrame() {

    private lateinit var clashGUI: ClashGUI
    private lateinit var selectionController: SelectionController

    private lateinit var save: Save

    init {
        createUI(title)
    }


    private fun createLayout(vararg arg: JComponent) {
        val gl = GroupLayout(contentPane)
        contentPane.layout = gl

        gl.autoCreateContainerGaps = true

        gl.setHorizontalGroup(
            gl.createSequentialGroup()
                .addComponent(arg[0])
        )

        gl.setVerticalGroup(
            gl.createSequentialGroup()
                .addComponent(arg[0])
        )

        pack()
    }

    private fun createUI(title: String) {
        setTitle(title)
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        setSize(300, 200)
        setLocationRelativeTo(null)

        clashGUI = ClashGUI()

        selectionController = SelectionController().withBytesTable(clashGUI.bytesTable);

        clashGUI.loadButton.addActionListener {

            withFile("E:/Gry/Clash/save") {
                save = parseFile(it.readBytes());

                initializeUnits()

                initializeTiles()

                initializePlayers()

                initializeCastles()

                initializeMap()
            }
        }

        clashGUI.saveButton.addActionListener {
            withFile("E:/Gry/Clash/save") {
                it.writeBytes(save.bytes.toByteArray())
            }
        }

        createLayout(clashGUI.mainPanel)
    }

    private fun withFile(pathName: String, function: (file: File) -> Unit) {
        val fc = JFileChooser();
        fc.currentDirectory = File(pathName);
        val returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            function.invoke(fc.selectedFile)
        }
    }

    private fun initializeCastles() {
        clashGUI.castlesTable.withData { save.castles }.withSubTable(
            clashGUI.castleUnitTable.withSelectionController(selectionController), Castle::units
        )
            .withSelectionController(selectionController)
    }


    private fun initializeMap() {
        clashGUI.mapPanel.tiles = { save.tiles }
    }

    private fun initializeUnits() {
        clashGUI.armyUnitsTable.withData { save.armies }.withSubTable(
            clashGUI.unitTable.withSelectionController(selectionController), Army::units
        )
            .withSelectionController(selectionController)
    }

    private fun initializeTiles() {
        clashGUI.tilesTable.withData { save.tiles }
            .withSelectionController(selectionController)
    }

    private fun initializePlayers() {
        clashGUI.playersTable.withData { save.players }
            .withSelectionController(selectionController)
    }
}


private fun createAndShowGUI() {
    val frame = ClashSaveEditor("Clash Save Editor")
    frame.isVisible = true
}

fun main(args: Array<String>) {
    EventQueue.invokeLater(::createAndShowGUI)
}