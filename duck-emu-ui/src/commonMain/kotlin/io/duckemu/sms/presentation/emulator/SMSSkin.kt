package io.duckemu.sms.presentation.emulator

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.duckemu.emulator.presentation.EmulatorScreen

@Composable
fun SMSSkin(viewModel: SMSViewModel) {
    Box(modifier = Modifier.fillMaxSize()) {
        EmulatorScreen(viewModel)
    }
}
