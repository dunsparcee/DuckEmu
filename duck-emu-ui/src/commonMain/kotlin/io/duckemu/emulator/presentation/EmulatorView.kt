package io.duckemu.emulator.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.duckemu.EmulatorViewModel

@Composable
fun EmulatorScreen(emuViewModel: EmulatorViewModel) {
    Canvas(Modifier.fillMaxSize()) {
        emuViewModel.graphics?.let { image ->

            val canvasWidth = size.width
            val canvasHeight = size.height
            val scale = minOf(canvasWidth / image.width, canvasHeight / image.height)

            val scaledWidth = image.width * scale
            val scaledHeight = image.height * scale

            val dx = (canvasWidth - scaledWidth) / 2
            val dy = (canvasHeight - scaledHeight) / 2

            drawImage(
                image = image,
                dstOffset = IntOffset(dx.toInt(), dy.toInt()),
                dstSize = IntSize(scaledWidth.toInt(), scaledHeight.toInt()),
                filterQuality = FilterQuality.None
            )
        }
    }
}