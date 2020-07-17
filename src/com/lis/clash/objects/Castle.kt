package com.lis.clash.objects

import com.lis.clash.*

class Castle(parent: ClashObject, index: Int) : ClashObject(parent, index) {
    @ClashSimpleProperty(2, 1, ByteConverter::class)
    var player: Byte by clashProperty(0)

    @ClashSimpleProperty(4, 1, ByteConverter::class)
    var type: Byte by clashProperty(0)

    @ClashSimpleProperty(3, 1, ByteConverter::class)
    var appearance: Byte by clashProperty(0)

    @ClashSimpleProperty(5, 10, StringConverter::class)
    var name: String by clashProperty("")

    @ClashAggregateProperty(18, 12, 31, Unit::class)
    var units: List<Unit> by clashProperty(emptyList())

    override fun isValid(): Boolean {
        return type.toInt() != -1
    }
}