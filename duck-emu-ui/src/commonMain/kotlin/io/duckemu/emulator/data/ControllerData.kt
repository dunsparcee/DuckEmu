package io.duckemu.emulator.data

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ControllerTheme {
    abstract val backgroundColorArgb: Int
    abstract val buttonColorArgb: Int
    abstract val backgroundAlpha: Float

    @Serializable
    @SerialName("gbc")
    data class GBC(
        override val backgroundColorArgb: Int = 0xFF3978F5.toInt(),
        override val buttonColorArgb: Int = 0xFFD1D1D1.toInt(),
        override val backgroundAlpha: Float = 1f,
        val actionButtonSize: Float = 80f,
        val dpadSize: Float = 150f,
        val dpadX: Float = 30f,
        val dpadY: Float = 0f,
        val aX: Float = -10f,
        val aY: Float = -20f,
        val bX: Float = -10f,
        val bY: Float = 40f,
        val smallButtonWidth: Float = 35f,
        val startSelectX: Float = 0f,
        val startSelectY: Float = 0f
    ) : ControllerTheme() {
        val backgroundColor get() = Color(backgroundColorArgb)
        val buttonColor get() = Color(buttonColorArgb)
    }

    @Serializable
    @SerialName("nes")
    data class NES(
        override val backgroundColorArgb: Int = 0xFF8B8B8B.toInt(), // NesGray
        override val buttonColorArgb: Int = 0xFFE60012.toInt(),     // NesRed
        override val backgroundAlpha: Float = 1f,
        val dpadSize: Float = 140f,
        val actionButtonSize: Float = 70f,
        val aYOffset: Float = -10f,
        val bYOffset: Float = 30f,
        val dpadXOffset: Float = 24f,
        val dpadYOffset: Float = 10f
    ) : ControllerTheme() {
        val backgroundColor get() = Color(backgroundColorArgb)
        val buttonColor get() = Color(buttonColorArgb)
    }
}