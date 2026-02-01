package io.duckemu

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

@Composable
fun EmulatorScreen(controller: GameBoyViewModel) {
    Canvas(Modifier.fillMaxSize()) {
        controller.gameBoyImage?.let { buffer ->
            val bitmap = buffer.toComposeImageBitmap()

            val canvasWidth = size.width
            val canvasHeight = size.height
            val scale = minOf(canvasWidth / bitmap.width, canvasHeight / bitmap.height)

            val scaledWidth = bitmap.width * scale
            val scaledHeight = bitmap.height * scale

            val dx = (canvasWidth - scaledWidth) / 2
            val dy = (canvasHeight - scaledHeight) / 2

            drawImage(
                image = bitmap,
                dstOffset = IntOffset(dx.toInt(), dy.toInt()),
                dstSize = IntSize(scaledWidth.toInt(), scaledHeight.toInt()),
                filterQuality = FilterQuality.None
            )
        }
    }
}