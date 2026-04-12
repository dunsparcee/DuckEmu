package io.duckemu.controller

actual object GamepadController {
    actual fun startListening(
        port: Int,
        onPressed: (String) -> Unit,
        onReleased: (String) -> Unit,
        onAxis: (String, Float) -> Unit
    ) {
    }

    actual fun findGamepadConnected(): Map<Int, String> {
        return mapOf()
    }

    actual fun stopListening(port: Int) {
    }
}