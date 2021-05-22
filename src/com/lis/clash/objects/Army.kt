package com.lis.clash.objects

import com.lis.clash.*

class Army(parent: ClashObject, index: Int) : ClashObject(parent, index) {
    @ClashSimpleProperty(0, 1)
    var x: Byte by clashProperty(0)

    @ClashSimpleProperty(2, 1)
    var y: Byte by clashProperty(0)

    @ClashSimpleProperty(4, 1)
    var player: Byte by clashProperty(0)

    @ClashSimpleProperty(5, 1)
    var dir: Byte by clashProperty(0)

    @ClashAggregateProperty(6, 10, 31, Unit::class)
    var units: List<Unit> by clashProperty(emptyList())

    override fun isValid(): Boolean {
        return units.isNotEmpty();
    }
}