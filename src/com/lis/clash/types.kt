package com.lis.clash

import com.lis.clash.objects.Tile

enum class UNIT(val id: Int) {
    // **Military units**
    PEASANT(0),
    SQUIRE(1),
    ARCHER(2),
    HIGHLANDER(3),
    LIGHT_INFANTRY(4),
    HEAVY_INFANTRY(5),
    CATAPULT(6),
    CROSSBOWMAN(7),
    MUSKETEER(8),
    SPEARMAN(9),
    PIKEMAN(10),
    CANNON(11),

    // **Support / non-combat**
    FORESTER(12),
    BUILDER(13),
    PIONEER(14),
    RAM(15),

    // **Cavalry & mounts**
    PEGASUS(16),
    DRAGON(17),
    LIGHT_CAVALRY(18),
    HEAVY_CAVALRY(19),
    KNIGHT(20),
    DRAGON_CAVALRY(21),

    // **Monsters**
    TROLL(22),
    SCORPION(23),
    SKELETON(24),
    WIZARD(25),
    GHOST(26),

    // **Flying creatures**
    EAGLE(27),
    FLY(28),           // e.g. generic “fly” unit
    DRAGONFLY(29),     // giant dragonfly

    // **Beasts**
    WORM(30),
    ELEPHANT(31),
    CYCLOPS(32),

    // **Resources or special**
    GOLD(33),         // if you treat gold nodes as “units”

    // **Tactical leaders**
    TACTICIAN(34),

    // **Miscellaneous foot**
    SOLDIER(35),
    PEON(36);

    companion object {
        private val byId = values().associateBy(UNIT::id)
        fun fromId(id: Int): UNIT? = byId[id]
    }
}




enum class TERRAIN(val id: Int, val cost: Int?) {
    ROAD(      0xCF,  3),
    PLAIN(     0xB7,  4),
    DESERT(    0x97,  5),
    FOREST(    0xB9,  6),
    SWAMP(     0x27,  7),
    HILLS(     0xCC,  8),
    MOUNTAINS( 0xCA, null),
    WATER(     0x93, null),
    TREASURE(  0xF002, null);

    companion object {
        fun fromId(id: Int): TERRAIN? = values().find { it.id == id }
    }
}

