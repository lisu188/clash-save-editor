package com.lis.clash.objects

import com.lis.clash.ClashAggregateProperty
import com.lis.clash.ClashSimpleProperty
import com.lis.clash.StringConverter

class Save : ClashObject(null, 0) {
    @ClashSimpleProperty(0, 16, StringConverter::class)
    var name: String by clashProperty("")

    @ClashAggregateProperty(16, 10000, 14, Tile::class)
    var tiles: List<Tile> by clashProperty(emptyList())

    @ClashAggregateProperty(147190, 500, 725, Army::class)
    var armies: List<Army> by clashProperty(emptyList())

    @ClashAggregateProperty(140044, 5, 1423, Player::class)
    var players: List<Player> by clashProperty(emptyList())

    @ClashAggregateProperty(509690, 10, 467, Castle::class)
    var castles: List<Castle> by clashProperty(emptyList())
}