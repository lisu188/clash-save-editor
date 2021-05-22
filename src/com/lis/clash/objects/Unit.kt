package com.lis.clash.objects

import com.lis.clash.ByteConverter
import com.lis.clash.ClashSimpleProperty

class Unit(parent: ClashObject, index: Int) : ClashObject(parent, index) {
    @ClashSimpleProperty(12, 1)
    var exp: Byte by clashProperty(0)

    @ClashSimpleProperty(11, 1)
    var morale: Byte by clashProperty(0)

    @ClashSimpleProperty(10, 1)
    var shout: Byte by clashProperty(0)

    @ClashSimpleProperty(9, 1)
    var health: Byte by clashProperty(0)

    @ClashSimpleProperty(8, 1)
    var move: Byte by clashProperty(0)

    @ClashSimpleProperty(0, 1)
    var type: Byte by clashProperty(0)

    override fun isValid(): Boolean {
        return type.toInt() != -1
    }

}