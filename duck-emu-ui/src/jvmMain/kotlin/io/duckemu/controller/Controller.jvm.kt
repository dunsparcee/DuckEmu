package io.duckemu.controller

import kotlinx.coroutines.*
import net.java.games.input.Component
import net.java.games.input.Component.Identifier.Axis
import net.java.games.input.Component.Identifier.Button
import net.java.games.input.Controller
import net.java.games.input.ControllerEnvironment
import net.java.games.input.Event
import kotlin.math.abs

actual object GamepadController {

    private var controllers: Map<Int, Controller> = mapOf()
    private const val AXIS_DEADZONE = 0.15f
    private val prevButtonState = mutableMapOf<String, Float>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val jobs = mutableMapOf<Int, Job>()

    actual fun findGamepadConnected(): Map<Int, String> {
        controllers = ControllerEnvironment
            .getDefaultEnvironment()
            .controllers
            .filter { it.type == Controller.Type.GAMEPAD || it.type == Controller.Type.STICK }
            .associateBy { System.identityHashCode(it) }

        return controllers.mapValues { it.value.name }
    }

    actual fun startListening(
        port: Int,
        onPressed: (String) -> Unit,
        onReleased: (String) -> Unit,
        onAxis: (String, Float) -> Unit
    ) {
        jobs[port]?.cancel()

        jobs[port] = scope.launch {
            val controller = controllers[port]

            prevButtonState.clear()
            val event = Event()

            while (isActive && controller?.poll() == true) {
                while (controller.eventQueue?.getNextEvent(event) == true) {
                    handleEvent(event, onPressed, onReleased, onAxis)
                }
                delay(8)
            }

            delay(2000)
        }
    }

    private fun handleEvent(
        event: Event,
        onPressed: (String) -> Unit,
        onReleased: (String) -> Unit,
        onAxis: (String, Float) -> Unit
    ) {
        val id = event.component.identifier
        val value = event.value
        val name = identifierName(id)

        when (id) {
            is Button -> {
                val prev = prevButtonState[name] ?: 0f
                if (value != prev) {
                    prevButtonState[name] = value
                    when (value) {
                        1.0f -> onPressed(name)
                        0.0f -> onReleased(name)
                    }
                }
            }

            Axis.POV -> {
                val current = povName(value)
                val prev = prevButtonState.keys.filter { it.startsWith("DPAD_") }.toSet()

                prev.forEach {
                    prevButtonState.remove(it)
                    onReleased(it)
                }
                if (current != null) {
                    prevButtonState[current] = 1f
                    onPressed(current)
                }
            }

            Axis.X -> onAxis("AXIS_LEFT_X", withDeadzone(value))
            Axis.Y -> onAxis("AXIS_LEFT_Y", withDeadzone(value))
            Axis.RX -> onAxis("AXIS_RIGHT_X", withDeadzone(value))
            Axis.RY -> onAxis("AXIS_RIGHT_Y", withDeadzone(value))
            Axis.RZ -> onAxis("AXIS_RZ", withDeadzone(value))

            Axis.Z -> {
                onAxis("AXIS_LT", withDeadzone(value.coerceAtLeast(0f)))
                onAxis("AXIS_RT", withDeadzone((-value).coerceAtLeast(0f)))
            }
        }
    }

    private fun withDeadzone(value: Float) = if (abs(value) < AXIS_DEADZONE) 0f else value

    private fun povName(value: Float) = when (value) {
        Component.POV.UP -> "DPAD_UP"
        Component.POV.DOWN -> "DPAD_DOWN"
        Component.POV.LEFT -> "DPAD_LEFT"
        Component.POV.RIGHT -> "DPAD_RIGHT"
        Component.POV.UP_LEFT -> "DPAD_UP_LEFT"
        Component.POV.UP_RIGHT -> "DPAD_UP_RIGHT"
        Component.POV.DOWN_LEFT -> "DPAD_DOWN_LEFT"
        Component.POV.DOWN_RIGHT -> "DPAD_DOWN_RIGHT"
        else -> null
    }

    private fun identifierName(id: Component.Identifier) = when (id) {
        Button._0, Button.A -> "BUTTON_A"
        Button._1, Button.B -> "BUTTON_B"
        Button._2, Button.X -> "BUTTON_X"
        Button._3, Button.Y -> "BUTTON_Y"
        Button._4, Button.LEFT_THUMB -> "BUTTON_LB"
        Button._5, Button.RIGHT_THUMB -> "BUTTON_RB"
        Button._6, Button.SELECT -> "BUTTON_SELECT"
        Button._7, Button.START -> "BUTTON_START"
        Button._8 -> "BUTTON_L3"
        Button._9 -> "BUTTON_R3"
        Button._10 -> "BUTTON_L3"
        Button._11 -> "BUTTON_R3"
        else -> id.name
    }

    actual fun stopListening(port: Int) {
        jobs[port]?.cancel()
        jobs.remove(port)
    }
}