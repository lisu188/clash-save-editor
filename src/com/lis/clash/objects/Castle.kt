package com.lis.clash.objects

import com.lis.clash.ClashAggregateProperty
import com.lis.clash.ClashMaskedProperty
import com.lis.clash.ClashSimpleProperty

class Castle(parent: ClashObject, index: Int) : ClashObject(parent, index) {
    companion object {
        // Derived from clash95.c / clash-disassembly building facts.
        const val BUILDING_HOSPITAL = 1
        const val BUILDING_BARRACKS = 2
        const val BUILDING_WORKSHOP = 4
        const val BUILDING_SCHOOL = 8
        const val BUILDING_SMITHS = 16
    }

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
    var addonTypeIds: List<Byte> by clashProperty(emptyList())

    @ClashSimpleProperty(414, 1)
    var selectedAddonSlotIndex: Int by clashProperty(0)

    @ClashSimpleProperty(416, 1)
    var castleAddonFlags: Int by clashProperty(0)

    @ClashSimpleProperty(420, 1)
    var constructionLockFlags: Int by clashProperty(0)

    @ClashSimpleProperty(421, 1)
    var wallStrength: Int by clashProperty(0)

    @ClashSimpleProperty(429, 1)
    var upgradeTimerTurns: Int by clashProperty(0)

    @ClashMaskedProperty(430, 2, 0x0FFF)
    var peasantCount: Int by clashProperty(0)

    @ClashSimpleProperty(434, 1)
    var satisfaction: Int by clashProperty(0)

    @ClashMaskedProperty(435, 1, 0x07)
    var plagueState: Int by clashProperty(0)

    @ClashMaskedProperty(436, 1, 0x3F)
    var taxRate: Int by clashProperty(0)

    @ClashSimpleProperty(438, 4)
    var storedMoney: Int by clashProperty(0)

    @ClashMaskedProperty(444, 1, 0x07)
    var techLevelBits: Int by clashProperty(0)

    @ClashSimpleProperty(463, 4)
    var castleFactId: Int by clashProperty(0)

    fun hasBuilding(flag: Int): Boolean {
        return (castleAddonFlags and flag) != 0
    }

    fun buildingNames(): List<String> {
        val result = mutableListOf<String>()
        if (hasBuilding(BUILDING_HOSPITAL)) result += "hospital"
        if (hasBuilding(BUILDING_BARRACKS)) result += "barracks"
        if (hasBuilding(BUILDING_WORKSHOP)) result += "workshop"
        if (hasBuilding(BUILDING_SCHOOL)) result += "school"
        if (hasBuilding(BUILDING_SMITHS)) result += "smiths"
        return result
    }

    override fun isValid(): Boolean {
        return type.toInt() != -1
    }
}
