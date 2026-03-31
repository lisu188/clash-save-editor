package com.lis.clash

import com.lis.clash.objects.Tile

data class UnitTypeMetadata(
    val id: Int,
    val localizedNames: String,
    val folder: String
) {
    val displayName: String
        get() = localizedNames.split(" / ").getOrNull(1) ?: localizedNames.split(" / ").first()
}

object UnitTypes {
    val all = listOf(
        UnitTypeMetadata(0, "Posp. ruszenie / Peasant / Bauern", "peon"),
        UnitTypeMetadata(1, "Lekka piechota / Light infantry", "infl"),
        UnitTypeMetadata(2, "Ciężka piechota / Heavy infantry", "infh"),
        UnitTypeMetadata(3, "Pikinier / Pikeman", "sprl"),
        UnitTypeMetadata(4, "Halabardnik / Heavy spearman", "sprh"),
        UnitTypeMetadata(5, "Lekka jazda / Light cavalry", "cavl"),
        UnitTypeMetadata(6, "Ciężka jazda / Heavy cavalry", "cavh"),
        UnitTypeMetadata(7, "Rycerstwo / Knights", "ryc"),
        UnitTypeMetadata(8, "Dragon / Dragon cavalry", "drag"),
        UnitTypeMetadata(9, "Łucznik / Archer", "arch"),
        UnitTypeMetadata(10, "Kusznik / Crossbower", "kusza"),
        UnitTypeMetadata(11, "Muszkieter / Musketeer", "muszk"),
        UnitTypeMetadata(12, "Katapulta / Catapult", "katap"),
        UnitTypeMetadata(13, "Taran / Taran / Rammbock", "taran"),
        UnitTypeMetadata(14, "Armata / Cannon / Kanonen", "armat"),
        UnitTypeMetadata(15, "Leśnik / Forester", "lesn"),
        UnitTypeMetadata(16, "Góral / Highlander", "goral"),
        UnitTypeMetadata(17, "Budowniczy / Builder", "budow"),
        UnitTypeMetadata(18, "Czerw / Worm", "worm"),
        UnitTypeMetadata(19, "Słoń / Elephant", "slon"),
        UnitTypeMetadata(20, "Cyklop / Cyclop", "cykl"),
        UnitTypeMetadata(21, "Troll", "trol"),
        UnitTypeMetadata(22, "Skorpion / Scorpion", "scorp"),
        UnitTypeMetadata(23, "Szkielet / Skeleton", "szk"),
        UnitTypeMetadata(24, "Mag / Wizard", "mag"),
        UnitTypeMetadata(25, "Duch / Ghost", "duch"),
        UnitTypeMetadata(26, "Orzeł / Eagle", "orzel"),
        UnitTypeMetadata(27, "Pegaz / Pegasus", "pegaz"),
        UnitTypeMetadata(28, "Skrzydlak / Winger", "skrz"),
        UnitTypeMetadata(29, "Ważka / Fly / Riesenlibelle", "wazka"),
        UnitTypeMetadata(30, "Smok / Dragon / Drachen", "smok"),
        UnitTypeMetadata(31, "Złoto / Gold", "gold"),
        UnitTypeMetadata(32, "Chłopi / Peasants", "peas"),
        UnitTypeMetadata(33, "Dowódca / Tactician / Soldat", "specm"),
        UnitTypeMetadata(34, "Dowódca / Tactician / Soldat", "speck")
    )

    private val byId = all.associateBy(UnitTypeMetadata::id)

    fun metadata(typeId: Int): UnitTypeMetadata? = byId[typeId]
}

enum class TILE(val type1: Byte, val type2: Byte) {
    GRASS(0, 0),
    TREASURE(-16, 2);

    fun matches(tile: Tile): Boolean {
        return tile.type1 == type1 && tile.type2 == type2
    }
}
