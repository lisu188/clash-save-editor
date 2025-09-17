package com.lis.clash

import com.lis.clash.objects.Army
import com.lis.clash.objects.Castle
import com.lis.clash.objects.Save
import java.awt.EventQueue
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.GroupLayout
import javax.swing.JComponent
import javax.swing.JFileChooser
import javax.swing.JFrame
import kotlin.math.sqrt
import kotlin.reflect.KFunction
import kotlin.reflect.full.functions
import kotlin.reflect.full.hasAnnotation

private fun parseFile(readBytes: ByteArray): Save =
    Save().withBytes(readBytes.toList())

class ClashSaveEditor(title: String) : JFrame() {
    private lateinit var clashGUI: ClashGUI
    private lateinit var selectionController: SelectionController
    private lateinit var save: Save

    /** actual width of the map (√tiles.size) */
    private var mapWidth: Int = 0

    init {
        createUI(title)
    }

    private fun toIndex(x: Int, y: Int): Int =
        y * mapWidth + x

    private fun fromIndex(index: Int): Pair<Int, Int> =
        index % mapWidth to index / mapWidth

    private fun createLayout(vararg arg: JComponent) {
        val gl = GroupLayout(contentPane).apply { autoCreateContainerGaps = true }
        contentPane.layout = gl
        gl.setHorizontalGroup(gl.createSequentialGroup().addComponent(arg[0]))
        gl.setVerticalGroup(gl.createSequentialGroup().addComponent(arg[0]))
        pack()
    }

    private fun createUI(title: String) {
        setTitle(title)
        defaultCloseOperation = EXIT_ON_CLOSE

        clashGUI = ClashGUI()
        selectionController = SelectionController().withBytesTable(clashGUI.bytesTable)

        clashGUI.mapPanel.apply {
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    val x = e.x / tileSize
                    val y = e.y / tileSize
                    val idx = toIndex(x, y)
                    if (idx in save.tiles.indices) {
                        clashGUI.tilesTable.setRowSelectionInterval(idx, idx)
                        clashGUI.tilesTable.scrollRectToVisible(
                            clashGUI.tilesTable.getCellRect(idx, 0, true)
                        )
                    }
                }
            })
        }

        clashGUI.loadButton.addActionListener {
            withFile("E:/Gry/Clash/save") { file ->
                save = parseFile(file.readBytes())
                mapWidth = sqrt(save.tiles.size.toDouble()).toInt()

                initializeUnits()
                initializeTiles()
                initializePlayers()
                initializeCastles()
                initializeMap()

                pack()
            }
        }

        initializeScripts()

        clashGUI.saveButton.addActionListener {
            withFile("E:/Gry/Clash/save") { file ->
                file.writeBytes(save.bytes.toByteArray())
            }
        }

        createLayout(clashGUI.mainPanel)
    }

    private fun initializeScripts() {
        data class FunctionWrapper(val f: KFunction<*>) {
            override fun toString() = f.name
        }
        Scripts::class.functions
            .filter { it.hasAnnotation<ClashScript>() }
            .map(::FunctionWrapper)
            .forEach { clashGUI.scriptBox.addItem(it) }

        clashGUI.executeButton.addActionListener {
            val wrapper = clashGUI.scriptBox.selectedItem as FunctionWrapper
            println(wrapper.f.call(Scripts::class.objectInstance, save))
        }
    }

    private fun withFile(pathName: String, fn: (File) -> Unit) {
        JFileChooser().apply {
            currentDirectory = File(pathName)
            if (showOpenDialog(this@ClashSaveEditor) == JFileChooser.APPROVE_OPTION) {
                fn(selectedFile)
            }
        }
    }

    private fun initializeUnits() {
        clashGUI.armyUnitsTable
            .withData { save.armies }
            .withSubTable(
                clashGUI.unitTable.withSelectionController(selectionController),
                Army::units
            )
            .withSelectionController(selectionController)
    }

    private fun initializeCastles() {
        clashGUI.castlesTable
            .withData { save.castles }
            .withSubTable(
                clashGUI.castleUnitTable.withSelectionController(selectionController),
                Castle::units
            )
            .withSelectionController(selectionController)
    }

    private fun initializeTiles() {
        clashGUI.tilesTable
            .withData { save.tiles }
            .withSelectionController(selectionController)
            .withSelectionListener { idx ->
                val (x, y) = fromIndex(idx)
                clashGUI.getxTile().text = x.toString()
                clashGUI.getyTile().text = y.toString()
            }

        listOf(clashGUI.getxTile(), clashGUI.getyTile()).forEach { field ->
            field.addActionListener {
                val x = clashGUI.getxTile().text.toIntOrNull() ?: return@addActionListener
                val y = clashGUI.getyTile().text.toIntOrNull() ?: return@addActionListener
                val idx = toIndex(x, y)
                if (idx in save.tiles.indices) {
                    clashGUI.tilesTable.setRowSelectionInterval(idx, idx)
                    clashGUI.tilesTable.scrollRectToVisible(
                        clashGUI.tilesTable.getCellRect(idx, 0, true)
                    )
                }
            }
        }
    }

    private fun initializePlayers() {
        clashGUI.playersTable
            .withData { save.players }
            .withSelectionController(selectionController)
    }

    private fun initializeMap() {
        clashGUI.mapPanel.tilesSupplier = { save.tiles }
    }
}

private fun createAndShowGUI() {
    ClashSaveEditor("Clash Save Editor").apply { isVisible = true }
}

fun main() = EventQueue.invokeLater(::createAndShowGUI)
