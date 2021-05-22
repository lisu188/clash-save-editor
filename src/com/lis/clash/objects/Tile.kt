package com.lis.clash.objects

import com.lis.clash.ClashSimpleProperty

class Tile(parent: ClashObject, index: Int) : ClashObject(parent, index) {

    @ClashSimpleProperty(0, 1)
    var type1: Byte by clashProperty(0)

    @ClashSimpleProperty(1, 1)
    var type2: Byte by clashProperty(0)

    @ClashSimpleProperty(2, 1)
    var type3: Byte by clashProperty(0)

    @ClashSimpleProperty(3, 1)
    var type4: Byte by clashProperty(0)

}