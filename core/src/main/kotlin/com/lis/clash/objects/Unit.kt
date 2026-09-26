package com.lis.clash.objects

import com.lis.clash.ClashMaskedProperty
import com.lis.clash.ClashSignedProperty
import com.lis.clash.ClashSimpleProperty

class Unit(parent: ClashObject, index: Int) : ClashObject(parent, index) {
    @ClashSignedProperty(0, 2)
    var typeId: Int by clashProperty(-1)

    @ClashSimpleProperty(2, 1)
    var ownerPlayerIndex: Int by clashProperty(0)

    @ClashSimpleProperty(8, 1)
    var currentActionPoints: Int by clashProperty(0)

    @ClashSimpleProperty(9, 1)
    var currentHealthPercent: Int by clashProperty(0)

    @ClashSimpleProperty(10, 1)
    var fatigue: Int by clashProperty(0)

    @ClashSimpleProperty(11, 1)
    var morale: Int by clashProperty(0)

    @com.lis.clash.ClashFieldEvidence(confidence = "partial")
    @ClashSimpleProperty(12, 1, readOnly = true)
    var stanceBits: Int by clashProperty(0)

    @Deprecated("Recovered meaning is status level; use statusLevel")
    @ClashMaskedProperty(12, 1, 0x03)
    var experienceLevel: Int by clashProperty(0)

    @Deprecated("Recovered meaning is order state; use orderState")
    @ClashMaskedProperty(12, 1, 0x03, 2)
    var experienceProgress: Int by clashProperty(0)

    @ClashMaskedProperty(12, 1, 0x03)
    var statusLevel: Int by clashProperty(0)

    @ClashMaskedProperty(12, 1, 0x03, 2)
    var orderState: Int by clashProperty(0)

    @ClashMaskedProperty(12, 1, 0x07, 4)
    var volleysUsed: Int by clashProperty(0)

    @ClashMaskedProperty(13, 1, 0x01)
    var readyForTurn: Int by clashProperty(0)

    @ClashMaskedProperty(13, 1, 0x01, 1)
    var spentTurn: Int by clashProperty(0)

    @ClashMaskedProperty(13, 1, 0x01, 3)
    var plagueFlag: Int by clashProperty(0)

    @com.lis.clash.ClashFieldEvidence(confidence = "partial")
    @ClashSimpleProperty(13, 1, readOnly = true)
    var stateFlags: Int by clashProperty(0)

    @ClashMaskedProperty(13, 1, 0x01, 2)
    var lowMoraleFlag: Int by clashProperty(0)

    @ClashSimpleProperty(18, 4, readOnly = true)
    var auxRuntimeState: Int by clashProperty(0)

    @com.lis.clash.ClashFieldEvidence(confidence = "partial")
    @ClashSimpleProperty(22, 1, readOnly = true)
    var stateBits2: Int by clashProperty(0)

    @com.lis.clash.ClashFieldEvidence(source = "clash-disassembly/src/units/0040F510_00411560_units_001.cpp:UnitStats_CalcEffectiveDefensePower")
    @ClashMaskedProperty(22, 1, 0x01)
    var defenseBonusFlag: Int by clashProperty(0)

    override fun isValid(): Boolean {
        return typeId != -1
    }

    @Deprecated("This checks status level, not accumulated experience")
    fun hasMaximumExperience(): Boolean {
        return statusLevel == MAX_EXPERIENCE_LEVEL
    }

    fun hasLowMoraleFlag(): Boolean {
        return lowMoraleFlag != 0
    }

    fun isMoraleFatigueProtectedType(): Boolean {
        return typeId in MORALE_FATIGUE_PROTECTED_TYPE_IDS
    }

    fun moraleBand(): String {
        return when (morale) {
            in 0..4 -> "low"
            in 11..15 -> "good"
            in 16..MAX_MORALE -> "excellent"
            else -> "normal"
        }
    }

    fun fatigueBand(): String {
        return when (fatigue) {
            in 80..89 -> "tired"
            in 90..99 -> "exhausted"
            MAX_FATIGUE -> "spent"
            else -> "normal"
        }
    }

    companion object {
        const val MAX_HEALTH_PERCENT = 100
        const val MAX_FATIGUE = 100
        const val MAX_MORALE = 20
        const val MAX_EXPERIENCE_LEVEL = 3
        const val MAX_EXPERIENCE_PROGRESS = 3
        val MORALE_FATIGUE_PROTECTED_TYPE_IDS = setOf(31, 32, 33, 34)
    }
}
