package io.duckemu.controller

expect object GamepadController {
    fun startListening(onButton: (String) -> Unit, onAxis: (String, Float) -> Unit)
}