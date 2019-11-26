package com.lis.clash

import java.awt.EventQueue
import java.awt.event.ActionListener
import javax.swing.*

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
    private var x: Byte
    private var bytes: List<Byte>
    private var y: Byte
    private var player: Byte
    private var dir: Byte
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
    private var bytes: List<Byte>
    var tiles: MutableList<Tile> = mutableListOf()
    var armies: MutableList<Army> = mutableListOf()

    constructor(
        _bytes: List<Byte>
    ) {
        bytes = _bytes
        for (i in 0 until 10000) {
            tiles.add(i, Tile(bytes.slice(i * 14 until ((i + 1) * 14))))
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

        val openButton = JButton("Open")
        openButton.addActionListener(ActionListener {
            //Create a file chooser
            val fc = JFileChooser();
            val returnVal = fc.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                val file = fc.selectedFile
                val save = parseFile(file.readBytes());
                print(save)
            }
        })

        createLayout(openButton)
    }


}

private fun createAndShowGUI() {
    val frame = ClashSaveEditor("Clash Save Editor")
    frame.isVisible = true
}

fun main(args: Array<String>) {
    EventQueue.invokeLater(::createAndShowGUI)
}