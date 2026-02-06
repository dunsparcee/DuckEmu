package io.duckemu.gbc.gpu

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

actual fun createImage(
    width: Int,
    height: Int,
    pixels: IntArray
): ImageBitmap {
    val argbPixels = IntArray(pixels.size) { i ->
        0xFF000000.toInt() or pixels[i]
    }

    val bytes = ByteArray(argbPixels.size * 4)
    for (i in argbPixels.indices) {
        val pixel = argbPixels[i]
        val offset = i * 4
        bytes[offset] = (pixel and 0xFF).toByte()
        bytes[offset + 1] = ((pixel shr 8) and 0xFF).toByte()
        bytes[offset + 2] = ((pixel shr 16) and 0xFF).toByte()
        bytes[offset + 3] = ((pixel shr 24) and 0xFF).toByte()
    }

    val imageInfo = ImageInfo.makeS32(width, height, ColorAlphaType.UNPREMUL)
    val skiaImage = Image.makeRaster(
        imageInfo = imageInfo,
        bytes = bytes,
        rowBytes = width * 4
    )

    return skiaImage.toComposeImageBitmap()
}