package com.lis.clash

import java.awt.EventQueue
import java.io.File
import java.lang.reflect.Modifier
import javax.swing.GroupLayout
import javax.swing.JComponent
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.table.TableModel
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaGetter


class Unit(_bytes: List<Byte>) {

    @Column(5)
    var exp: Byte

    @Column(4)
    var morale: Byte

    @Column(3)
    var shout: Byte

    @Column(2)
    var health: Byte

    @Column(1)
    var move: Byte

    @Column(0)
    var type: Byte

    @Column(6)
    var bytes: List<Byte> = _bytes


    init {
        type = bytes[0]
        move = bytes[8]
        health = bytes[9]
        shout = bytes[10]
        morale = bytes[11]
        exp = bytes[12]
    }
}

class Army(_bytes: List<Byte>) {
    @Column(0)
    var x: Byte

    @Column(1)
    var y: Byte

    @Column(2)
    var player: Byte

    @Column(3)
    var dir: Byte

    @Column(4)
    var bytes: List<Byte> = _bytes

    var units: MutableList<Unit> = mutableListOf()

    init {
        x = bytes[0]
        y = bytes[2]
        player = bytes[4]
        dir = bytes[5]
        for (i in 0 until 10) {
            val element = Unit(bytes.slice(6 + i * 31 until (6 + (i + 1) * 31)))
            if (element.type.compareTo(-1) == 0) {
                break;
            }
            units.add(i, element)
        }
    }
}


class Save(_bytes: List<Byte>) {
    private var name: String
    private var bytes: List<Byte> = _bytes
    var tiles: MutableList<Tile> = mutableListOf()
    var armies: MutableList<Army> = mutableListOf()


    init {
        name = String(bytes.slice(0 until 16).toByteArray())
        for (i in 0 until 10000) {
            tiles.add(i, Tile(bytes.slice(16 + i * 14 until (16 + (i + 1) * 14))))
        }
        for (i in 0 until 500) {
            val element = Army(bytes.slice(147190 + i * 725 until (147190 + (i + 1) * 725)))
            if (element.units.size == 0) {
                break;
            }
            armies.add(i, element)
        }
    }
}

class Tile(_bytes: List<Byte>) {
    @Column(1)
    var subtype: Byte

    @Column(4)
    var bytes: List<Byte> = _bytes

    @Column(0)
    var type: Byte

    @Column(2)
    var unknown: Byte

    @Column(3)
    var anim: Byte

    init {
        type = bytes[0]
        subtype = bytes[2]
        unknown = bytes[4]
        anim = bytes[6]
    }
}

private fun parseFile(readBytes: ByteArray): Save {
    return Save(readBytes.toList())
}

class ClashSaveEditor(title: String) : JFrame() {

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

        val clashGUI = ClashGUI()
        val openButton = clashGUI.getLoadButton();
        openButton.addActionListener {
            //Create a file chooser
            val fc = JFileChooser();
            fc.currentDirectory = File("./save");
            val returnVal = fc.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                val file = fc.selectedFile
                val save = parseFile(file.readBytes());

                initializeUnits(save, clashGUI)

                initializeMap(save, clashGUI)

                initializeTiles(save, clashGUI)
            }
        }


        createLayout(clashGUI.mainPanel)
    }

    private fun initializeMap(save: Save, clashGUI: ClashGUI) {
        clashGUI.mapPanel.tiles = save.tiles
    }

    private fun initializeUnits(save: Save, clashGUI: ClashGUI) {
        val dataModel: TableModel = buildTable(save.armies)

        clashGUI.armyTable.model = dataModel

        clashGUI.armyTable.selectionModel.addListSelectionListener {
            clashGUI.unitTable.model = buildTable(save.armies[clashGUI.armyTable.selectedRow].units)
        }
    }

    private fun initializeTiles(save: Save, clashGUI: ClashGUI) {
        val dataModel: TableModel = buildTable(save.tiles)

        clashGUI.tilesTable.model = dataModel
    }
}


fun isFieldAccessible(property: KProperty1<*, *>): Boolean {
    return property.javaGetter?.modifiers?.let { !Modifier.isPrivate(it) } ?: false
}

private fun createAndShowGUI() {
    val frame = ClashSaveEditor("Clash Save Editor")
    frame.isVisible = true
}

fun main(args: Array<String>) {
    EventQueue.invokeLater(::createAndShowGUI)
}