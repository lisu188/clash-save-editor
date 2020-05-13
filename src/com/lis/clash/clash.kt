package com.lis.clash

import java.awt.EventQueue
import java.io.File
import javax.swing.GroupLayout
import javax.swing.JComponent
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.table.TableModel
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties


open class ClashObject() {

    private var bytes: List<Byte> by Delegates.observable(listOf(), { _, oldValue, newValue ->
        if (oldValue != newValue) {
            onBytesChanged()
        }
    })


    public fun <T> clashProperty(
        initialValue: T
    ): ReadWriteProperty<Any?, T> {
        return Delegates.observable(initialValue, { property, oldValue, newValue ->
            if (oldValue != newValue && property.hasAnnotation<ClashProperty>()) {
                val annotation = getAnnotation<ClashProperty>(property)
                val toBytes = getConverter(annotation).toBytes(newValue!!)
                val start = bytes.subList(0, annotation.index)
                val mid =
                    if (toBytes.size < annotation.length) toBytes + List(annotation.length - toBytes.size) { -1 } else toBytes
                val end = bytes.subList(annotation.index + annotation.length, bytes.size)
                bytes = start + mid + end
            }
        })
    }

    private fun onBytesChanged() {
        this::class.memberProperties
            .filter { it.hasAnnotation<ClashProperty>() }
            .map { it as KMutableProperty<*> }
            .forEach {
                val annotation = getAnnotation<ClashProperty>(it)
                val propertyBytes = bytes.slice(annotation.index until annotation.index + annotation.length)
                val propertyValue = getConverter(annotation).fromBytes(propertyBytes)
                it.setter.call(this, propertyValue)
            }

        this::class.memberProperties
            .filter { it.hasAnnotation<ClashAggregateProperty>() }
            .map { it as KMutableProperty<*> }
            .forEach {
                val annotation = getAnnotation<ClashAggregateProperty>(it)
                val list = mutableListOf<ClashObject>()
                for (i in 0 until annotation.count) {
                    list.add(
                        annotation.clas.createInstance().withBytes(
                            bytes.slice(
                                annotation.index + i * annotation.size until annotation.index + (i + 1) * annotation.size
                            )
                        )
                    )
                }
                it.setter.call(this, list)
            }
    }

    private inline fun <reified T : Annotation> getAnnotation(it: KProperty<*>) = it.findAnnotation<T>()!!

    private fun getConverter(annotation: ClashProperty?) = annotation?.converter?.objectInstance!!

    fun <T> withBytes(slice: List<Byte>): T {
        bytes = slice
        return this as T
    }
}

class Unit() : ClashObject() {
    @ClashProperty(12, 1, ByteConverter::class)
    var exp: Byte by clashProperty(0)

    @ClashProperty(11, 1, ByteConverter::class)
    var morale: Byte by clashProperty(0)

    @ClashProperty(10, 1, ByteConverter::class)
    var shout: Byte by clashProperty(0)

    @ClashProperty(9, 1, ByteConverter::class)
    var health: Byte by clashProperty(0)

    @ClashProperty(8, 1, ByteConverter::class)
    var move: Byte by clashProperty(0)

    @ClashProperty(0, 1, ByteConverter::class)
    var type: Byte by clashProperty(0)

}

class Player : ClashObject() {

    @ClashProperty(0, 10, StringConverter::class)
    var name: String by clashProperty("")
}

class Army : ClashObject() {
    @ClashProperty(0, 1, ByteConverter::class)
    var x: Byte by clashProperty(0)

    @ClashProperty(2, 1, ByteConverter::class)
    var y: Byte by clashProperty(0)

    @ClashProperty(4, 1, ByteConverter::class)
    var player: Byte by clashProperty(0)

    @ClashProperty(5, 1, ByteConverter::class)
    var dir: Byte by clashProperty(0)

    @ClashAggregateProperty(6, 10, 31, Unit::class)
    var units: List<Unit> by clashProperty(emptyList())
}


class Save() : ClashObject() {
    @ClashProperty(0, 16, StringConverter::class)
    var name: String by clashProperty("")

    @ClashAggregateProperty(16, 10000, 14, Tile::class)
    var tiles: List<Tile> by clashProperty(emptyList())

    @ClashAggregateProperty(147190, 500, 725, Army::class)
    var armies: List<Army> by clashProperty(emptyList())

    @ClashAggregateProperty(140044, 5, 1423, Player::class)
    var players: List<Player> by clashProperty(emptyList())

    @ClashAggregateProperty(509690, 10, 467, Castle::class)
    var castles: List<Castle> by clashProperty(emptyList())
}

class Castle() : ClashObject() {
    @ClashProperty(2, 1, ByteConverter::class)
    var player: Byte by clashProperty(0)

    @ClashProperty(4, 1, ByteConverter::class)
    var type: Byte by clashProperty(0)

    @ClashProperty(3, 1, ByteConverter::class)
    var appearance: Byte by clashProperty(0)

    @ClashProperty(5, 10, StringConverter::class)
    var name: String by clashProperty("")

    @ClashAggregateProperty(18, 12, 31, Unit::class)
    var units: List<Unit> by clashProperty(emptyList())
}

class Tile() : ClashObject() {
    @ClashProperty(2, 1, ByteConverter::class)
    var subtype: Byte by clashProperty(0)

    @ClashProperty(0, 1, ByteConverter::class)
    var type: Byte by clashProperty(0)

    @ClashProperty(4, 1, ByteConverter::class)
    var unknown: Byte by clashProperty(0)

    @ClashProperty(6, 1, ByteConverter::class)
    var anim: Byte by clashProperty(0)

}

private fun parseFile(readBytes: ByteArray): Save {
    return Save().withBytes(readBytes.toList())
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

                initializePlayers(save, clashGUI)

                initializeCastles(save, clashGUI)
            }
        }


        createLayout(clashGUI.mainPanel)
    }

    private fun initializeCastles(save: Save, clashGUI: ClashGUI) {
        val dataModel: TableModel = buildTable(save.castles)

        clashGUI.castlesTable.model = dataModel

        clashGUI.castlesTable.selectionModel.addListSelectionListener {
            clashGUI.castleUnitTable.model = buildTable(save.castles[clashGUI.castlesTable.selectedRow].units)
        }

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

    private fun initializePlayers(save: Save, clashGUI: ClashGUI) {
        val dataModel: TableModel = buildTable(save.players)

        clashGUI.playersTable.model = dataModel
    }
}


private fun createAndShowGUI() {
    val frame = ClashSaveEditor("Clash Save Editor")
    frame.isVisible = true
}

fun main(args: Array<String>) {
    EventQueue.invokeLater(::createAndShowGUI)
}