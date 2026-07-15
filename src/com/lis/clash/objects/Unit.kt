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

    @ClashSimpleProperty(12, 1)
    var stanceBits: Int by clashProperty(0)

    @ClashMaskedProperty(12, 1, 0x03)
    var experienceLevel: Int by clashProperty(0)

    @ClashMaskedProperty(12, 1, 0x03, 2)
    var experienceProgress: Int by clashProperty(0)

    @ClashSimpleProperty(13, 1)
    var stateFlags: Int by clashProperty(0)

    @ClashMaskedProperty(13, 1, 0x01, 2)
    var lowMoraleFlag: Int by clashProperty(0)

    @ClashSimpleProperty(18, 4)
    var auxRuntimeState: Int by clashProperty(0)

    @ClashSimpleProperty(22, 1)
    var stateBits2: Int by clashProperty(0)

    override fun isValid(): Boolean {
        return typeId in 0..40
    }

    fun hasMaximumExperience(): Boolean {
        return experienceLevel == MAX_EXPERIENCE_LEVEL
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
