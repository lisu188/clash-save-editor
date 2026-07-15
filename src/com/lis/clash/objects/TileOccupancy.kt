package com.lis.clash.objects

import com.lis.clash.ClashSimpleProperty
import com.lis.clash.SaveFormat

class TileOccupancy(parent: ClashObject, index: Int) : ClashObject(parent, index) {
    @ClashSimpleProperty(0, 2)
    var rawValue: Int by clashProperty(SaveFormat.EMPTY_OCCUPANCY)

    fun isEmpty(): Boolean = rawValue == SaveFormat.EMPTY_OCCUPANCY

    fun armyStackIndex(): Int? {
        return rawValue.takeIf { it in 0 until SaveFormat.OCCUPANCY_BUILDING_INDEX_BASE }
    }

    fun buildingIndex(): Int? {
        return rawValue
            .takeIf { it in SaveFormat.OCCUPANCY_BUILDING_INDEX_BASE until SaveFormat.EMPTY_OCCUPANCY }
            ?.minus(SaveFormat.OCCUPANCY_BUILDING_INDEX_BASE)
    }
}
