package com.lis.clash.objects

import com.lis.clash.ClashSimpleProperty

class Player(parent: ClashObject, index: Int) : ClashObject(parent, index) {
    @ClashSimpleProperty(0, 10)
    var name: String by clashProperty("")

    @ClashSimpleProperty(53, 1300)
    var explored: List<Byte> by clashProperty(emptyList())
}