package com.lis.clash.objects

import com.lis.clash.ClashSignedProperty
import com.lis.clash.ClashSimpleProperty

/** Save-local options; layout pinned to clash-disassembly c9c0fa7, options[27]. */
class Options(parent: ClashObject, index: Int) : ClashObject(parent, index) {
    @ClashSignedProperty(0, 4)
    var transitionAnimationsEnabled: Int by clashProperty(0)
    @ClashSignedProperty(4, 4)
    var gridOverlayEnabled: Int by clashProperty(0)
    @ClashSignedProperty(8, 4)
    var statusOverlayEnabled: Int by clashProperty(0)
    @ClashSignedProperty(12, 4)
    var fastMovementAnimationsEnabled: Int by clashProperty(0)
    @ClashSignedProperty(16, 4)
    var musicEnabled: Int by clashProperty(0)
    @ClashSignedProperty(20, 4)
    var soundEffectsEnabled: Int by clashProperty(0)
    @ClashSimpleProperty(24, 1)
    var scrollSpeedRaw: Int by clashProperty(0)
    @ClashSimpleProperty(25, 1)
    var soundVolumeRaw: Int by clashProperty(0)
    // Shared options application also interprets this signed byte as palette brightness.
    @com.lis.clash.ClashFieldEvidence(confidence = "medium-high")
    @ClashSignedProperty(26, 1)
    var musicVolumeRaw: Int by clashProperty(0)
}
