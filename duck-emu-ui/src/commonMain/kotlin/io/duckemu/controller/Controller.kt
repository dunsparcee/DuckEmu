package io.duckemu.controller

expect object GamepadController {
    fun startListening(
        port: Int,
        onPressed: (String) -> Unit,
        onReleased: (String) -> Unit,
        onAxis: (String, Float) -> Unit = { _, _ -> }
    )

    fun findGamepadConnected(): Map<Int, String>

    fun stopListening(port: Int)
}