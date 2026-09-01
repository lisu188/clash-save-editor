package com.lis.clash.objects

import com.lis.clash.ClashMaskedProperty
import com.lis.clash.ClashSignedProperty
import com.lis.clash.ClashSimpleProperty

class Unit(parent: ClashObject, index: Int) : ClashObject(parent, index) {
    @ClashSignedProperty(0, 2)
    var typeId: Int by clashProperty(0)

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
    var statusLevel: Int by clashProperty(0)

    @ClashMaskedProperty(12, 1, 0x03, 2)
    var orderState: Int by clashProperty(0)

    @ClashMaskedProperty(12, 1, 0x07, 4)
    var volleysUsed: Int by clashProperty(0)

    @ClashSimpleProperty(13, 1)
    var stateFlags: Int by clashProperty(0)

    @ClashMaskedProperty(13, 1, 0x01)
    var readyForTurnFlag: Int by clashProperty(0)

    @ClashMaskedProperty(13, 1, 0x01, 1)
    var spentTurnFlag: Int by clashProperty(0)

    @ClashMaskedProperty(13, 1, 0x01, 2)
    var lowMoraleFlag: Int by clashProperty(0)

    @ClashMaskedProperty(13, 1, 0x01, 3)
    var plagueFlag: Int by clashProperty(0)

    @ClashSimpleProperty(18, 4)
    var auxRuntimeState: Int by clashProperty(0)

    @ClashSimpleProperty(22, 1)
    var stateBits2: Int by clashProperty(0)

    @Deprecated("Byte +12 bits 0..1 are a status level, not an experience level")
    var experienceLevel: Int
        get() = statusLevel
        set(value) {
            statusLevel = value
        }

    @Deprecated("Byte +12 bits 2..3 are an order state, not experience progress")
    var experienceProgress: Int
        get() = orderState
        set(value) {
            orderState = value
        }

    override fun isValid(): Boolean {
        return typeId != -1
    }

    fun remainingVolleys(): Int {
        return (statusLevel + 1 - volleysUsed).coerceAtLeast(0)
    }

    fun hasMaximumStatusLevel(): Boolean {
        return statusLevel == MAX_STATUS_LEVEL
    }

    @Deprecated("Use hasMaximumStatusLevel")
    fun hasMaximumExperience(): Boolean {
        return hasMaximumStatusLevel()
    }

    fun hasLowMoraleFlag(): Boolean {
        return lowMoraleFlag != 0
    }

    fun hasPlagueFlag(): Boolean {
        return plagueFlag != 0
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
        const val MAX_STATUS_LEVEL = 3
        const val MAX_ORDER_STATE = 3
        const val MAX_VOLLEYS_USED = 7
        @Deprecated("Use MAX_STATUS_LEVEL")
        const val MAX_EXPERIENCE_LEVEL = MAX_STATUS_LEVEL
        @Deprecated("Use MAX_ORDER_STATE")
        const val MAX_EXPERIENCE_PROGRESS = MAX_ORDER_STATE
        val MORALE_FATIGUE_PROTECTED_TYPE_IDS = setOf(31, 32, 33, 34)
    }
}
