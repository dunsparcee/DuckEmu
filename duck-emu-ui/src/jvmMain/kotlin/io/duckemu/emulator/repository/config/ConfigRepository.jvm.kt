package io.duckemu.emulator.repository.config

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.path


actual fun appPath(): String? {
    return FileKit.filesDir.path
}

actual fun handleFilePermission(file: String) {
}