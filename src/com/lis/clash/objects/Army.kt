package com.lis.clash.objects

import com.lis.clash.*

class Army(parent: ClashObject, index: Int) : ClashObject(parent, index) {
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

    override fun isValid(): Boolean {
        return units.isNotEmpty();
    }
}