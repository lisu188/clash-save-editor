package com.lis.clash.objects

import com.lis.clash.ClashAggregateProperty
import com.lis.clash.ClashSimpleProperty

class Castle(parent: ClashObject, index: Int) : ClashObject(parent, index) {
    @ClashSimpleProperty(2, 1)
    var player: Byte by clashProperty(0)

    @ClashSimpleProperty(4, 1)
    var type: Byte by clashProperty(0)

    @ClashSimpleProperty(3, 1)
    var appearance: Byte by clashProperty(0)

    @ClashSimpleProperty(5, 10)
    var name: String by clashProperty("")

    @ClashAggregateProperty(18, 12, 31, Unit::class)
    var units: List<Unit> by clashProperty(emptyList())

    override fun isValid(): Boolean {
        return type.toInt() != -1
    }
}