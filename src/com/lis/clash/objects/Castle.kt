package com.lis.clash.objects

import com.lis.clash.CastleAddonSlot
import com.lis.clash.CastleAddonTypes
import com.lis.clash.ClashAggregateProperty
import com.lis.clash.ClashMaskedProperty
import com.lis.clash.ClashSignedProperty
import com.lis.clash.ClashSimpleProperty
import com.lis.clash.GarrisonOrder
import com.lis.clash.RawSixByteRecord

class Castle(parent: ClashObject, index: Int) : ClashObject(parent, index) {
    companion object {
        const val BUILDING_HOSPITAL = 1
        const val BUILDING_BARRACKS = 2
        const val BUILDING_WORKSHOP = 4
        const val BUILDING_SCHOOL = 8
        const val BUILDING_SMITHS = 16
    }

    @ClashSimpleProperty(0, 1)
    var tileRow: Int by clashProperty(0)

    @ClashSimpleProperty(1, 1)
    var tileColumn: Int by clashProperty(0)

    @ClashSimpleProperty(2, 1)
    var ownerPlayerIndex: Int by clashProperty(0)

    @ClashSimpleProperty(3, 1)
    var appearance: Int by clashProperty(0)

    @ClashSignedProperty(4, 1)
    var buildingType: Int by clashProperty(0)

    @ClashSimpleProperty(5, 11)
    var displayName: String by clashProperty("")

    @ClashSignedProperty(16, 2)
    var constructionWorkRemaining: Int by clashProperty(0)

    @ClashAggregateProperty(18, 12, 31, Unit::class)
    var units: List<Unit> by clashProperty(emptyList())

    @ClashSimpleProperty(390, 12)
    var garrisonServiceStateBytes: List<Byte> by clashProperty(emptyList())

    @ClashSimpleProperty(402, 12)
    var unitLicenceTypeIds: List<Byte> by clashProperty(emptyList())

    @ClashSignedProperty(414, 1)
    var activeProductionLicenceSlotIndex: Int by clashProperty(0)

    @ClashSimpleProperty(415, 1)
    var productionTurnsRemaining: Int by clashProperty(0)

    @ClashSimpleProperty(416, 1)
    var castleAddonFlags: Int by clashProperty(0)

    @ClashSimpleProperty(420, 1)
    var constructionLockFlags: Int by clashProperty(0)

    @ClashSimpleProperty(421, 1)
    var wallStrength: Int by clashProperty(0)

    @ClashSimpleProperty(422, 7)
    var wallSectionIntegrity: List<Byte> by clashProperty(emptyList())

    @ClashSimpleProperty(429, 1)
    var upgradeTimerTurns: Int by clashProperty(0)

    @ClashMaskedProperty(430, 2, 0x0FFF)
    var peasantCount: Int by clashProperty(0)

    @ClashMaskedProperty(432, 2, 0x0FFF)
    var populationGrowthDeltaRaw12: Int by clashProperty(0)

    @ClashSignedProperty(434, 1)
    var satisfaction: Int by clashProperty(0)

    @ClashMaskedProperty(435, 1, 0x07)
    var plagueState: Int by clashProperty(0)

    @ClashMaskedProperty(436, 1, 0x3F)
    var taxRate: Int by clashProperty(0)

    @ClashSimpleProperty(438, 4)
    var storedMoney: Int by clashProperty(0)

    @ClashSimpleProperty(442, 2)
    var lastCollectedGoldIncome: Int by clashProperty(0)

    @ClashMaskedProperty(444, 1, 0x07)
    var techLevelBits: Int by clashProperty(0)

    @ClashSimpleProperty(445, 18)
    var prisonerSlotsRaw: List<Byte> by clashProperty(emptyList())

    @ClashSimpleProperty(463, 4)
    var castleFactHandle: Int by clashProperty(0)

    @Deprecated("Use buildingType")
    var footprintClass: Int
        get() = buildingType
        set(value) {
            buildingType = value
        }

    @Deprecated("Use garrisonServiceStateBytes")
    var garrisonOrderBytes: List<Byte>
        get() = garrisonServiceStateBytes
        set(value) {
            garrisonServiceStateBytes = value
        }

    @Deprecated("Use unitLicenceTypeIds")
    var addonTypeIds: List<Byte>
        get() = unitLicenceTypeIds
        set(value) {
            unitLicenceTypeIds = value
        }

    @Deprecated("Use activeProductionLicenceSlotIndex")
    var selectedAddonSlotIndex: Int
        get() = activeProductionLicenceSlotIndex
        set(value) {
            activeProductionLicenceSlotIndex = value
        }

    @Deprecated("Use castleFactHandle")
    var castleFactId: Int
        get() = castleFactHandle
        set(value) {
            castleFactHandle = value
        }

    fun populationGrowthDelta(): Int {
        val value = populationGrowthDeltaRaw12 and 0x0FFF
        return if ((value and 0x0800) != 0) value - 0x1000 else value
    }

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

    fun unitLicenceSlots(): List<CastleAddonSlot> {
        return unitLicenceTypeIds.mapIndexedNotNull { slotIndex, rawTypeId ->
            val typeId = rawTypeId.toInt() and 0xFF
            if (typeId == CastleAddonTypes.EMPTY_SLOT) {
                null
            } else {
                CastleAddonSlot(slotIndex, typeId, CastleAddonTypes.metadata(typeId)?.displayName)
            }
        }
    }

    fun unitLicenceTypeNames(): List<String> {
        return unitLicenceSlots().map { slot ->
            slot.displayName ?: "unknown(${slot.typeId})"
        }
    }

    @Deprecated("Use unitLicenceSlots")
    fun addonSlots(): List<CastleAddonSlot> = unitLicenceSlots()

    @Deprecated("Use unitLicenceTypeNames")
    fun addonTypeNames(): List<String> = unitLicenceTypeNames()

    fun garrisonServiceStates(): List<GarrisonOrder> {
        return garrisonServiceStateBytes.map { raw ->
            GarrisonOrder(raw.toInt() and 0xFF)
        }
    }

    @Deprecated("Use garrisonServiceStates")
    fun garrisonOrders(): List<GarrisonOrder> = garrisonServiceStates()

    fun prisonerSlots(): List<RawSixByteRecord> {
        return prisonerSlotsRaw.chunked(6).map(::RawSixByteRecord)
    }

    fun decodedPrisonerSlots(): List<BuildingPrisonerSlot> {
        return prisonerSlotsRaw.chunked(6).filter { it.size == 6 }.map { bytes ->
            BuildingPrisonerSlot(
                prisonerTypeId = bytes[0].toInt(),
                capturedOwnerPlayerIndex = bytes[1].toInt() and 0xFF,
                turnsHeld = bytes[2].toInt() and 0xFF,
                pendingAction = bytes[3].toInt() and 0xFF,
                ransomValue = (bytes[4].toInt() and 0xFF) or ((bytes[5].toInt() and 0xFF) shl 8)
            )
        }
    }

    override fun isValid(): Boolean {
        return buildingType != -1
    }
}

data class BuildingPrisonerSlot(
    val prisonerTypeId: Int,
    val capturedOwnerPlayerIndex: Int,
    val turnsHeld: Int,
    val pendingAction: Int,
    val ransomValue: Int
)
