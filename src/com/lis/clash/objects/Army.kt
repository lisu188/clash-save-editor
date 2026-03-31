package com.lis.clash.objects

import com.lis.clash.*

class Army(parent: ClashObject, index: Int) : ClashObject(parent, index) {
    @ClashSimpleProperty(0, 2)
    var tileRow: Int by clashProperty(0)

    @ClashSimpleProperty(2, 2)
    var tileColumn: Int by clashProperty(0)

    @ClashSimpleProperty(4, 1)
    var ownerPlayerIndex: Int by clashProperty(0)

    @ClashSimpleProperty(5, 1)
    var facingDirection: Int by clashProperty(0)

    @ClashAggregateProperty(6, 10, 31, Unit::class)
    var units: List<Unit> by clashProperty(emptyList())

    @ClashSimpleProperty(316, 4)
    var queuedPathWaypointCount: Int by clashProperty(0)

    @ClashSimpleProperty(720, 1)
    var isHiddenOnWorldMap: Int by clashProperty(0)

    fun queuedPathWaypoints(): List<QueuedPathWaypoint> {
        val count = queuedPathWaypointCount.coerceIn(0, 100)
        return List(count) { waypointIndex ->
            val baseIndex = 320 + waypointIndex * 4
            QueuedPathWaypoint(
                tileRow = bytes[baseIndex].toUByte().toInt(),
                tileColumn = bytes[baseIndex + 1].toUByte().toInt(),
                cumulativeCost = readLittleEndianInt(bytes.slice(baseIndex + 2 until baseIndex + 4))
            )
        }
    }

    override fun isValid(): Boolean {
        return units.isNotEmpty()
    }
}
