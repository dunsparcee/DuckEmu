package io.duckemu.controller

import net.java.games.input.Component.Identifier.Axis
import net.java.games.input.Component.Identifier.Button
import net.java.games.input.Component
import net.java.games.input.Controller
import net.java.games.input.ControllerEnvironment
import net.java.games.input.Event
import kotlin.concurrent.thread
import kotlin.math.abs

actual object GamepadController {

    private const val AXIS_DEADZONE = 0.15f
    private val prevButtonState = mutableMapOf<String, Float>()

    actual fun startListening(
        onButton: (String) -> Unit,
        onAxis: (String, Float) -> Unit
    ) {
        thread(name = "gamepad-poll", isDaemon = true) {
            val controller = findGamepad() ?: return@thread
            val event = Event()

            while (true) {
                if (!controller.poll()) break

                while (controller.eventQueue.getNextEvent(event)) {
                    handleEvent(event, onButton, onAxis)
                }

                Thread.sleep(8)
            }
        }
    }

    private fun handleEvent(
        event: Event,
        onButton: (String) -> Unit,
        onAxis: (String, Float) -> Unit
    ) {
        val id    = event.component.identifier
        val value = event.value
        val name  = identifierName(id)

        when (id) {
            is Button -> {
                val prev = prevButtonState[name] ?: 0f
                if (value != prev) {
                    prevButtonState[name] = value
                    when (value) {
                        1.0f -> onButton("${name}_DOWN")
                        0.0f -> onButton("${name}_UP")
                    }
                }
            }

            Axis.POV -> povName(value)?.let(onButton)

            Axis.X  -> onAxis("AXIS_LEFT_X",  withDeadzone(value))
            Axis.Y  -> onAxis("AXIS_LEFT_Y",  withDeadzone(value))
            Axis.RX -> onAxis("AXIS_RIGHT_X", withDeadzone(value))
            Axis.RY -> onAxis("AXIS_RIGHT_Y", withDeadzone(value))
            Axis.RZ -> onAxis("AXIS_RZ",      withDeadzone(value))

            Axis.Z  -> {
                onAxis("AXIS_LT", withDeadzone(value.coerceAtLeast(0f)))
                onAxis("AXIS_RT", withDeadzone((-value).coerceAtLeast(0f)))
            }
        }
    }

    private fun withDeadzone(value: Float) = if (abs(value) < AXIS_DEADZONE) 0f else value

    private fun findGamepad() = ControllerEnvironment
        .getDefaultEnvironment()
        .controllers
        .firstOrNull { it.type == Controller.Type.GAMEPAD || it.type == Controller.Type.STICK }

    private fun povName(value: Float) = when (value) {
        Component.POV.UP         -> "DPAD_UP"
        Component.POV.DOWN       -> "DPAD_DOWN"
        Component.POV.LEFT       -> "DPAD_LEFT"
        Component.POV.RIGHT      -> "DPAD_RIGHT"
        Component.POV.UP_LEFT    -> "DPAD_UP_LEFT"
        Component.POV.UP_RIGHT   -> "DPAD_UP_RIGHT"
        Component.POV.DOWN_LEFT  -> "DPAD_DOWN_LEFT"
        Component.POV.DOWN_RIGHT -> "DPAD_DOWN_RIGHT"
        else                     -> null
    }

    private fun identifierName(id: Component.Identifier) = when (id) {
        Button._0, Button.A           -> "BUTTON_A"
        Button._1, Button.B           -> "BUTTON_B"
        Button._2, Button.X           -> "BUTTON_X"
        Button._3, Button.Y           -> "BUTTON_Y"
        Button._4, Button.LEFT_THUMB  -> "BUTTON_LB"
        Button._5, Button.RIGHT_THUMB -> "BUTTON_RB"
        Button._6                     -> "BUTTON_LT"
        Button._7                     -> "BUTTON_RT"
        Button._8, Button.SELECT      -> "BUTTON_SELECT"
        Button._9, Button.START       -> "BUTTON_START"
        Button._10                    -> "BUTTON_L3"
        Button._11                    -> "BUTTON_R3"
        else                          -> id.name
    }
}