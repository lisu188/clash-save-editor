package com.lis.clash.objects

import com.lis.clash.ByteConverter
import com.lis.clash.ClashProperty

class Unit(parent: ClashObject, index: Int) : ClashObject(parent, index) {
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

    override fun isValid(): Boolean {
        return type.toInt() != -1
    }

}