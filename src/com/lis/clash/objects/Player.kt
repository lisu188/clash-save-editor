package com.lis.clash.objects

import com.lis.clash.ClashMaskedProperty
import com.lis.clash.ClashSimpleProperty

class Player(parent: ClashObject, index: Int) : ClashObject(parent, index) {
    @ClashSimpleProperty(0, 4)
    var isActive: Int by clashProperty(0)

    @ClashSimpleProperty(4, 11)
    var displayName: String by clashProperty("")

    @ClashSimpleProperty(15, 4)
    var cameraLeft: Int by clashProperty(0)

    @ClashSimpleProperty(19, 4)
    var cameraTop: Int by clashProperty(0)

    @ClashSimpleProperty(23, 4)
    var minimapVisibleFlag: Int by clashProperty(0)

    @ClashSimpleProperty(27, 4)
    var controllerMode: Int by clashProperty(0)

    @ClashSimpleProperty(39, 4)
    var religionFlag: Int by clashProperty(0)

    @ClashMaskedProperty(47, 1, 0x07)
    var techLevel: Int by clashProperty(0)

    @ClashMaskedProperty(48, 1, 0x07)
    var lastReportedTechLevel: Int by clashProperty(0)

    @ClashSimpleProperty(49, 4)
    var battleActionTakenFlag: Int by clashProperty(0)

    @ClashSimpleProperty(53, 4)
    var consecutiveIdleBattleTurns: Int by clashProperty(0)

    @ClashSimpleProperty(57, 1300)
    var revealedTilesBitset: List<Byte> by clashProperty(emptyList())
}
