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
import kotlin.reflect.KFunction
import kotlin.reflect.full.functions
import kotlin.reflect.full.hasAnnotation

private fun parseFile(readBytes: ByteArray): Save {
    return Save.parse(readBytes)
}

class ClashSaveEditor(title: String) : JFrame() {

    private lateinit var clashGUI: ClashGUI
    private lateinit var selectionController: SelectionController

    private lateinit var save: Save
    private var tileNavigationBound = false

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
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(300, 200)
        setLocationRelativeTo(null)

        clashGUI = ClashGUI()

        selectionController = SelectionController().withBytesTable(clashGUI.bytesTableView())

        clashGUI.loadButton.addActionListener {
            withFile("E:/Gry/Clash/save") {
                save = parseFile(it.readBytes())

                initializeUnits()

                initializeTiles()

                initializePlayers()

                initializeCastles()

                initializeMap()
            }
        }

        initializeScripts()

        clashGUI.saveButton.addActionListener {
            withFile("E:/Gry/Clash/save") {
                it.writeBytes(save.bytes.toByteArray())
            }
        }

        createLayout(clashGUI.mainPanel)
    }

    private fun initializeScripts() {
        class FunctionWrapper(val function: KFunction<*>) {
            override fun toString(): String {
                return function.name
            }
        }
        Scripts::class.functions
            .filter { it.hasAnnotation<ClashScript>() }
            .sortedBy { it.name }
            .forEach {
                clashGUI.scriptBox.addItem(FunctionWrapper(it))
            }

        clashGUI.executeButton.addActionListener {
            if (!::save.isInitialized) {
                println("Load a save before executing scripts")
                return@addActionListener
            }
            println(
                (clashGUI.scriptBox.selectedItem as FunctionWrapper).function.call(
                    Scripts::class.objectInstance,
                    save
                )
            )
        }
    }

    private fun withFile(pathName: String, function: (file: File) -> Unit) {
        val fc = JFileChooser()
        fc.currentDirectory = File(pathName)
        val returnVal = fc.showOpenDialog(this)
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            function.invoke(fc.selectedFile)
        }
    }

    private fun initializeCastles() {
        clashGUI.castlesTableView().withData { save.castles }.withSubTable(
            clashGUI.castleUnitTableView().withSelectionController(selectionController), Castle::units
        )
            .withSelectionController(selectionController)
    }

    private fun initializeMap() {
        clashGUI.mapPanelView().tiles = { save.tiles }
        clashGUI.mapPanelView().mapWidth = { save.mapWidthTiles.takeIf { it > 0 } ?: 100 }
        clashGUI.mapPanelView().mapHeight = { save.mapHeightTiles.takeIf { it > 0 } ?: 100 }
        clashGUI.mapPanelView().selectedTileIndex = { clashGUI.tilesTableView().selectedRow }
        clashGUI.mapPanelView().onTileSelected = { tileIndex ->
            selectTile(tileIndex)
        }
        clashGUI.mapPanelView().revalidate()
        clashGUI.mapPanelView().repaint()
    }

    private fun initializeUnits() {
        clashGUI.armyUnitsTableView().withData { save.armies }.withSubTable(
            clashGUI.unitTableView().withSelectionController(selectionController), Army::units
        )
            .withSelectionController(selectionController)
    }

    private fun initializeTiles() {
        clashGUI.tilesTableView().withData { save.tiles }
            .withSelectionController(selectionController)
            .withSelectionListener {
                if (it >= 0) {
                    val (tileRow, tileColumn) = fromIndex(it, save.mapWidthTiles.takeIf { width -> width > 0 } ?: 100)
                    clashGUI.getxTile().text = tileRow.toString()
                    clashGUI.getyTile().text = tileColumn.toString()
                }
                clashGUI.mapPanelView().repaint()
            }

        if (!tileNavigationBound) {
            val tileNavigator = fun() {
                if (!::save.isInitialized) {
                    return
                }
                val mapWidth = save.mapWidthTiles.takeIf { it > 0 } ?: 100
                val tileIndex = toIndex(clashGUI.getxTile().text.toInt(), clashGUI.getyTile().text.toInt(), mapWidth)
                selectTile(tileIndex)
            }
            clashGUI.getxTile().addActionListener { tileNavigator() }
            clashGUI.getyTile().addActionListener { tileNavigator() }
            tileNavigationBound = true
        }
    }

    private fun initializePlayers() {
        clashGUI.playersTableView().withData { save.players }
            .withSelectionController(selectionController)
    }

    private fun selectTile(tileIndex: Int) {
        if (tileIndex !in save.tiles.indices) {
            return
        }
        clashGUI.tilesTableView().setRowSelectionInterval(tileIndex, tileIndex)
        clashGUI.tilesTableView().scrollRectToVisible(clashGUI.tilesTableView().getCellRect(tileIndex, 0, true))
        clashGUI.mapPanelView().repaint()
    }
}

private fun ClashGUI.armyUnitsTableView(): ClashTable = armyUnitsTable as ClashTable

private fun ClashGUI.unitTableView(): ClashTable = unitTable as ClashTable

private fun ClashGUI.mapPanelView(): MapPanel = mapPanel as MapPanel

private fun ClashGUI.tilesTableView(): ClashTable = tilesTable as ClashTable

private fun ClashGUI.playersTableView(): ClashTable = playersTable as ClashTable

private fun ClashGUI.castlesTableView(): ClashTable = castlesTable as ClashTable

private fun ClashGUI.castleUnitTableView(): ClashTable = castleUnitTable as ClashTable

private fun ClashGUI.bytesTableView(): BytesTable = bytesTable as BytesTable

private fun createAndShowGUI() {
    val frame = ClashSaveEditor("Clash Save Editor")
    frame.isVisible = true
}

fun main() {
    EventQueue.invokeLater(::createAndShowGUI)
}
