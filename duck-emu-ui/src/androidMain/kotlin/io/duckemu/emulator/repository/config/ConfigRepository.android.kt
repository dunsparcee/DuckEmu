package io.duckemu.emulator.repository.config

import android.app.Application
import android.content.Context

var appContext: Context? = null

actual fun provideStorePath(): String? =
    appContext?.filesDir?.absolutePath