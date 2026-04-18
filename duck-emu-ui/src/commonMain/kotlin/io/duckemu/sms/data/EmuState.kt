package io.duckemu.sms.data

object EmuState {
    var is_sms: Boolean = true
    var is_gg: Boolean = false
    var no_of_scanlines: Int = 0
    var cyclesPerLine: Int = 0
    
    var h_start: Int = 0
    var h_end: Int = 32
    var emuWidth: Int = 256
    var emuHeight: Int = 192
    
    var controller1: Int = 0xFF
    var controller2: Int = 0xFF
    var ggstart: Int = 0xFF
    
    var lightgunX: Int = 0
    var lightgunY: Int = 0
    var lightgunClick: Boolean = false
    var lightgunEnabled: Boolean = false
    
    var soundEnabled: Boolean = true
    var supportsSound: Boolean = true
}
