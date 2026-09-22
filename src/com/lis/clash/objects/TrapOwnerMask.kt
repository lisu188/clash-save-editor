package com.lis.clash.objects

import com.lis.clash.ClashSimpleProperty

class TrapOwnerMask(parent: ClashObject, index: Int) : ClashObject(parent, index) {
    @ClashSimpleProperty(0, 1)
    var ownerMask: Int by clashProperty(0)

    fun isKnownToPlayer(playerIndex: Int): Boolean {
        return playerIndex in 0..7 && ownerMask and (1 shl playerIndex) != 0
    }
}
