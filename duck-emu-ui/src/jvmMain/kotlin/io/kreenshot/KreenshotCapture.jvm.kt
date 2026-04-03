package io.kreenshot

import androidx.compose.ui.awt.ComposeWindow
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.path
import okio.FileSystem
import okio.Path.Companion.toPath
import java.awt.Rectangle
import java.awt.Robot
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

actual object KreenshotCapture {
    private var windowRef: ComposeWindow? = null

    fun init(window: ComposeWindow) {
        windowRef = window
    }

    actual fun capture(onComplete: (ByteArray?) -> Unit) {
        val window = windowRef
        val stream = ByteArrayOutputStream()

        if (window != null && window.isShowing) {
            val insets = window.insets

            val rect = Rectangle(
                window.locationOnScreen.x + insets.left,
                window.locationOnScreen.y + insets.top,
                window.width - insets.left - insets.right,
                window.height - insets.top - insets.bottom
            )

            val image = Robot().createScreenCapture(rect)
            ImageIO.write(image, "PNG", stream)
        } else {
            val screenSize = java.awt.Toolkit.getDefaultToolkit().screenSize
            val screen = Robot().createScreenCapture(Rectangle(screenSize))
            ImageIO.write(screen, "PNG", stream)
        }

        onComplete(stream.toByteArray())
    }

    actual fun save(bytes: ByteArray, fileName: String) {
        val path = FileKit.filesDir.path.plus("/${fileName}.png")
            .toPath()
        FileSystem.SYSTEM.write(path) {
            write(bytes)
        }
    }
}