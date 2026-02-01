package io.duckemu.gbc.gpu

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun createImage(
    width: Int,
    height: Int,
    pixels: IntArray
): ImageBitmap {
    val argbPixels = IntArray(pixels.size) { i ->
        0xFF000000.toInt() or pixels[i]
    }

    val bitmap = Bitmap.createBitmap(
        argbPixels,
        width,
        height,
        Bitmap.Config.ARGB_8888
    )

    return bitmap.asImageBitmap()
}