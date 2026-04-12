package io.duckemu.emulator.repository.config

import android.content.Context
import androidx.core.net.toUri

var appContext: Context? = null

actual fun appPath(): String? =
    appContext?.filesDir?.absolutePath

actual fun handleFilePermission(file: String) {
    appContext?.contentResolver?.takePersistableUriPermission(
        file.toUri(),
        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
    )
}