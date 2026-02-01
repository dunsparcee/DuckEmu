package io.duckemu.gbc

class Controller {
    var buttonState: Int = 0
    var sendActionToPin10: Boolean = false

    fun buttonPressed(buttonIndex: Int) {
        buttonState = buttonState or (1 shl buttonIndex)
        sendActionToPin10 = true
    }

    fun buttonRelease(buttonIndex: Int) {
        buttonState = buttonState and 0xff - (1 shl buttonIndex)
        sendActionToPin10 = true
    }
}