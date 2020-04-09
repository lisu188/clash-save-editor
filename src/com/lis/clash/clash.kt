package com.lis.clash

import java.awt.EventQueue
import java.io.File
import java.lang.reflect.Modifier
import javax.swing.GroupLayout
import javax.swing.JComponent
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.table.AbstractTableModel
import javax.swing.table.TableModel
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaGetter


class Unit {
    private var exp: Byte
    private var morale: Byte
    private var shout: Byte
    private var health: Byte
    private var move: Byte
    private var type: Byte
    private var bytes: List<Byte>


    constructor(
        _bytes: List<Byte>
    ) {
        bytes = _bytes
        type = bytes[0]
        move = bytes[8]
        health = bytes[9]
        shout = bytes[10]
        morale = bytes[11]
        exp = bytes[12]
    }
}

class Army {
    @Column(0)
    var x: Byte

    @Column(1)
    var y: Byte

    @Column(2)
    var player: Byte

    @Column(3)
    var dir: Byte

    private var bytes: List<Byte>
    private var units: MutableList<Unit> = mutableListOf()

    constructor(
        _bytes: List<Byte>
    ) {
        bytes = _bytes
        x = bytes[0]
        y = bytes[2]
        player = bytes[4]
        dir = bytes[5]
        for (i in 0 until 10) {
            units.add(i, Unit(bytes.slice(6 + i * 31 until (6 + (i + 1) * 31))))
        }
    }
}


class Save {
    private var name: String
    private var bytes: List<Byte>
    var tiles: MutableList<Tile> = mutableListOf()
    var armies: MutableList<Army> = mutableListOf()


    constructor(
        _bytes: List<Byte>
    ) {
        bytes = _bytes
        name = String(bytes.slice(0 until 16).toByteArray())
        for (i in 0 until 10000) {
            tiles.add(i, Tile(bytes.slice(16 + i * 14 until (16 + (i + 1) * 14))))
        }
        for (i in 0 until 500) {
            armies.add(i, Army(bytes.slice(147190 + i * 725 until (147190 + (i + 1) * 725))))
        }
    }
}

class Tile {
    private var subtype: Byte
    private var bytes: List<Byte>
    private var type: Byte
    private var unknown: Byte
    private var anim: Byte

    constructor(
        _bytes: List<Byte>
    ) {
        bytes = _bytes
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

                val dataModel: TableModel = object : AbstractTableModel() {
                    override fun getColumnName(column: Int): String {
                        return getProperty(column).name
                    }

                    override fun getColumnCount(): Int {
                        return Army::class.memberProperties
                            .filter { it.hasAnnotation<Column>() }
                            .size
                    }

                    override fun getRowCount(): Int {
                        return save.armies.size
                    }

                    override fun getValueAt(row: Int, col: Int): Any? {
                        return getProperty(col)
                            .get(save.armies.get(row))
                    }

                    private fun getProperty(col: Int): KProperty1<Army, *> {
                        return Army::class.memberProperties
                            .filter { it.findAnnotation<Column>()?.no == col }
                            .first()
                    }
                }

                clashGUI.armyTable.model = dataModel
            }
        }


        createLayout(clashGUI.mainPanel)
    }


}

annotation class Column(val no: Int)

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