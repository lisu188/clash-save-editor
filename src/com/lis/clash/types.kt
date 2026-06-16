package com.lis.clash

enum class UnitCategory {
    FLYING,
    CARGO,
    SPECIAL_PERSONAGE
}

data class UnitTypeMetadata(
    val id: Int,
    val localizedNames: String,
    val folder: String,
    val categories: Set<UnitCategory> = emptySet()
) {
    val displayName: String
        get() = localizedNames.split(" / ").getOrNull(1) ?: localizedNames.split(" / ").first()

    fun hasCategory(category: UnitCategory): Boolean {
        return category in categories
    }
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
        UnitTypeMetadata(26, "Orzeł / Eagle", "orzel", setOf(UnitCategory.FLYING)),
        UnitTypeMetadata(27, "Pegaz / Pegasus", "pegaz", setOf(UnitCategory.FLYING)),
        UnitTypeMetadata(28, "Skrzydlak / Winger", "skrz", setOf(UnitCategory.FLYING)),
        UnitTypeMetadata(29, "Ważka / Fly / Riesenlibelle", "wazka", setOf(UnitCategory.FLYING)),
        UnitTypeMetadata(30, "Smok / Dragon / Drachen", "smok", setOf(UnitCategory.FLYING)),
        UnitTypeMetadata(31, "Złoto / Gold", "gold", setOf(UnitCategory.CARGO)),
        UnitTypeMetadata(32, "Chłopi / Peasants", "peas", setOf(UnitCategory.CARGO)),
        UnitTypeMetadata(33, "Dowódca / Tactician / Soldat", "specm", setOf(UnitCategory.SPECIAL_PERSONAGE)),
        UnitTypeMetadata(34, "Dowódca / Tactician / Soldat", "speck", setOf(UnitCategory.SPECIAL_PERSONAGE))
    )

    private val byId = all.associateBy(UnitTypeMetadata::id)

    fun metadata(typeId: Int): UnitTypeMetadata? = byId[typeId]
}

data class QueuedPathWaypoint(
    val tileRow: Int,
    val tileColumn: Int,
    val cumulativeCost: Int
)

data class RawSixByteRecord(
    val rawBytes: List<Byte>
) {
    val marker: Int
        get() = rawBytes.firstOrNull()?.toInt() ?: -1

    val isEmpty: Boolean
        get() = rawBytes.firstOrNull() == (-1).toByte()
}

data class GarrisonOrder(
    val rawValue: Int
) {
    val trainCountdown: Int
        get() = rawValue and 0x07

    val repairCountdown: Int
        get() = (rawValue ushr 3) and 0x07
}

data class CastleAddonTypeMetadata(
    val id: Int,
    val displayName: String
)

object CastleAddonTypes {
    const val EMPTY_SLOT = 0xFF

    val all = listOf(
        CastleAddonTypeMetadata(0, "Court"),
        CastleAddonTypeMetadata(1, "Tower"),
        CastleAddonTypeMetadata(2, "Hospital"),
        CastleAddonTypeMetadata(3, "Barracks"),
        CastleAddonTypeMetadata(4, "Workshop"),
        CastleAddonTypeMetadata(5, "School"),
        CastleAddonTypeMetadata(6, "Smiths"),
        CastleAddonTypeMetadata(7, "Peasants"),
        CastleAddonTypeMetadata(8, "Barracks")
    )

    private val byId = all.associateBy(CastleAddonTypeMetadata::id)

    fun metadata(typeId: Int): CastleAddonTypeMetadata? = byId[typeId]
}

data class CastleAddonSlot(
    val slotIndex: Int,
    val typeId: Int,
    val displayName: String?
)
