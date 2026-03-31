package com.lis.clash.objects

import com.lis.clash.ClashAggregateProperty
import com.lis.clash.ClashSimpleProperty

class Save : ClashObject(null, 0) {
    @ClashSimpleProperty(0, 16)
    var name: String by clashProperty("")

    @ClashAggregateProperty(16, 10000, 14, Tile::class)
    var tiles: List<Tile> by clashProperty(emptyList())

    @ClashSimpleProperty(140000, 4)
    var mapWidthTiles: Int by clashProperty(0)

    @ClashSimpleProperty(140004, 4)
    var mapHeightTiles: Int by clashProperty(0)

    @ClashSimpleProperty(140008, 4)
    var mapViewLeft: Int by clashProperty(0)

    @ClashSimpleProperty(140012, 4)
    var mapViewTop: Int by clashProperty(0)

    @ClashSimpleProperty(140017, 4)
    var activeMissionIndex: Int by clashProperty(0)

    @ClashAggregateProperty(147190, 500, 725, Army::class)
    var armies: List<Army> by clashProperty(emptyList())

    @ClashAggregateProperty(140040, 5, 1423, Player::class)
    var players: List<Player> by clashProperty(emptyList())

    @ClashSimpleProperty(147139, 4)
    var turnOwnerPlayerIndex: Int by clashProperty(0)

    @ClashSimpleProperty(147143, 4)
    var viewedPlayerIndex: Int by clashProperty(0)

    @ClashAggregateProperty(509690, 10, 467, Castle::class)
    var castles: List<Castle> by clashProperty(emptyList())
}
