package com.lis.clash.objects

import com.lis.clash.ByteConverter
import com.lis.clash.ClashProperty

class Tile(parent: ClashObject, index: Int) : ClashObject(parent, index) {
    @ClashProperty(2, 1, ByteConverter::class)
    var subtype: Byte by clashProperty(0)

    @ClashProperty(0, 1, ByteConverter::class)
    var type: Byte by clashProperty(0)

    @ClashProperty(4, 1, ByteConverter::class)
    var unknown: Byte by clashProperty(0)

    @ClashProperty(6, 1, ByteConverter::class)
    var anim: Byte by clashProperty(0)

}