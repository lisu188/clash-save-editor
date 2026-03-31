package com.lis.clash.objects

import com.lis.clash.ClashSignedProperty
import com.lis.clash.ClashSimpleProperty

class Unit(parent: ClashObject, index: Int) : ClashObject(parent, index) {
    @ClashSignedProperty(0, 2)
    var typeId: Int by clashProperty(0)

    @ClashSimpleProperty(2, 1)
    var ownerPlayerIndex: Int by clashProperty(0)

    @ClashSimpleProperty(8, 1)
    var currentActionPoints: Int by clashProperty(0)

    @ClashSimpleProperty(9, 1)
    var currentHealthPercent: Int by clashProperty(0)

    @ClashSimpleProperty(10, 1)
    var fatigue: Int by clashProperty(0)

    @ClashSimpleProperty(11, 1)
    var morale: Int by clashProperty(0)

    @ClashSimpleProperty(12, 1)
    var stanceBits: Int by clashProperty(0)

    @ClashSimpleProperty(13, 1)
    var stateFlags: Int by clashProperty(0)

    @ClashSimpleProperty(18, 4)
    var auxRuntimeState: Int by clashProperty(0)

    @ClashSimpleProperty(22, 1)
    var stateBits2: Int by clashProperty(0)

    override fun isValid(): Boolean {
        return typeId != -1
    }

}
