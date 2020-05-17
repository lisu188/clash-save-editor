package com.lis.clash.objects

import com.lis.clash.ClashProperty
import com.lis.clash.StringConverter

class Player(parent: ClashObject, index: Int) : ClashObject(parent, index) {
    @ClashProperty(0, 10, StringConverter::class)
    var name: String by clashProperty("")
}