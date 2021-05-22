package com.lis.clash.objects

import com.lis.clash.ClashSimpleProperty
import com.lis.clash.StringConverter

class Player(parent: ClashObject, index: Int) : ClashObject(parent, index) {
    @ClashSimpleProperty(0, 10)
    var name: String by clashProperty("")
}