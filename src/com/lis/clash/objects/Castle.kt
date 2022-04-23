package com.lis.clash.objects

import com.lis.clash.ClashAggregateProperty
import com.lis.clash.ClashSimpleProperty

class Castle(parent: ClashObject, index: Int) : ClashObject(parent, index) {
    @ClashSimpleProperty(0, 1)
    var x: Byte by clashProperty(0)

    @ClashSimpleProperty(1, 1)
    var y: Byte by clashProperty(0)

    @ClashSimpleProperty(2, 1)
    var player: Byte by clashProperty(0)

    @ClashSimpleProperty(3, 1)
    var appearance: Byte by clashProperty(0)

    @ClashSimpleProperty(4, 1)
    var type: Byte by clashProperty(0)

    @ClashSimpleProperty(5, 10)
    var name: String by clashProperty("")

    @ClashAggregateProperty(18, 12, 31, Unit::class)
    var units: List<Unit> by clashProperty(emptyList())

    @ClashSimpleProperty(402, 12)
    var unitsToBuild: List<Byte> by clashProperty(emptyList())

    @ClashSimpleProperty(430, 1)
    var peasants: Byte by clashProperty(0)

    @ClashSimpleProperty(434, 1)
    var hapiness: Byte by clashProperty(0)

    @ClashSimpleProperty(438, 1)
    var gold: Byte by clashProperty(0)

    //1 hospital
    //2 barrracks
    //4 workshop
    //8 school
    @ClashSimpleProperty(416, 1)
    var building: Byte by clashProperty(0)

    @ClashSimpleProperty(420, 1)
    var canBuild: Byte by clashProperty(0)

    @ClashSimpleProperty(436, 1)
    var tax: Byte by clashProperty(0)

    @ClashSimpleProperty(425, 1)
    var walls: Byte by clashProperty(0)

    override fun isValid(): Boolean {
        return type.toInt() != -1
    }
}